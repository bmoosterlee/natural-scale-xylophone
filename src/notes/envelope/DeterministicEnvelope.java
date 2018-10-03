package notes.envelope;

public interface DeterministicEnvelope extends Envelope {
    long getEndingSampleCount();

    DeterministicCompositeEnvelope add(DeterministicEnvelope envelope);

}
