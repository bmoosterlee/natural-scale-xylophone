package notes;

import frequency.Frequency;
import notes.envelope.Envelope;

public class Note {
    //todo create a wave class which can precalculate amplutides.
    //todo we give the wave class a starting sample count, and an end sample count or timeframe in samples
    //todo the wave will then precalculate it's amplitudes for these samples

    //todo wave should share their information, since the amplutides for a wave are the same for each wave of that
    //todo frequency. The wave is thus not part of a note, but part of a frequency.

    //todo however, a wave has a frequency, not the other way around.
    //todo so wave have a frequency object within them

    //todo by default, wave only have an amplitude calculation function.
    //todo the precalculated wave will be built on top of the wave structure, and have a start and end time.
    //todo if we use immutable data structures to store the wave data, we can easily build a new precalc wave
    //todo on top of an old one, by simply copying the data structure and adding onto it, by using a zipper for example.

    private final Frequency frequency;
    private final Envelope envelope;

    public Note(Frequency frequency, Envelope envelope){
        this.frequency = frequency;
        this.envelope = envelope;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public boolean isDead(long sampleCount) {
        return getEnvelope().isDead(sampleCount);
    }
}
