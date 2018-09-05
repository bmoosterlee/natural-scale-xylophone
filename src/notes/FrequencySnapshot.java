package notes;

import java.util.*;

public class FrequencySnapshot {
    public final Set<Double> liveFrequencies;
    final HashMap<Double, Double> frequencyAngleComponents;
    public final Map<Double, Set<Note>> frequencyNoteTable;

    public FrequencySnapshot() {
        liveFrequencies = new HashSet<>();
        frequencyAngleComponents = new HashMap<>();
        frequencyNoteTable = new HashMap<>();
    }

    public FrequencySnapshot(FrequencySnapshot frequencySnapshot) {
        liveFrequencies = frequencySnapshot.getLiveFrequencies();
        frequencyAngleComponents = frequencySnapshot.getFrequencyAngleComponents();
        if(liveFrequencies.size()!=frequencyAngleComponents.size()){
            liveFrequencies.size();
        }
        frequencyNoteTable = frequencySnapshot.getFrequencyNoteTable();
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
        FrequencySnapshot frequencySnapshot = new FrequencySnapshot(this);
        Set<Double> touchedFrequencies = new HashSet<>();

        for (Note note : inaudibleNotes) {
            Double frequency = note.frequency;

            Set<Note> noteSet = frequencySnapshot.frequencyNoteTable.get(frequency);
            noteSet.remove(note);
            touchedFrequencies.add(frequency);
        }

        for(Double frequency : touchedFrequencies){
            if (frequencySnapshot.frequencyNoteTable.get(frequency).isEmpty()) {
                frequencySnapshot.liveFrequencies.remove(frequency);
                frequencySnapshot.frequencyNoteTable.remove(frequency);
                frequencySnapshot.frequencyAngleComponents.remove(frequency);
            }

        }

        return frequencySnapshot;
    }

    FrequencySnapshot addNote(Note note) {
        FrequencySnapshot frequencySnapshot = new FrequencySnapshot(this);

        Double frequency = note.frequency;
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

    public Set<Double> getLiveFrequencies(){
        return new HashSet<>(liveFrequencies);
    }
}