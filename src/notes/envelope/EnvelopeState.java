package notes.envelope;

import notes.Note;
import notes.envelope.functions.DeterministicFunction;
import notes.envelope.functions.LinearFunctionMemoizer;
import sound.SampleRate;
import time.TimeInSeconds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EnvelopeState {
    Set<Note> notes;
    Map<Note, Envelope> map;

    SampleRate sampleRate;

    private final DeterministicFunction envelopeFunction;

    public EnvelopeState(SampleRate sampleRate){
        this(sampleRate, new HashSet<>(), new HashMap<>());
    }

    public EnvelopeState(SampleRate sampleRate, Set<Note> notes, Map<Note, Envelope> map) {
        this.sampleRate = sampleRate;
        envelopeFunction = LinearFunctionMemoizer.ENVELOPE_MEMOIZER.get(this.sampleRate, 0.10, new TimeInSeconds(0.7));

        this.notes = notes;
        this.map = map;
    }

    public EnvelopeState update(Set<Note> notes) {
        if(notes.equals(this.notes)){
            return this;
        }

        Map<Note, Envelope> newMap = new HashMap<>(map);

        Set<Note> addedNotes = new HashSet<>(notes);
        addedNotes.removeAll(this.notes);

        Set<Note> removedNotes = new HashSet<>(this.notes);
        removedNotes.removeAll(notes);

        for(Note note : addedNotes){
            newMap.put(note, new SimpleDeterministicEnvelope(note.getStartingCount(), sampleRate, envelopeFunction));
        }

        for(Note note : removedNotes){
            newMap.remove(note);
        }

        return new EnvelopeState(sampleRate, notes, newMap);
    }

    public EnvelopeState update(long sampleCount) {
        return this;
    }

    public Envelope getNote(Note note) {
        return map.get(note);
    }

    public Set<Note> getDeadNotes(long sampleCount) {
        Set<Note> deadNotes = new HashSet<>();

        for(Note note : notes){
            if(map.get(note).isDead(sampleCount)){
                deadNotes.add(note);
            }
        }

        return deadNotes;
    }
}
