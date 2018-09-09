package notes;

import java.util.*;

public class FrequencySnapshot {
    public final Set<Frequency> liveFrequencies;
    final HashMap<Frequency, Double> frequencyAngleComponents;
    public final Map<Frequency, Set<Note>> frequencyNoteTable;

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

    FrequencySnapshot removeInaudibleNotes(Set<Note> inaudibleNotes) {
        FrequencySnapshot frequencySnapshot = new FrequencySnapshot(this);
        Set<Frequency> touchedFrequencies = new HashSet<>();

        for (Note note : inaudibleNotes) {
            Frequency frequency = note.frequency;

            Set<Note> noteSet = frequencySnapshot.frequencyNoteTable.get(frequency);
            noteSet.remove(note);
            touchedFrequencies.add(frequency);
        }

        for(Frequency frequency : touchedFrequencies){
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

        Frequency frequency = note.frequency;
        Set<Note> noteSet;

        frequencySnapshot.liveFrequencies.add(frequency);

        if (!frequencySnapshot.frequencyNoteTable.containsKey(note)) {
            frequencySnapshot.frequencyAngleComponents.put(frequency, frequency.getValue() * 2.0 * Math.PI);
            noteSet = new HashSet<>();
            frequencySnapshot.frequencyNoteTable.put(frequency, noteSet);
        } else {
            noteSet = frequencySnapshot.frequencyNoteTable.get(frequency);
        }
        noteSet.add(note);

        return frequencySnapshot;
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

    public Set<Frequency> getLiveFrequencies(){
        return new HashSet<>(liveFrequencies);
    }
}