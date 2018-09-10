package notes.state;

import notes.Frequency;
import notes.Note;

import java.util.*;

public class FrequencyState {
    public final Set<Frequency> frequencies;
    public final Map<Frequency, Set<Note>> frequencyNoteTable;
    public final WaveState waveState;

    public FrequencyState() {
        frequencies = new HashSet<>();
        frequencyNoteTable = new HashMap<>();
        waveState = new WaveState();
    }

    public FrequencyState(Set<Frequency> frequencies, Map<Frequency, Set<Note>> frequencyNoteTable, WaveState waveState) {
        this.frequencies = frequencies;
        this.frequencyNoteTable = frequencyNoteTable;
        this.waveState = waveState;
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
        if(inaudibleNotes.isEmpty()){
            return this;
        }

        Set<Frequency> newFrequencies = frequencies;
        Map<Frequency, Set<Note>> newFrequencyNoteTable = copyFrequencyNoteTable();
        WaveState newWaveState = waveState;

        Set<Frequency> touchedFrequencies = new HashSet<>();

        for (Note note : inaudibleNotes) {
            Frequency frequency = note.getFrequency();

            newFrequencyNoteTable.get(frequency).remove(note);
            touchedFrequencies.add(frequency);
        }

        if(!touchedFrequencies.isEmpty()) {
            newFrequencies = new HashSet<>(frequencies);

            for (Frequency frequency : touchedFrequencies) {
                if (newFrequencyNoteTable.get(frequency).isEmpty()) {
                    newFrequencies.remove(frequency);
                    newFrequencyNoteTable.remove(frequency);
                    newWaveState = newWaveState.remove(frequency);
                }
            }
        }

        return new FrequencyState(newFrequencies, newFrequencyNoteTable, newWaveState);
    }

    FrequencyState addNote(Note note) {
        Frequency frequency = note.getFrequency();

        Map<Frequency, Set<Note>> newFrequencyNoteTable = copyFrequencyNoteTable();
        Set<Frequency> newFrequencies = frequencies;
        WaveState newWaveState = waveState;

        Set<Note> noteSet;

        if (!newFrequencyNoteTable.containsKey(frequency)) {
            newFrequencies = new HashSet<>(frequencies);
            newFrequencies.add(frequency);
            newWaveState = newWaveState.add(frequency);

            noteSet = new HashSet<>();
            newFrequencyNoteTable.put(frequency, noteSet);
        } else {
            noteSet = newFrequencyNoteTable.get(frequency);
        }
        noteSet.add(note);

        return new FrequencyState(newFrequencies, newFrequencyNoteTable, newWaveState);
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

    public Map<Frequency, Double> getFrequencyVolumeTable(long sampleCount, NoteState noteState) {
        return getFrequencyVolumeTable(noteState.getVolumeTable(sampleCount));
    }

}