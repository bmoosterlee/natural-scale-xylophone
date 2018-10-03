package notes.envelope;

public interface DeterministicEnvelope extends Envelope {
    public long getEndingSampleCount();
    DeterministicCompositeEnvelope add(DeterministicEnvelope envelope);

}
