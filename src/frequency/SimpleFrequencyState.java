package frequency;

import notes.Note;

import java.util.*;

public class SimpleFrequencyState implements FrequencyState {
    public final Set<Note> notes;
    public final Set<Frequency> frequencies;
    public final Map<Frequency, Set<Note>> frequencyNoteTable;

    public SimpleFrequencyState() {
        notes = new HashSet<>();
        frequencies = new HashSet<>();
        frequencyNoteTable = new HashMap<>();
    }

    public SimpleFrequencyState(Set<Note> notes, Set<Frequency> frequencies, Map<Frequency, Set<Note>> frequencyNoteTable) {
        this.notes = notes;
        this.frequencies = frequencies;
        this.frequencyNoteTable = frequencyNoteTable;
    }

    public Map<Frequency, Double> getFrequencyVolumeTable(long sampleCount) {
        Map<Frequency, Double> frequencyVolumes = new HashMap<>();

        Iterator<Frequency> iterator = frequencies.iterator();
        while (iterator.hasNext()) {
            Frequency frequency = iterator.next();
            Double volume = 0.;
            Iterator<Note> noteIterator = frequencyNoteTable.get(frequency).iterator();
            while (noteIterator.hasNext()) {
                Note note = noteIterator.next();
                try {
                    volume += note.getEnvelope().getVolume(sampleCount);
                } catch (NullPointerException e) {
                    continue;
                }
            }
            frequencyVolumes.put(frequency, volume);
        }
        return frequencyVolumes;
    }

    @Override
    public Double getVolume(Frequency frequency, long sampleCount) {
        Double volume = 0.;
        Iterator<Note> noteIterator = frequencyNoteTable.get(frequency).iterator();
        while (noteIterator.hasNext()) {
            Note note = noteIterator.next();
            try {
                volume += note.getEnvelope().getVolume(sampleCount);
            } catch (NullPointerException e) {
                continue;
            }
        }
        return volume;
    }

    @Override
    public FrequencyState update(Set<Note> notes) {
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

    public SimpleFrequencyState removeNote(Note note) {
        Frequency frequency = note.getFrequency();

        Set<Note> newNotes = new HashSet<>(notes);
        Map<Frequency, Set<Note>> newFrequencyNoteTable = copyFrequencyNoteTable();
        Set<Frequency> newFrequencies = new HashSet<>(frequencies);

        newNotes.remove(note);
        Set<Note> notes = newFrequencyNoteTable.get(frequency);
        try {
            notes.remove(note);

            if (notes.isEmpty()) {
                newFrequencies.remove(frequency);
                newFrequencyNoteTable.remove(frequency);
            }
        }
        catch(NullPointerException e){
            
        }

        return new SimpleFrequencyState(newNotes, newFrequencies, newFrequencyNoteTable);
    }

    public SimpleFrequencyState removeNotes(Set<Note> notes) {
        if (notes.isEmpty()) {
            return this;
        }

        Set<Note> newNotes = new HashSet<>(notes);
        newNotes.removeAll(notes);

        Set<Frequency> newFrequencies = frequencies;
        Map<Frequency, Set<Note>> newFrequencyNoteTable = copyFrequencyNoteTable();

        Set<Frequency> touchedFrequencies = new HashSet<>();

        for (Note note : notes) {
            Frequency frequency = note.getFrequency();

            newFrequencyNoteTable.get(frequency).remove(note);
            touchedFrequencies.add(frequency);
        }

        if (!touchedFrequencies.isEmpty()) {
            newFrequencies = new HashSet<>(frequencies);

            for (Frequency frequency : touchedFrequencies) {
                if (newFrequencyNoteTable.get(frequency).isEmpty()) {
                    newFrequencies.remove(frequency);
                    newFrequencyNoteTable.remove(frequency);
                }
            }
        }

        return new SimpleFrequencyState(newNotes, newFrequencies, newFrequencyNoteTable);
    }

    @Override
    public FrequencyState update(long sampleCount) {
        return this;
    }

    public SimpleFrequencyState addNote(Note note) {
        Frequency frequency = note.getFrequency();

        Set<Note> newNotes = new HashSet<>(notes);
        Map<Frequency, Set<Note>> newFrequencyNoteTable = copyFrequencyNoteTable();
        Set<Frequency> newFrequencies = frequencies;

        newNotes.add(note);
        Set<Note> noteSet;

        if (!newFrequencyNoteTable.containsKey(frequency)) {
            newFrequencies = new HashSet<>(frequencies);
            newFrequencies.add(frequency);

            noteSet = new HashSet<>();
            newFrequencyNoteTable.put(frequency, noteSet);
        } else {
            noteSet = newFrequencyNoteTable.get(frequency);
        }
        noteSet.add(note);

        return new SimpleFrequencyState(newNotes, newFrequencies, newFrequencyNoteTable);
    }

    Map<Frequency, Set<Note>> copyFrequencyNoteTable() {
        //todo if we want the list to be immutable, we could calculate the death time of each note, and create a
        //todo new version of the object at each death time.
        //todo by using zipper like structures, we can only update a particular note list.
        //todo we can link together all these death time based frequencytables, and request the current one from the
        // todo noteManager, like an iterator. We might even turn it into an iterator.
        HashMap<Frequency, Set<Note>> frequencyNoteTableCopy = new HashMap<>();

        Iterator<Map.Entry<Frequency, Set<Note>>> iterator = frequencyNoteTable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Frequency, Set<Note>> entry = iterator.next();
            frequencyNoteTableCopy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        return frequencyNoteTableCopy;
    }

    public Set<Frequency> getFrequencies(){
        return new HashSet<>(frequencies);
    }

}