package pianola;/*todo write a history tracker of when notes were played. Take average of notes played to form smoothed peaks
 * todo find maxima of these smoothed values. Perform fourier analysis on this signal, also obtaining the phases of
 * todo a frequency. find harmonic image of each frequency, multiply image by the phase of the tonic.
 * todo add all these phase dependent harmonic images together, and display the rhythmic value in real time.
 * todo let the pianola take the rhythmic harmonic value in real time and play that.
 * todo use a lookahead of the length of the shortest frame for the pianola, and play the maximum within that frame*/

import component.*;
import frequency.Frequency;
import pianola.patterns.PianolaPattern;
import spectrum.buckets.BucketHistory;
import spectrum.buckets.Buckets;
import spectrum.buckets.BucketsAverager;
import spectrum.buckets.PrecalculatedBucketHistory;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Set;

public class Pianola extends TickablePipeComponent<Pulse, Frequency>{

    public Pianola(BoundedBuffer<Pulse> tickBuffer, BoundedBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> spectrumBuffer, BoundedBuffer<Frequency> outputBuffer, PianolaPattern pianolaPattern, int inaudibleFrequencyMargin) {
        super(tickBuffer, outputBuffer, build(spectrumBuffer, pianolaPattern,inaudibleFrequencyMargin));
    }

    public static CallableWithArguments<Pulse, Frequency> build(BoundedBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> spectrumBuffer, PianolaPattern pianolaPattern, int inaudibleFrequencyMargin){
        return new CallableWithArguments<>() {
            private final InputPort<Buckets> notesInput;
            private BucketHistory noteHistory;
            private final int repetitionDampener;
            private final OutputPort<Buckets> averagerInput;

            private final InputPort<Buckets> preparedNotesInput;
            private final InputPort<Buckets> preparedHarmonicsInput;

            private InputPort<Frequency> methodOutputPort;

            private OutputPort<Collection<Frequency>> outputPort;

            {
                int count = 0;
                int capacity = 1000;

                repetitionDampener = 3;
                noteHistory = new PrecalculatedBucketHistory(50);

                BoundedBuffer<Buckets> noteSpectrumBuffer = new BoundedBuffer<>(capacity, "pianola - note spectrum");
                BoundedBuffer<Buckets> harmonicSpectrumBuffer = new BoundedBuffer<>(capacity, "pianola - harmonic spectrum");
                new Unpairer<>(spectrumBuffer, noteSpectrumBuffer, harmonicSpectrumBuffer);

                BoundedBuffer<Buckets> notesAveragerInputBuffer = new BoundedBuffer<>(capacity, "Pianola" + String.valueOf(count));
                count++;
                BoundedBuffer<Buckets> notesAveragerOutputBuffer = new BoundedBuffer<>(capacity, "Pianola" + String.valueOf(count));
                count++;
                new BucketsAverager(2 * inaudibleFrequencyMargin, notesAveragerInputBuffer, notesAveragerOutputBuffer);
                averagerInput = new OutputPort<>(notesAveragerInputBuffer);

                BoundedBuffer<Buckets> harmonicsAveragerBuffer = new BoundedBuffer<>(capacity, "Pianola" + String.valueOf(count));
                count++;
                new BucketsAverager(inaudibleFrequencyMargin, harmonicSpectrumBuffer, harmonicsAveragerBuffer);

                BoundedBuffer<Collection<Frequency>> patternOutput = new BoundedBuffer<>(1, "pianola - output");
                outputPort = patternOutput.createOutputPort();
                BoundedBuffer<Frequency> methodOutput = toBuffer(patternOutput);
                methodOutputPort = methodOutput.createInputPort();

                notesInput = new InputPort<>(noteSpectrumBuffer);
                preparedNotesInput = new InputPort<>(notesAveragerOutputBuffer);
                preparedHarmonicsInput = new InputPort<>(harmonicsAveragerBuffer);
            }

            private Frequency playNotes(Pulse input) {
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

                    Set<Frequency> results = pianolaPattern.playPattern(noteBuckets, harmonicsBuckets);
                    outputPort.produce(results);

                    return methodOutputPort.consume();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public Frequency call(Pulse input) {
                return playNotes(input);
            }
        };
    }

    public static <T> BoundedBuffer<T> toBuffer(BoundedBuffer<Collection<T>> input){
        BoundedBuffer<T> outputBuffer = new BoundedBuffer<>(1, "toBuffer - output");
        InputPort<Collection<T>> inputPort = input.createInputPort();
        OutputPort<T> outputPort = outputBuffer.createOutputPort();

        new Tickable() {
            @Override
            protected void tick() {
                try {
                    Collection<T> input = inputPort.consume();
                    for(T element : input){
                        outputPort.produce(element);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        return outputBuffer;
    }
}
