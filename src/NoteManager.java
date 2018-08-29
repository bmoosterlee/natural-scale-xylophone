import javafx.util.Pair;

import java.util.*;

public class NoteManager {
    private final NoteEnvironment noteEnvironment;
    private NoteSnapshot noteSnapshot;
    private FrequencySnapshot frequencySnapshot;

    public NoteManager(NoteEnvironment noteEnvironment) {
        this.noteEnvironment = noteEnvironment;

        noteSnapshot = new NoteSnapshot();
        frequencySnapshot =  new FrequencySnapshot();
    }

    HashMap<Note, Double> getVolumeTable(long currentSampleCount, Set<Note> liveNotes, HashMap<Note, Envelope> envelopes) {
        HashMap<Note, Double> volumeTable = new HashMap<>();
        for(Note note : liveNotes) {
            double volume;
            try {
                volume = envelopes.get(note).getVolume(currentSampleCount);
            }
            catch(NullPointerException e){
                volume = 0.0;
            }
            volumeTable.put(note, volume);
        }
        return volumeTable;
    }

    void removeInaudibleNotes(Set<Note> inaudibleNotes) {
        NoteSnapshot newNoteSnapshot = new NoteSnapshot(noteSnapshot);
        synchronized (noteSnapshot) {
            newNoteSnapshot.liveNotes.removeAll(inaudibleNotes);
            Iterator<Note> iterator = inaudibleNotes.iterator();
            while (iterator.hasNext()) {
                newNoteSnapshot.envelopes.remove(iterator.next());
            }
            noteSnapshot = newNoteSnapshot;
        }

        synchronized (frequencySnapshot) {
            frequencySnapshot = frequencySnapshot.removeInaudibleNotes(inaudibleNotes);
        }
    }

    public void addNote(double frequency) {
        Pair<Note, Envelope> pair = noteEnvironment.createNote();
        Note note = pair.getKey();
        Envelope envelope = pair.getValue();

        NoteSnapshot newNoteSnapshot = new NoteSnapshot(noteSnapshot);
        synchronized (noteSnapshot) {
            newNoteSnapshot.liveNotes.add(note);
            newNoteSnapshot.envelopes.put(note, envelope);
            noteSnapshot = newNoteSnapshot;
        }

        synchronized (frequencySnapshot) {
            frequencySnapshot = frequencySnapshot.addNote(note, frequency);
        }
    }

    public NoteFrequencySnapshot getSnapshot() {
        synchronized (noteSnapshot) {
            synchronized (frequencySnapshot) {
                return new NoteFrequencySnapshot(new NoteSnapshot(noteSnapshot), new FrequencySnapshot(frequencySnapshot));
            }
        }
    }

    Map<Double, Double> getFrequencyVolumeTable(Map<Double, Set<Note>> frequencyNoteTable, Map<Note, Double> volumeTable) {
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
}