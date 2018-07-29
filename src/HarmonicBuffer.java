import javafx.util.Pair;

import java.util.*;

public class HarmonicBuffer {
    HashSet<Harmonic> previousHighHarmonics;
    HashMap<Harmonic, Note> previousHighHarmonicNotes;
    PriorityQueue<Harmonic> previousHighHarmonicsVolume;
    HashMap<Harmonic, Double> volumeTable;
    HashMap<Note, HashSet<Harmonic>> notesForPreviousHighHarmonics;

    public HarmonicBuffer() {
        previousHighHarmonics = new HashSet<>();
        previousHighHarmonicNotes = new HashMap<>();
        volumeTable = new HashMap<>();

        notesForPreviousHighHarmonics = new HashMap<>();
    }

    void addHarmonicToHarmonicBuffer(double newHarmonicVolume, Note highestValueNote, Harmonic highestValueHarmonic) {
        synchronized (this) {
            previousHighHarmonics.add(highestValueHarmonic);
            previousHighHarmonicNotes.put(highestValueHarmonic, highestValueNote);
            volumeTable.put(highestValueHarmonic, newHarmonicVolume);
            previousHighHarmonicsVolume.add(highestValueHarmonic);

            if (!notesForPreviousHighHarmonics.containsKey(highestValueNote)) {
                HashSet<Harmonic> harmonicsForThisNote = new HashSet<>();
                harmonicsForThisNote.add(highestValueHarmonic);
                notesForPreviousHighHarmonics.put(highestValueNote, harmonicsForThisNote);
            } else {
                notesForPreviousHighHarmonics.get(highestValueNote).add(highestValueHarmonic);
            }
        }
    }

    double getHighestPriorityHarmonicVolume(int maxHarmonics) {
        try {
            return volumeTable.get(previousHighHarmonicsVolume.toArray(new Harmonic[previousHighHarmonicsVolume.size()])[maxHarmonics]);
        }
        catch(ArrayIndexOutOfBoundsException e){
            return 0;
        }
    }

    PriorityQueue<Harmonic> calculateHarmonicVolumes(HashMap<Note, Double> noteVolumeTable) {
//            calculate harmonic volumes using note volumes
//            store volumes in a pair with the harmonic in a priorityqueue

        PriorityQueue<Harmonic> newHarmonicPriorityQueue = new PriorityQueue<>((o1, o2) -> -volumeTable.get(o1).compareTo(volumeTable.get(o2)));
        synchronized (notesForPreviousHighHarmonics) {
            for(Map.Entry<Note,Double> pair : new HashSet<>(noteVolumeTable.entrySet())){
                HashSet<Harmonic> harmonics = notesForPreviousHighHarmonics.get(pair.getKey());
                if(harmonics!=null) {
                    for (Harmonic harmonic : harmonics) {
                        newHarmonicPriorityQueue.add(harmonic);
                    }
                }
            }
        }
        return newHarmonicPriorityQueue;
    }

    void rebuildHarmonicHierarchy(HashMap<Note, Double> volumeTable) {
        previousHighHarmonicsVolume = calculateHarmonicVolumes(volumeTable);
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

    public LinkedList<Pair<Harmonic, Double>> getHarmonicHierarchy() {
        LinkedList<Harmonic> harmonicHierarchy = new LinkedList<>(previousHighHarmonicsVolume);
        LinkedList<Pair<Harmonic, Double>> harmonicsWithVolumes = new LinkedList<>();
        for(Harmonic harmonic : harmonicHierarchy){
            harmonicsWithVolumes.addLast(new Pair(harmonic, volumeTable.get(harmonic)));
        }
        return harmonicsWithVolumes;
    }
}