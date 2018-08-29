import java.util.*;

public class FrequencySnapshot {
    final HashMap<Double, Double> frequencyAngleComponents;
    final Map<Double, Set<Note>> frequencyNoteTable;
    final Map<Note, Double> noteFrequencyTable;

    public FrequencySnapshot() {
        frequencyAngleComponents = new HashMap<>();
        frequencyNoteTable = new HashMap<>();
        noteFrequencyTable = new HashMap<>();
    }

    public FrequencySnapshot(FrequencySnapshot frequencySnapshot) {
        frequencyAngleComponents = frequencySnapshot.getFrequencyAngleComponents();
        frequencyNoteTable = frequencySnapshot.getFrequencyNoteTable();
        noteFrequencyTable = frequencySnapshot.getNoteFrequencyTable();
    }

    FrequencySnapshot removeInaudibleNotes(Set<Note> inaudibleNotes) {
        FrequencySnapshot frequencySnapshot = new FrequencySnapshot(this);

        for (Note note : inaudibleNotes) {
            Double frequency = frequencySnapshot.noteFrequencyTable.get(note);

            frequencySnapshot.noteFrequencyTable.remove(note);
            Set<Note> noteSet = frequencySnapshot.frequencyNoteTable.get(frequency);
            try {
                noteSet.remove(note);
                if (noteSet.isEmpty()) {
                    frequencySnapshot.frequencyNoteTable.remove(frequency);
                    frequencySnapshot.frequencyAngleComponents.remove(frequency);
                }
            }
            catch(NullPointerException e){

            }

        }

        return frequencySnapshot;
    }

    FrequencySnapshot addNote(Note note) {
        FrequencySnapshot frequencySnapshot = new FrequencySnapshot(this);

        double frequency = note.getFrequency();
        Set<Note> noteSet;

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
}