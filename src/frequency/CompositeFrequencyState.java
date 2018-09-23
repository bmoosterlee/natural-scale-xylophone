package frequency;

import notes.Note;
import notes.envelope.Envelope;

import java.util.*;

public class CompositeFrequencyState implements FrequencyState {
    //todo the issue is that all old frequencies and envelopes will stack up
    //todo one way to alleviate this is to implement the precalc envelope class,
    //todo which then allows us to create a real compositeEnvelope by adding them
    //todo together. This will be an instance of precalc envelope
    //todo after that, we will stil have the issue of frequencies stacking up.
    //todo we do know when a frequency died based on the envelope,
    //todo but since the object has no idea what samplecount we're on,
    //todo we can't flush it. One way to solve this would be to call an update
    //todo method on the object which is called by the noteEnvironment.
    //todo when we update the frequencyState, we also clip the precalced envelopes
    //todo by calling an update method there. This would involve setting the starting
    //todo samplecount, and only copying data from after that moment.
    public final Set<Note> notes;
    public final Set<Frequency> frequencies;
    public final Map<Frequency, Envelope> frequencyEnvelopes;

    public CompositeFrequencyState() {
        notes = new HashSet<>();
        frequencies = new HashSet<>();
        frequencyEnvelopes = new HashMap<>();
    }

    public CompositeFrequencyState(Set<Note> notes, Set<Frequency> frequencies, Map<Frequency, Envelope> frequencyEnvelopes) {
        this.notes = notes;
        this.frequencies = frequencies;
        this.frequencyEnvelopes = frequencyEnvelopes;
    }

    public CompositeFrequencyState addNote(Note note) {
        return addNote(note, note.getFrequency(), note.getEnvelope());
    }

    private CompositeFrequencyState addNote(Note note, Frequency frequency, Envelope envelope) {
        Set<Note> newNotes = new HashSet<>(notes);
        newNotes.add(note);
        Set<Frequency> newFrequencies = addFrequency(frequency, frequencies);
        Map<Frequency, Envelope> newFrequencyEnvelopes = addFrequencyEnvelope(frequency, envelope, frequencyEnvelopes);

        return new CompositeFrequencyState(newNotes, newFrequencies, newFrequencyEnvelopes);
    }

    protected Map<Frequency, Envelope> addFrequencyEnvelope(Frequency frequency, Envelope envelope, Map<Frequency, Envelope> frequencyEnvelopes) {
        Map<Frequency, Envelope> newFrequencyEnvelopes = new HashMap<>(frequencyEnvelopes);
        Envelope newEnvelope;
        try {
            newEnvelope = frequencyEnvelopes.get(frequency).add(envelope);
        }
        catch(NullPointerException e){
            newEnvelope = envelope;
        }
        newFrequencyEnvelopes.put(frequency, newEnvelope);
        return newFrequencyEnvelopes;
    }

    protected Set<Frequency> addFrequency(Frequency frequency, Set<Frequency> frequencies) {
        Set<Frequency> newFrequencies = new HashSet<>(frequencies);
        if (!newFrequencies.contains(frequency)) {
            newFrequencies = new HashSet<>(newFrequencies);
            newFrequencies.add(frequency);
        }
        return newFrequencies;
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

    @Override
    public FrequencyState update(Set<Note> notes) {
        if(notes.equals(this.notes)){
            return this;
        }

        FrequencyState newFrequencyState = this;

        Set<Note> removedNotes = new HashSet<>(this.notes);
        removedNotes.removeAll(new HashSet<>(notes));

        Set<Note> addedNotes = new HashSet<>(notes);
        addedNotes.removeAll(new HashSet<>(this.notes));

        if(!removedNotes.isEmpty()) {
            Iterator<Note> iterator = removedNotes.iterator();
            while (iterator.hasNext()) {
                newFrequencyState = newFrequencyState.removeNote(iterator.next());
            }
        }

        if(!addedNotes.isEmpty()) {
            Iterator<Note> iterator = addedNotes.iterator();
            while (iterator.hasNext()) {
                newFrequencyState = newFrequencyState.addNote(iterator.next());
            }
        }

        return newFrequencyState;
    }

    @Override
    public FrequencyState update(long sampleCount) {
        Set<Note> newNotes = new HashSet<>(notes);
        Set<Frequency> newFrequencies = new HashSet<>(getFrequencies());
        Map<Frequency, Envelope> newFrequencyEnvelopes = new HashMap<>(frequencyEnvelopes);

        for(Frequency frequency : getFrequencies()){
            Envelope update = frequencyEnvelopes.get(frequency).update(sampleCount);
            if(update==null){
                newFrequencies.remove(frequency);
                newFrequencyEnvelopes.remove(frequency);
            }
            else {
                newFrequencyEnvelopes.put(frequency, update);
            }
        }

        return new CompositeFrequencyState(newNotes, newFrequencies, newFrequencyEnvelopes);
    }
}