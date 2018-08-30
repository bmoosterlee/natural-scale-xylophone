package notes;

import java.util.*;

public class FrequencySnapshot {
    final long sampleCount;
    public final Set<Double> liveFrequencies;
    final HashMap<Double, Double> frequencyAngleComponents;
    public final Map<Double, Set<Note>> frequencyNoteTable;
    final Map<Note, Double> noteFrequencyTable;

    public FrequencySnapshot(long sampleCount) {
        this.sampleCount = sampleCount;
        liveFrequencies = new HashSet<>();
        frequencyAngleComponents = new HashMap<>();
        frequencyNoteTable = new HashMap<>();
        noteFrequencyTable = new HashMap<>();
    }

    public FrequencySnapshot(long sampleCount, FrequencySnapshot frequencySnapshot) {
        this.sampleCount = sampleCount;
        liveFrequencies = frequencySnapshot.getLiveFrequencies();
        frequencyAngleComponents = frequencySnapshot.getFrequencyAngleComponents();
        if(liveFrequencies.size()!=frequencyAngleComponents.size()){
            liveFrequencies.size();
        }
        frequencyNoteTable = frequencySnapshot.getFrequencyNoteTable();
        noteFrequencyTable = frequencySnapshot.getNoteFrequencyTable();
    }

    public Map<Double, Double> getFrequencyVolumeTable(Map<Note, Double> volumeTable) {
        Map<Double, Double> frequencyVolumes = new HashMap<>();

        Iterator<Map.Entry<Double, Set<Note>>> iterator = frequencyNoteTable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Double, Set<Note>> entry = iterator.next();
            Double frequency = entry.getKey();
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

    FrequencySnapshot removeInaudibleNotes(Set<Note> inaudibleNotes) {
        FrequencySnapshot frequencySnapshot = new FrequencySnapshot(sampleCount, this);

        for (Note note : inaudibleNotes) {
            Double frequency = frequencySnapshot.noteFrequencyTable.get(note);

            frequencySnapshot.noteFrequencyTable.remove(note);
            Set<Note> noteSet = frequencySnapshot.frequencyNoteTable.get(frequency);
            noteSet.remove(note);
            if (noteSet.isEmpty()) {
                frequencySnapshot.liveFrequencies.remove(frequency);
                frequencySnapshot.frequencyNoteTable.remove(frequency);
                frequencySnapshot.frequencyAngleComponents.remove(frequency);
            }

        }

        return frequencySnapshot;
    }

    FrequencySnapshot addNote(Note note, Double frequency) {
        FrequencySnapshot frequencySnapshot = new FrequencySnapshot(sampleCount, this);

        Set<Note> noteSet;

        frequencySnapshot.liveFrequencies.add(frequency);

        if (!frequencySnapshot.frequencyNoteTable.containsKey(note)) {
            frequencySnapshot.frequencyAngleComponents.put(frequency, frequency * 2.0 * Math.PI);
            noteSet = new HashSet<>();
            frequencySnapshot.frequencyNoteTable.put(frequency, noteSet);
        } else {
            noteSet = frequencySnapshot.frequencyNoteTable.get(frequency);
        }
        noteSet.add(note);
        frequencySnapshot.noteFrequencyTable.put(note, frequency);

        return frequencySnapshot;
    }

    Map<Double, Set<Note>> getFrequencyNoteTable() {
        //todo if we want the list to be immutable, we could calculate the death time of each note, and create a
        //todo new version of the object at each death time.
        //todo by using zipper like structures, we can only update a particular note list.
        //todo we can link together all these death time based frequencytables, and request the current one from the
        // todo noteManager, like an iterator. We might even turn it into an iterator.
        HashMap<Double, Set<Note>> frequencyNoteTableCopy = new HashMap<>();

        Iterator<Map.Entry<Double, Set<Note>>> iterator = frequencyNoteTable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Double, Set<Note>> entry = iterator.next();
            frequencyNoteTableCopy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        return frequencyNoteTableCopy;
    }

    public HashMap<Double, Double> getFrequencyAngleComponents() {
        return new HashMap<>(frequencyAngleComponents);
    }

    public Map<Note,Double> getNoteFrequencyTable() {
        return new HashMap<>(noteFrequencyTable);
    }

    public Set<Double> getLiveFrequencies(){
        return new HashSet<>(liveFrequencies);
    }
}