package mixer.envelope;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CompositeEnvelope<T extends Envelope> extends SimpleEnvelope {
    final Set<T> envelopes;

    CompositeEnvelope(T envelope) {
        super(envelope.getStartingSampleCount(), envelope.getSampleRate(), null);
        envelopes = new HashSet<>();
        envelopes.add(envelope);
    }

    CompositeEnvelope(Collection<T> envelopes) {
        this(envelopes, calculateStartingSampleCount(envelopes));
    }

    CompositeEnvelope(Collection<T> envelopes, long startingSampleCount) {
        super(startingSampleCount, envelopes.iterator().next().getSampleRate(), null);
        this.envelopes = new HashSet<>(envelopes);
    }

    private static Long calculateStartingSampleCount(Collection<? extends Envelope> envelopes) {
        Long startingSampleCount = null;

        for(Envelope envelope : envelopes){
            long sampleCount = envelope.getStartingSampleCount();
            if(startingSampleCount==null || sampleCount<startingSampleCount){
                startingSampleCount = sampleCount;
            }
        }

        return startingSampleCount;
    }

    public double getVolume(long sampleCount) {
        double volume = 0.;

        for(T envelope : envelopes){
            volume += envelope.getVolume(sampleCount);
        }

        return volume;
    }

    @Override
    public CompositeEnvelope add(Envelope envelope) {
        Set<Envelope> newEnvelopes = new HashSet<>(envelopes);

        newEnvelopes.add(envelope);

        return new CompositeEnvelope<>(newEnvelopes);
    }

    public Envelope add(CompositeEnvelope envelope) {
        Set<Envelope> newEnvelopes = new HashSet<>(envelopes);

        newEnvelopes.addAll(envelope.envelopes);

        return new CompositeEnvelope<>(newEnvelopes);
    }

    @Override
    public Envelope update(long sampleCount){
        Envelope newEnvelope = null;

        for(Envelope envelope : envelopes){
            Envelope update = envelope.update(sampleCount);
            if(update!=null){
                if(newEnvelope == null){
                    newEnvelope = update;
                }
                else {
                    newEnvelope.add(update);
                }
            }
        }

        return newEnvelope;
    }
}
