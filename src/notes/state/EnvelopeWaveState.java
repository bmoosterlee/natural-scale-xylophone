package notes.state;

import frequency.Frequency;
import notes.envelope.Envelope;
import wave.Wave;

import java.util.HashMap;
import java.util.Map;

public class EnvelopeWaveState {
    private final long sampleCount;
    private final Map<Frequency, EnvelopeWaveSlice> envelopeWaveSlices;

    EnvelopeWaveState(long sampleCount, Map<Frequency, EnvelopeWaveSlice> envelopeWaveSlices) {
        this.sampleCount = sampleCount;
        this.envelopeWaveSlices = envelopeWaveSlices;
    }

    public EnvelopeWaveState add(Frequency frequency, Envelope envelope, Wave wave) {
        Map<Frequency, EnvelopeWaveSlice> newVolumes = new HashMap<>(envelopeWaveSlices);

        try {
            newVolumes.put(frequency, envelopeWaveSlices.get(frequency).add(envelope));
        }
        catch(NullPointerException e){
            newVolumes.put(frequency, new EnvelopeWaveSlice(sampleCount, envelope, wave));
        }

        return new EnvelopeWaveState(sampleCount, newVolumes);
    }

    public VolumeAmplitudeState calculateValue() {
        Map<Frequency, VolumeAmplitude> volumeAmplitudes = new HashMap<>();

        for(Frequency frequency : envelopeWaveSlices.keySet()){
            volumeAmplitudes.put(frequency, envelopeWaveSlices.get(frequency).calculateValue());
        }

        return new VolumeAmplitudeState(volumeAmplitudes);
    }
}
