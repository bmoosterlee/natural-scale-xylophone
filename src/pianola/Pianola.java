package pianola;/*todo write a history tracker of when notes were played. Take average of notes played to form smoothed peaks
 * todo find maxima of these smoothed values. Perform fourier analysis on this signal, also obtaining the phases of
 * todo a frequency. find harmonic image of each frequency, multiply image by the phase of the tonic.
 * todo add all these phase dependent harmonic images together, and display the rhythmic value in real time.
 * todo let the pianola take the rhythmic harmonic value in real time and play that.
 * todo use a lookahead of the length of the shortest frame for the pianola, and play the maximum within that frame*/

import component.Pulse;
import component.TimedConsumer;
import component.buffer.*;
import frequency.Frequency;
import pianola.patterns.PianolaPattern;
import spectrum.buckets.Buckets;
import spectrum.buckets.BucketsAverager;
import spectrum.buckets.PrecalculatedBucketHistoryComponent;

import java.util.LinkedList;
import java.util.List;

public class Pianola {
    private final PianolaPattern pianolaPattern;

    private final InputPort<Buckets> preparedNotesInput;
    private final InputPort<Buckets> preparedHarmonicsInput;

    private final OutputPort<List<Frequency>> outputPort;

    private final TickRunner tickRunner = new MyTickRunner();

    public Pianola(SimpleBuffer<Pulse> tickBuffer, BoundedBuffer<Buckets> noteSpectrumBuffer, BoundedBuffer<Buckets> harmonicSpectrumBuffer, SimpleBuffer<List<Frequency>> outputBuffer, PianolaPattern pianolaPattern, int inaudibleFrequencyMargin) {
        this.pianolaPattern = pianolaPattern;

        int repetitionDampener = 3;

        LinkedList<SimpleBuffer<Pulse>> tickBroadcast = new LinkedList<>(tickBuffer.broadcast(2, "pianola tick - broadcast"));
        preparedNotesInput =
            tickBroadcast.poll()
            .performMethod(TimedConsumer.consumeFrom(noteSpectrumBuffer), "pianola - consume from note spectrum buffer")
            .performMethod(PrecalculatedBucketHistoryComponent.recordHistory(50))
            .performMethod(input -> input.multiply(repetitionDampener))
            .performMethod(BucketsAverager.build(2 * inaudibleFrequencyMargin))
            .createInputPort();

        preparedHarmonicsInput =
            tickBroadcast.poll()
            .performMethod(TimedConsumer.consumeFrom(harmonicSpectrumBuffer), "pianola - consume from harmonic spectrum buffer")
            .performMethod(BucketsAverager.build(inaudibleFrequencyMargin))
            .createInputPort();

        outputPort = outputBuffer.createOutputPort();

        start();
    }

    private void tick() {
        try {
            Buckets noteBuckets = preparedNotesInput.consume();
            Buckets harmonicsBuckets = preparedHarmonicsInput.consume();

            LinkedList<Frequency> results = new LinkedList<>(pianolaPattern.playPattern(noteBuckets, harmonicsBuckets));

            outputPort.produce(results);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void start(){
        tickRunner.start();
    }

    private class MyTickRunner extends TickRunner {
        @Override
        protected void tick() {
            Pianola.this.tick();
        }
    }
}
