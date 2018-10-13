package notes.envelope;

import sound.SampleRate;

import java.util.*;

public class PrecalculatedEnvelope extends SimpleDeterministicEnvelope {
    private final Map<Long, Double> volumes;

    public PrecalculatedEnvelope(DeterministicEnvelope deterministicEnvelope){
        super(deterministicEnvelope.getStartingSampleCount(), deterministicEnvelope.getSampleRate(), deterministicEnvelope.getEndingSampleCount());
        volumes = new HashMap<>();
        for(long i = deterministicEnvelope.getStartingSampleCount(); i<deterministicEnvelope.getEndingSampleCount(); i++){
            double volume = deterministicEnvelope.getVolume(i);
            if(volume!=0) {
                volumes.put(i, volume);
            }
        }
    }

    private PrecalculatedEnvelope(long startingSampleCount, long endingSampleCount, SampleRate sampleRate, Map<Long, Double> volumes) {
        super(startingSampleCount, sampleRate, endingSampleCount);
        this.volumes = volumes;
    }

    @Override
    public double getVolume(long sampleCount) {
        try{
            return volumes.get(sampleCount);
        }
        catch(NoSuchElementException | NullPointerException e){
            return 0.;
        }
    }

    public PrecalculatedEnvelope add(PrecalculatedEnvelope envelope) {
        Map<Long, Double> newVolumes = new HashMap<>(volumes);
        newVolumes.putAll(envelope.volumes);
        return new PrecalculatedEnvelope(Math.min(getStartingSampleCount(), envelope.getStartingSampleCount()), Math.max(getEndingSampleCount(), envelope.getEndingSampleCount()), getSampleRate(), newVolumes);
    }

    @Override
    public Envelope update(long sampleCount){

        if(isDead(sampleCount)) {
            return null;
        }
        else {
            Map<Long, Double> newVolumes = new HashMap<>();
            for (Long i : volumes.keySet()) {
                if (i >= sampleCount) {
                    newVolumes.put(i, volumes.get(i));
                }
            }
            return new PrecalculatedEnvelope(sampleCount, getEndingSampleCount(), getSampleRate(), newVolumes);
            // return this;
        }
    }

}
