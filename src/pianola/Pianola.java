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

import java.util.*;

public class Pianola<I extends Packet<Pulse>, N extends Packet<Buckets>, H extends Packet<Buckets>, O extends Packet<List<Frequency>>> {

    public Pianola(BoundedBuffer<Pulse, I> tickBuffer, BoundedBuffer<Buckets, N> noteSpectrumBuffer, BoundedBuffer<Buckets, H> harmonicSpectrumBuffer, SimpleBuffer<List<Frequency>, O> outputBuffer, PianolaPattern pianolaPattern, int inaudibleFrequencyMargin) {
        int repetitionDampener = 3;

        LinkedList<SimpleBuffer<Pulse, I>> tickBroadcast = new LinkedList<>(tickBuffer.broadcast(2, "pianola tick - broadcast"));

        tickBroadcast.poll()
        .performMethod(TimedConsumer.consumeFrom(noteSpectrumBuffer), "pianola - consume from note spectrum buffer")
        .connectTo(PrecalculatedBucketHistoryComponent.buildPipe(50))
        .performMethod(input -> input.multiply(repetitionDampener), "pianola - multiply note spectrum")
        .performMethod(BucketsAverager.build(2 * inaudibleFrequencyMargin), "pianola - average note spectrum")
        .pairWith(
            tickBroadcast.poll()
            .performMethod(TimedConsumer.consumeFrom(harmonicSpectrumBuffer), "pianola - consume from harmonic spectrum buffer")
            .performMethod(BucketsAverager.build(inaudibleFrequencyMargin), "pianola - average harmonic spectrum"))
        .performMethod(input -> new LinkedList<>(pianolaPattern.playPattern(input.getKey(), input.getValue())), "pianola - play pattern")
        .relayTo(outputBuffer);
    }

}
