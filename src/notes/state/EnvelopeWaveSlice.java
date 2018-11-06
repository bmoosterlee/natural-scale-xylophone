package notes.state;

import notes.envelope.Envelope;
import wave.Wave;

//todo move to composite volume. Do this by having a volume object which either has atomic volume (value) or composite volume (set)
public class EnvelopeWaveSlice {
    private final long sampleCount;
    private final Envelope envelope;
    private final Wave wave;

    EnvelopeWaveSlice(long samplecount, Envelope envelope, Wave wave) {
        this.sampleCount = samplecount;
        this.envelope = envelope;
        this.wave = wave;
    }

    public EnvelopeWaveSlice add(Envelope newEnvelope) {
        return new EnvelopeWaveSlice(sampleCount, envelope.add(newEnvelope), wave);
    }

    //todo place calculateValue calls in envelope and wave as well.
    //todo this way we can calculate immediately when necessary.
    VolumeAmplitude calculateValue(){
        return new VolumeAmplitude(envelope.getVolume(sampleCount), wave.getAmplitude(sampleCount));
    }
}
