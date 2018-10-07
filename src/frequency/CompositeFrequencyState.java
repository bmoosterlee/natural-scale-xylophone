package frequency;

import notes.Note;

import java.util.*;

public class CompositeFrequencyState extends SimpleFrequencyState<CompositeFrequencyStateBucket> {
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

    public CompositeFrequencyState(Set<Note> notes, Set<Frequency> frequencies, Map<Frequency,
            CompositeFrequencyStateBucket> buckets, Factory<CompositeFrequencyStateBucket> factory) {
        super(notes, frequencies, buckets, factory);
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
        FrequencyState newFrequencyState = this;

        for(Frequency frequency : getFrequencies()){
            newFrequencyState = updateFrequency(frequency, sampleCount);
        }

        return newFrequencyState;
    }

    private FrequencyState updateFrequency(Frequency frequency, long sampleCount) {
        Set<Frequency> newFrequencies = new HashSet<>(getFrequencies());

        Map<Frequency, CompositeFrequencyStateBucket> newFrequencyEnvelopes = new HashMap<>(buckets);

        CompositeFrequencyStateBucket update = buckets.get(frequency).update(sampleCount);
        if(update==null){
            newFrequencies.remove(frequency);
            newFrequencyEnvelopes.remove(frequency);
        }
        else {
            newFrequencyEnvelopes.put(frequency, update);
        }

        return new CompositeFrequencyState(notes, newFrequencies, newFrequencyEnvelopes, factory);
    }
}