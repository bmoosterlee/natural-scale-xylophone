package frequency.state;

import frequency.Frequency;
import notes.Note;
import notes.envelope.EnvelopeState;

import java.util.*;

public class FrequencyState {
    //todo find a way to create events and event listeners for adding notes and frequencies.
    //todo either that, or have a singular pipeline down for creating notes and frequencies.
    //todo explore including envelopes in buckets for faster buckets value calculation
    //todo of course, this would mean we're excluding other volume calculations.
    //todo to assess whether this is useful and doesn't require double volume calculation,
    //todo we should encapsulate accessing volumes to see where we can hide implementation details.

    public final Set<Note> notes;
    public final Set<Frequency> frequencies;
    public final Map<Frequency, Set<Note>> buckets;

    public FrequencyState() {
        notes = new HashSet<>();
        frequencies = new HashSet<>();
        buckets = new HashMap<>();
    }

    protected FrequencyState(Set<Note> notes, Set<Frequency> frequencies, Map<Frequency, Set<Note>> buckets) {
        this.notes = notes;
        this.frequencies = frequencies;
        this.buckets = buckets;
    }

    public Map<Frequency, Double> getFrequencyVolumeTable(EnvelopeState envelopeState, long sampleCount) {
        Map<Frequency, Double> frequencyVolumes = new HashMap<>();

        for(Frequency frequency : frequencies){
            frequencyVolumes.put(frequency, getVolume(frequency, envelopeState, sampleCount));
        }
        return frequencyVolumes;
    }

    public Double getVolume(Frequency frequency, EnvelopeState envelopeState, long sampleCount) {
        try {
            Double volume = 0.;
            for(Note note : buckets.get(frequency)) {
                volume += envelopeState.getNote(note).getVolume(sampleCount);
            }
            return volume;
        }
        catch(NullPointerException e){
            return 0.;
        }
    }

    public FrequencyState update(Set<Note> notes) {
        FrequencyState newFrequencyState = this;

        Set<Note> addedNotes = new HashSet<>(notes);
        addedNotes.removeAll(this.notes);

        Set<Note> removedNotes = new HashSet<>(this.notes);
        removedNotes.removeAll(notes);

        for(Note note : removedNotes){
            newFrequencyState = newFrequencyState.removeNote(note);
        }

        for(Note note : addedNotes){
            newFrequencyState = newFrequencyState.addNote(note);
        }

        return newFrequencyState;
    }

    public FrequencyState removeNote(Note note) {
        Set<Note> newNotes = new HashSet<>(notes);
        newNotes.remove(note);

        Frequency frequency = note.getFrequency();

        Set<Frequency> newFrequencies = new HashSet<>(getFrequencies());
        Map<Frequency, Set<Note>> newBuckets = buckets;
        Set<Note> oldBucket = newBuckets.get(frequency);
        if(oldBucket!=null) {
            newBuckets = new HashMap<>(buckets);

            Set<Note> newBucket = new HashSet<>(oldBucket);
            newBucket.remove(note);

            if (newBucket.isEmpty()) {
                newFrequencies.remove(frequency);
                newBuckets.remove(frequency);
            } else {
                newBuckets.put(frequency, newBucket);
            }
        }

        return new FrequencyState(newNotes, newFrequencies, newBuckets);
    }

    public FrequencyState update(long sampleCount) {
        return this;
    }

    public FrequencyState addNote(Note note) {
        Frequency frequency = note.getFrequency();
        Set<Note> newNotes = new HashSet<>(notes);
        newNotes.add(note);

        Set<Frequency> newFrequencies;

        Map<Frequency, Set<Note>> newBuckets = new HashMap<>(buckets);
        Set<Note> newBucket;

        if(!buckets.containsKey(frequency)) {
            newFrequencies = new HashSet<>(frequencies);
            newFrequencies.add(frequency);

            newBucket = new HashSet<>();
            newBuckets.put(frequency, newBucket);
        }
        else {
            newFrequencies = frequencies;

            newBucket = new HashSet<>(buckets.get(frequency));
        }

        newBucket.add(note);

        return new FrequencyState(newNotes, newFrequencies, newBuckets);
    }

    public Set<Frequency> getFrequencies(){
        return new HashSet<>(frequencies);
    }

}