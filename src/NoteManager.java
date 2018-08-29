import java.util.*;

public class NoteManager {
    private final NoteEnvironment noteEnvironment;
    private FrequencySnapshot frequencySnapshot;
    private HashSet<Note> liveNotes;

    public NoteManager(NoteEnvironment noteEnvironment) {
        this.noteEnvironment = noteEnvironment;

        liveNotes = new HashSet();
        frequencySnapshot =  new FrequencySnapshot();
    }

    static HashMap<Note, Double> getVolumeTable(long currentSampleCount, Set<Note> liveNotes) {
        HashMap<Note, Double> newVolumeTable = new HashMap<>();
        for(Note note : liveNotes) {
            newVolumeTable.put(note, note.getVolume(currentSampleCount));
        }
        return newVolumeTable;
    }

    void removeInaudibleNotes(Set<Note> inaudibleNotes) {
        synchronized (liveNotes) {
            liveNotes.removeAll(inaudibleNotes);
        }

        synchronized (frequencySnapshot) {
            frequencySnapshot = frequencySnapshot.removeInaudibleNotes(inaudibleNotes);
        }
    }

    public HashSet<Note> getLiveNotes() {
        synchronized (liveNotes) {
            return new HashSet<>(liveNotes);
        }
    }

    public void addNote(double frequency) {
        Note note = noteEnvironment.createNote(frequency);
        synchronized (liveNotes) {
            liveNotes.add(note);
        }

        synchronized (frequencySnapshot) {
            frequencySnapshot = frequencySnapshot.addNote(note);
        }
    }

    public NoteSnapshot getSnapshot() {
            return new NoteSnapshot(getLiveNotes(), new FrequencySnapshot(frequencySnapshot));
    }
}