package notes.envelope;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class DeterministicCompositeEnvelope extends CompositeEnvelope<DeterministicEnvelope> implements DeterministicEnvelope {
    private final long endingSampleCount;

    public DeterministicCompositeEnvelope(DeterministicEnvelope envelope) {
        super(envelope);
        endingSampleCount = envelope.getEndingSampleCount();
    }

    public DeterministicCompositeEnvelope(Collection<DeterministicEnvelope> envelopes) {
        super(envelopes);
        endingSampleCount = calculateEndingSampleCount(envelopes);
    }

    public DeterministicCompositeEnvelope(Collection<DeterministicEnvelope> envelopes, long startingSampleCount, long endingSampleCount) {
        super(envelopes, startingSampleCount);
        this.endingSampleCount = endingSampleCount;
    }

    protected static Long calculateEndingSampleCount(Collection<DeterministicEnvelope> envelopes) {
        Long endingSampleCount = null;
        Iterator<DeterministicEnvelope> iterator = envelopes.iterator();
        while(iterator.hasNext()){
            DeterministicEnvelope envelope = iterator.next();
            long sampleCount = envelope.getEndingSampleCount();
            if(endingSampleCount==null || sampleCount>endingSampleCount){
                endingSampleCount = sampleCount;
            }
        }
        return endingSampleCount;
    }

    @Override
    public long getEndingSampleCount() {
        return endingSampleCount;
    }

    public DeterministicCompositeEnvelope add(DeterministicEnvelope envelope) {
        Set<DeterministicEnvelope> newEnvelopes = new HashSet<>(envelopes);

        newEnvelopes.add(envelope);

        return new DeterministicCompositeEnvelope(newEnvelopes,
                                                  Math.min(getStartingSampleCount(), envelope.getStartingSampleCount()),
                                                  Math.max(getEndingSampleCount(), envelope.getEndingSampleCount()));
    }

    public Envelope add(DeterministicCompositeEnvelope envelope) {
        Set<DeterministicEnvelope> newEnvelopes = new HashSet<>(envelopes);

        newEnvelopes.addAll(envelope.envelopes);

        return new DeterministicCompositeEnvelope(newEnvelopes,
                                                  Math.min(getStartingSampleCount(), envelope.getStartingSampleCount()),
                                                  Math.max(getEndingSampleCount(), envelope.getEndingSampleCount()));
    }

    @Override
    public boolean isDead(long sampleCount) {
        return sampleCount >= getEndingSampleCount();
    }
}
