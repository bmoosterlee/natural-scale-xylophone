package notes.envelope;

import main.SampleRate;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CompositeEnvelope extends Envelope {
    Set<Envelope> envelopes;

    public CompositeEnvelope(long startingSampleCount, SampleRate sampleRate){
        super(startingSampleCount, sampleRate, null);
    }

    public CompositeEnvelope(Envelope envelope) {
        super(envelope.startingSampleCount, envelope.sampleRate, null);
        envelopes = new HashSet<>();
        envelopes.add(envelope);
    }

    public CompositeEnvelope(Collection<Envelope> envelopes) {
        this(calculateStartingSampleCount(envelopes), envelopes.iterator().next().sampleRate);
        this.envelopes = new HashSet<>(envelopes);
    }

    private static Long calculateStartingSampleCount(Collection<Envelope> envelopes) {
        Long startingSampleCount = null;
        Iterator<Envelope> iterator = envelopes.iterator();
        while(iterator.hasNext()){
            Envelope envelope = iterator.next();
            long sampleCount = envelope.startingSampleCount;
            if(startingSampleCount==null || sampleCount<startingSampleCount){
                startingSampleCount = sampleCount;
            }
        }
        return startingSampleCount;
    }

    public double getVolume(long sampleCount) {
        double volume = 0.;
        Iterator<Envelope> iterator = envelopes.iterator();
        while(iterator.hasNext()) {
            Envelope envelope = iterator.next();
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
}
