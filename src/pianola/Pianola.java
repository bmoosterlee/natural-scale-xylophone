package pianola;/*todo write a history tracker of when notes were played. Take average of notes played to form smoothed peaks
 * todo find maxima of these smoothed values. Perform fourier analysis on this signal, also obtaining the phases of
 * todo a frequency. find harmonic image of each frequency, multiply image by the phase of the tonic.
 * todo add all these phase dependent harmonic images together, and display the rhythmic value in real time.
 * todo let the pianola take the rhythmic harmonic value in real time and play that.
 * todo use a lookahead of the length of the shortest frame for the pianola, and play the maximum within that frame*/

import component.*;
import component.buffer.*;
import component.buffer.RunningPipeComponent;
import frequency.Frequency;
import pianola.patterns.PianolaPattern;
import spectrum.buckets.BucketHistory;
import spectrum.buckets.Buckets;
import spectrum.buckets.BucketsAverager;
import spectrum.buckets.PrecalculatedBucketHistory;

import java.util.*;

public class Pianola extends RunningPipeComponent<Pulse, List<Frequency>> {

    public Pianola(SimpleBuffer<Pulse> tickBuffer, BoundedBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> spectrumBuffer, SimpleBuffer<List<Frequency>> outputBuffer, PianolaPattern pianolaPattern, int inaudibleFrequencyMargin) {
        super(tickBuffer, outputBuffer, build(spectrumBuffer, pianolaPattern,inaudibleFrequencyMargin));
    }

    public static CallableWithArguments<Pulse, List<Frequency>> build(BoundedBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> spectrumBuffer, PianolaPattern pianolaPattern, int inaudibleFrequencyMargin){
        return new CallableWithArguments<>() {
            private final InputPort<Buckets> notesInput;
            private BucketHistory noteHistory;
            private final int repetitionDampener;
            private final OutputPort<Buckets> averagerInput;

            private final InputPort<Buckets> preparedNotesInput;
            private final InputPort<Buckets> preparedHarmonicsInput;

            private InputPort<List<Frequency>> methodOutputPort;

            private OutputPort<List<Frequency>> outputPort;

            {
                int count = 0;
                int capacity = 1000;

                repetitionDampener = 3;
                noteHistory = new PrecalculatedBucketHistory(50);

                SimpleBuffer<Buckets> noteSpectrumBuffer = new SimpleBuffer<>(capacity, "pianola - note spectrum");
                SimpleBuffer<Buckets> harmonicSpectrumBuffer = new SimpleBuffer<>(capacity, "pianola - harmonic spectrum");
                new Unpairer<>(spectrumBuffer, noteSpectrumBuffer, harmonicSpectrumBuffer);

                SimpleBuffer<Buckets> notesAveragerInputBuffer = new SimpleBuffer<>(capacity, "Pianola" + String.valueOf(count));
                count++;
                averagerInput = new OutputPort<>(notesAveragerInputBuffer);

                BoundedBuffer<List<Frequency>> patternOutput = new SimpleBuffer<>(1, "pianola - output");
                outputPort = patternOutput.createOutputPort();
                methodOutputPort = patternOutput.createInputPort();

                notesInput = noteSpectrumBuffer.createInputPort();
                preparedNotesInput =
                    notesAveragerInputBuffer
                    .performMethod(BucketsAverager.build(2 * inaudibleFrequencyMargin))
                    .createInputPort();
                preparedHarmonicsInput =
                    harmonicSpectrumBuffer
                    .performMethod(BucketsAverager.build(inaudibleFrequencyMargin))
                    .createInputPort();
            }

            private List<Frequency> playNotes() {
                try {
                    Buckets origNoteBuckets;
                    try {
                        origNoteBuckets = notesInput.consume();
                    } catch (NullPointerException e) {
                        origNoteBuckets = new Buckets();
                    }
                    noteHistory = noteHistory.addNewBuckets(origNoteBuckets);

                    Buckets notesBeforeAveraging = noteHistory.getTimeAveragedBuckets().multiply(repetitionDampener);
                    averagerInput.produce(notesBeforeAveraging);

                    Buckets noteBuckets = preparedNotesInput.consume();
                    Buckets harmonicsBuckets = preparedHarmonicsInput.consume();

                    LinkedList<Frequency> results = new LinkedList<>(pianolaPattern.playPattern(noteBuckets, harmonicsBuckets));
                    outputPort.produce(results);

                    return methodOutputPort.consume();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public List<Frequency> call(Pulse input) {
                return playNotes();
            }
        };
    }

}
