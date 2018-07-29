import javafx.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;

public class HarmonicBuffer {
    HashSet<Harmonic> previousHighHarmonics;
    HashMap<Harmonic, Note> previousHighHarmonicNotes;
    PriorityQueue<Pair<Harmonic, Double>> previousHighHarmonicsVolume;
    HashMap<Note, HashSet<Harmonic>> notesForPreviousHighHarmonics;

    public HarmonicBuffer() {
        previousHighHarmonics = new HashSet<>();
        previousHighHarmonicNotes = new HashMap<>();
        previousHighHarmonicsVolume = new PriorityQueue<>((o1, o2) -> -o1.getValue().compareTo(o2.getValue()));
        notesForPreviousHighHarmonics = new HashMap<>();
    }

    void addHarmonicToHarmonicBuffer(double newHarmonicVolume, Note highestValueNote, Harmonic highestValueHarmonic) {
        synchronized (this) {
            previousHighHarmonics.add(highestValueHarmonic);
            previousHighHarmonicNotes.put(highestValueHarmonic, highestValueNote);
            previousHighHarmonicsVolume.add(new Pair(highestValueHarmonic, newHarmonicVolume));

            if (!notesForPreviousHighHarmonics.containsKey(highestValueNote)) {
                HashSet<Harmonic> harmonicsForThisNote = new HashSet<>();
                harmonicsForThisNote.add(highestValueHarmonic);
                notesForPreviousHighHarmonics.put(highestValueNote, harmonicsForThisNote);
            } else {
                notesForPreviousHighHarmonics.get(highestValueNote).add(highestValueHarmonic);
            }
        }
    }

    Double getHighestPriorityHarmonicVolume() {
        Double highestPriorityHarmonicVolume;
        Pair<Harmonic, Double> peek = previousHighHarmonicsVolume.peek();
        if (peek == null) {
            highestPriorityHarmonicVolume = 0.;
        } else {
            highestPriorityHarmonicVolume = peek.getValue();
        }
        return highestPriorityHarmonicVolume;
    }

    Pair<Harmonic, Double> getHighestValueHarmonic() {
        return previousHighHarmonicsVolume.poll();
    }

    PriorityQueue<Pair<Harmonic, Double>> calculateHarmonicVolumes(HashMap<Note, Double> volumeTable) {
//            calculate harmonic volumes using note volumes
//            store volumes in a pair with the harmonic in a priorityqueue

        PriorityQueue<Pair<Harmonic, Double>> newHarmonicPriorityQueue = new PriorityQueue<>((o1, o2) -> -o1.getValue().compareTo(o2.getValue()));
        synchronized (notesForPreviousHighHarmonics) {
            for(Map.Entry<Note,Double> pair : new HashSet<>(volumeTable.entrySet())){
                Note note = pair.getKey();
                Double volume = pair.getValue();
                HashSet<Harmonic> harmonics = notesForPreviousHighHarmonics.get(note);
                if(harmonics!=null) {
                    for (Harmonic harmonic : harmonics) {
                        newHarmonicPriorityQueue.add(new Pair(harmonic,
                                harmonic.getVolume(volume)));
                    }
                }
            }
        }
        return newHarmonicPriorityQueue;
    }

    void rebuildHarmonicHierarchy(HashMap<Note, Double> volumeTable) {
        synchronized(previousHighHarmonicsVolume) {
            previousHighHarmonicsVolume = calculateHarmonicVolumes(volumeTable);
        }
    }

    void removeNote(Note note) {
        synchronized(this) {
            //            remove dead notes here based on volume
            HashSet<Harmonic> harmonics = notesForPreviousHighHarmonics.get(note);
            if(harmonics!=null) {
                for (Harmonic harmonic : harmonics) {
                    previousHighHarmonics.remove(harmonic);
                    previousHighHarmonicNotes.remove(harmonic);
                }
            }

            notesForPreviousHighHarmonics.remove(note);
        }
    }

    void addNote(Note note) {
        synchronized(notesForPreviousHighHarmonics) {
            notesForPreviousHighHarmonics.put(note, new HashSet<>());
        }
    }
}