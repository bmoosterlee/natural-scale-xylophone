package frequency.state;

import frequency.Frequency;
import notes.Note;

import java.util.*;

public class SimpleFrequencyState<T extends FrequencyStateBucket> implements FrequencyState {
    public final Set<Note> notes;
    public final Set<Frequency> frequencies;
    public final Map<Frequency, T> buckets;
    protected final Factory<T> factory;

    public SimpleFrequencyState(Factory<T> factory) {
        notes = new HashSet<>();
        frequencies = new HashSet<>();
        buckets = new HashMap<>();
        this.factory = factory;
    }

    protected SimpleFrequencyState(Set<Note> notes, Set<Frequency> frequencies, Map<Frequency, T> buckets, Factory<T> factory) {
        this.notes = notes;
        this.frequencies = frequencies;
        this.buckets = buckets;
        this.factory = factory;
    }

    public Map<Frequency, Double> getFrequencyVolumeTable(long sampleCount) {
        Map<Frequency, Double> frequencyVolumes = new HashMap<>();

        for(Frequency frequency : frequencies){
            frequencyVolumes.put(frequency, getVolume(frequency, sampleCount));
        }
        return frequencyVolumes;
    }

    public Double getVolume(Frequency frequency, long sampleCount) {
        try {
            return buckets.get(frequency).getVolume(sampleCount);
        }
        catch(NullPointerException e){
            return 0.;
        }
    }

    public FrequencyState update(Set<Note> notes) {
        FrequencyState newFrequencyState = this;

        Set<Note> removedNotes = new HashSet<>(this.notes);
        removedNotes.removeAll(notes);

        Set<Note> addedNotes = new HashSet<>(notes);
        addedNotes.removeAll(this.notes);

        for(Note note : removedNotes){
            newFrequencyState = newFrequencyState.removeNote(note);
        }

        for(Note note : addedNotes){
            newFrequencyState = newFrequencyState.addNote(note);
        }

        return newFrequencyState;
    }

    public SimpleFrequencyState<T> removeNote(Note note) {
        Set<Note> newNotes = new HashSet<>(notes);
        newNotes.remove(note);

        Frequency frequency = note.getFrequency();

        Set<Frequency> newFrequencies = new HashSet<>(getFrequencies());
        Map<Frequency, T> newBuckets = new HashMap<>(buckets);
        T newBucket = null;
        try {
            newBucket = newBuckets.get(frequency).removeNote(note);
        }
        catch(NullPointerException e){

        }
        if(newBucket==null){
            newFrequencies.remove(frequency);
            newBuckets.remove(frequency);
        }
        else{
            newBuckets.put(frequency, newBucket);
        }

        return new SimpleFrequencyState<>(newNotes, newFrequencies, newBuckets, factory);
    }

    public FrequencyState update(long sampleCount) {
        return this;
    }

    public SimpleFrequencyState addNote(Note note) {
        Frequency frequency = note.getFrequency();
        Set<Note> newNotes = new HashSet<>(notes);
        newNotes.add(note);

        Set<Frequency> newFrequencies = frequencies;
        if (!newFrequencies.contains(frequency)) {
            newFrequencies = new HashSet<>(frequencies);
            newFrequencies.add(frequency);
        }

        Map<Frequency, T> newBuckets = new HashMap<>(buckets);
        T newBucket;
        try {
            newBucket = newBuckets.get(frequency).addNote(note);
        }
        catch(NullPointerException e){
            newBucket = factory.make().addNote(note);
        }
        newBuckets.put(frequency, newBucket);

        return new SimpleFrequencyState<>(newNotes, newFrequencies, newBuckets, factory);
    }

    public Set<Frequency> getFrequencies(){
        return new HashSet<>(frequencies);
    }

}