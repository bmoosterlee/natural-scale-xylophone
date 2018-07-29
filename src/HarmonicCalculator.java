import javafx.util.Pair;

import java.util.*;

public class HarmonicCalculator {
    
    private final HarmonicBuffer harmonicBuffer = new HarmonicBuffer();

    private HashMap<Note, MemoableIterator> iteratorTable;

    public HarmonicCalculator(NoteEnvironment noteEnvironment){
        iteratorTable = new HashMap<>();

        noteEnvironment.addNoteObservable.addObserver((Observer<Note>) event -> addNote(event));
        noteEnvironment.removeNoteObservable.addObserver((Observer<Note>) event -> removeNote(event));
    }

    private void addNewHarmonicsToBuffer(HashMap<Note, Double> volumeTable, PriorityQueue<Note> iteratorHierarchy) {
        double newHarmonicVolume;
        double highestPriorityHarmonicVolume;
        do {
            newHarmonicVolume = getNewHarmonicVolume(volumeTable, iteratorHierarchy);
            highestPriorityHarmonicVolume = harmonicBuffer.getHighestPriorityHarmonicVolume();
        } while(newHarmonicVolume > highestPriorityHarmonicVolume);
    }

    private double getNewHarmonicVolume(HashMap<Note, Double> volumeTable, PriorityQueue<Note> iteratorHierarchy) {
        try {
            Note highestValueNote = iteratorHierarchy.poll();

            MemoableIterator highestValueMemoableIterator = iteratorTable.get(highestValueNote);
            Fraction nextHarmonicAsFraction = highestValueMemoableIterator.next();

            iteratorHierarchy.add(highestValueNote);

            Harmonic highestValueHarmonic = new Harmonic(highestValueNote, nextHarmonicAsFraction, volumeTable.get(highestValueNote));

//            compare this harmonic to the highest value harmonic from the previousHighHarmonicsVolume priorityqueue
            double newHarmonicVolume = highestValueHarmonic.getVolume(volumeTable.get(highestValueNote));

            harmonicBuffer.addHarmonicToHarmonicBuffer(newHarmonicVolume, highestValueNote, highestValueHarmonic);

            return newHarmonicVolume;
        }
        catch(NullPointerException e){
            return 0.;
        }
    }

    public LinkedList<Pair<Harmonic, Double>> getCurrentHarmonicHierarchy(long currentSampleCount, HashSet<Note> liveNotes) {
        HashMap<Note, Double> volumeTable = getVolumes(currentSampleCount, liveNotes);

        PriorityQueue<Note> iteratorHierarchy = rebuildIteratorHierarchy(volumeTable, liveNotes);

        harmonicBuffer.rebuildHarmonicHierarchy(volumeTable);

        addNewHarmonicsToBuffer(volumeTable, iteratorHierarchy);

        return harmonicBuffer.getHarmonicHierarchy();
    }

    private PriorityQueue<Note> rebuildIteratorHierarchy(HashMap<Note, Double> volumeTable, HashSet<Note> liveNotes) {
        PriorityQueue<Note> iteratorHierarchy = new PriorityQueue<>((o1, o2) -> -Double.compare(
                Harmonic.getHarmonicValue(volumeTable.get(o1), iteratorTable.get(o1).currentHarmonicAsFraction),
                Harmonic.getHarmonicValue(volumeTable.get(o2), iteratorTable.get(o2).currentHarmonicAsFraction))
        );
        iteratorHierarchy.addAll(liveNotes);

        return iteratorHierarchy;
    }

    private HashMap<Note, Double> getVolumes(long currentSampleCount, HashSet<Note> liveNotes) {
        HashMap<Note, Double> newVolumeTable = new HashMap<>();
        for(Note note : liveNotes) {
            newVolumeTable.put(note, note.getVolume(currentSampleCount));
        }
        return newVolumeTable;
    }

    private void removeNote(Note note) {
        harmonicBuffer.removeNote(note);
    }

    private void addNote(Note note) {
        MemoableIterator MemoableIterator = new MemoableIterator();
        iteratorTable.put(note, MemoableIterator);
//          calculate note volumes and pair them with their note
        harmonicBuffer.addNote(note);
    }

}
