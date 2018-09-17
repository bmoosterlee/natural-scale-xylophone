package notes.envelope;

import notes.Frequency;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CompositeEnvelope<T extends Envelope> extends SimpleEnvelope {
    final Set<T> envelopes;

    public CompositeEnvelope(T envelope) {
        super(envelope.getStartingSampleCount(), envelope.getSampleRate(), null, null);
        envelopes = new HashSet<>();
        envelopes.add(envelope);
    }

    public CompositeEnvelope(Collection<T> envelopes) {
        this(envelopes, calculateStartingSampleCount(envelopes));
    }

    public CompositeEnvelope(Collection<T> envelopes, long startingSampleCount) {
        super(startingSampleCount, envelopes.iterator().next().getSampleRate(), null, null);
        this.envelopes = new HashSet<>(envelopes);
    }

    protected static Long calculateStartingSampleCount(Collection<? extends Envelope> envelopes) {
        Long startingSampleCount = null;
        Iterator<? extends Envelope> iterator = envelopes.iterator();
        while(iterator.hasNext()){
            Envelope envelope = iterator.next();
            long sampleCount = envelope.getStartingSampleCount();
            if(startingSampleCount==null || sampleCount<startingSampleCount){
                startingSampleCount = sampleCount;
            }
        }
        return startingSampleCount;
    }

    public double getVolume(long sampleCount) {
        double volume = 0.;
        Iterator<T> iterator = envelopes.iterator();
        while(iterator.hasNext()) {
            T envelope = iterator.next();
            volume += envelope.getVolume(sampleCount);
        }
        return volume;
    }

    @Override
    public CompositeEnvelope add(Envelope envelope) {
        Set<Envelope> newEnvelopes = new HashSet<>(envelopes);

        newEnvelopes.add(envelope);

        return new CompositeEnvelope(newEnvelopes);
    }

    public Envelope add(CompositeEnvelope envelope) {
        Set<Envelope> newEnvelopes = new HashSet<>(envelopes);

        newEnvelopes.addAll(envelope.envelopes);

        return new CompositeEnvelope(newEnvelopes);
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
