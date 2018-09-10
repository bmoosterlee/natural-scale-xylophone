package notes;

import java.util.*;

public class FrequencyState {
    public final Set<Frequency> frequencies;
    final HashMap<Frequency, Double> frequencyAngleComponents;
    public final Map<Frequency, Set<Note>> frequencyNoteTable;

    public FrequencyState() {
        frequencies = new HashSet<>();
        frequencyAngleComponents = new HashMap<>();
        frequencyNoteTable = new HashMap<>();
    }

    public FrequencyState(FrequencyState frequencies) {
        this.frequencies = frequencies.getFrequencies();
        frequencyAngleComponents = frequencies.getFrequencyAngleComponents();
        frequencyNoteTable = frequencies.getFrequencyNoteTable();
    }

    public Map<Frequency, Double> getFrequencyVolumeTable(Map<Note, Double> volumeTable) {
        Map<Frequency, Double> frequencyVolumes = new HashMap<>();

        Iterator<Map.Entry<Frequency, Set<Note>>> iterator = frequencyNoteTable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Frequency, Set<Note>> entry = iterator.next();
            Frequency frequency = entry.getKey();
            Double volume = 0.;
            Iterator<Note> noteIterator = entry.getValue().iterator();
            while (noteIterator.hasNext()) {
                Note note = noteIterator.next();
                try {
                    volume += volumeTable.get(note);
                } catch (NullPointerException e) {
                    continue;
                }
            }
            frequencyVolumes.put(frequency, volume);
        }
        return frequencyVolumes;
    }

    FrequencyState removeInaudibleNotes(Set<Note> inaudibleNotes) {
        FrequencyState frequencyState = new FrequencyState(this);
        Set<Frequency> touchedFrequencies = new HashSet<>();

        for (Note note : inaudibleNotes) {
            Frequency frequency = note.frequency;

            Set<Note> noteSet = frequencyState.frequencyNoteTable.get(frequency);
            noteSet.remove(note);
            touchedFrequencies.add(frequency);
        }

        for(Frequency frequency : touchedFrequencies){
            if (frequencyState.frequencyNoteTable.get(frequency).isEmpty()) {
                frequencyState.frequencies.remove(frequency);
                frequencyState.frequencyNoteTable.remove(frequency);
                frequencyState.frequencyAngleComponents.remove(frequency);
            }

        }

        return frequencyState;
    }

    FrequencyState addNote(Note note) {
        FrequencyState frequencyState = new FrequencyState(this);

        Frequency frequency = note.frequency;
        Set<Note> noteSet;

        frequencyState.frequencies.add(frequency);

        if (!frequencyState.frequencyNoteTable.containsKey(note)) {
            frequencyState.frequencyAngleComponents.put(frequency, frequency.getValue() * 2.0 * Math.PI);
            noteSet = new HashSet<>();
            frequencyState.frequencyNoteTable.put(frequency, noteSet);
        } else {
            noteSet = frequencyState.frequencyNoteTable.get(frequency);
        }
        noteSet.add(note);

        return frequencyState;
    }

    Map<Frequency, Set<Note>> getFrequencyNoteTable() {
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

    public HashMap<Frequency, Double> getFrequencyAngleComponents() {
        return new HashMap<>(frequencyAngleComponents);
    }

    public Set<Frequency> getFrequencies(){
        return new HashSet<>(frequencies);
    }

    public Map<Frequency, Double> getFrequencyVolumeTable(long sampleCount, NoteState noteState) {
        return getFrequencyVolumeTable(noteState.getVolumeTable(sampleCount));
    }

}