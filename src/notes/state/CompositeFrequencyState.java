package notes.state;

import notes.Frequency;
import notes.Note;
import notes.envelope.Envelope;

import java.util.*;

public class CompositeFrequencyState implements FrequencyState {
    public final Set<Frequency> frequencies;
    public final Map<Frequency, Envelope> frequencyEnvelopes;

    public CompositeFrequencyState() {
        frequencies = new HashSet<>();
        frequencyEnvelopes = new HashMap<>();
    }

    public CompositeFrequencyState(Set<Frequency> frequencies, Map<Frequency, Envelope> frequencyEnvelopes) {
        this.frequencies = frequencies;
        this.frequencyEnvelopes = frequencyEnvelopes;
    }

    public CompositeFrequencyState addNote(Note note) {
        Frequency frequency = note.getFrequency();

        Map<Frequency, Envelope> newFrequencyEnvelopes = new HashMap<>(frequencyEnvelopes);
        Set<Frequency> newFrequencies = frequencies;

        if (!frequencies.contains(frequency)) {
            newFrequencies = new HashSet<>(frequencies);
            newFrequencies.add(frequency);
        }
        Envelope newEnvelope;
        try {
            newEnvelope = frequencyEnvelopes.get(frequency).add(note.getEnvelope());
        }
        catch(NullPointerException e){
            newEnvelope = note.getEnvelope();
        }
        newFrequencyEnvelopes.put(frequency, newEnvelope);

        return new CompositeFrequencyState(newFrequencies, newFrequencyEnvelopes);
    }

    public Set<Frequency> getFrequencies(){
        return new HashSet<>(frequencies);
    }

    @Override
    public FrequencyState removeNote(Note note) {
        return this;
    }

    public Map<Frequency, Double> getFrequencyVolumeTable(long sampleCount) {
        Map<Frequency, Double> frequencyVolumes = new HashMap<>();

        Iterator<Frequency> iterator = frequencies.iterator();
        while (iterator.hasNext()) {
            Frequency frequency = iterator.next();
            Double volume = frequencyEnvelopes.get(frequency).getVolume(sampleCount);
            frequencyVolumes.put(frequency, volume);
        }

        return frequencyVolumes;
    }

    @Override
    public Double getVolume(Frequency frequency, long sampleCount) {
        return frequencyEnvelopes.get(frequency).getVolume(sampleCount);
    }

}