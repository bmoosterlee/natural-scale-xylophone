import javafx.util.Pair;

import java.util.*;

public class HarmonicCalculator {
    
    private final HarmonicBuffer harmonicBuffer = new HarmonicBuffer();

    private HashMap<Note, MemoableIterator> iteratorTable;

    public HarmonicCalculator(NoteEnvironment noteEnvironment){
        iteratorTable = new HashMap<>();

        noteEnvironment.addNoteObservable.addObserver((Observer<Note>) event -> addNote(event));
    }

    private void addNewHarmonicsToBuffer(HashMap<Note, Double> volumeTable, PriorityQueue<Note> iteratorHierarchy, HashMap<Harmonic, Double> harmonicVolumeTable, int maxHarmonics) {
        while(getNewHarmonicVolume(volumeTable, iteratorHierarchy) > harmonicBuffer.getHighestPriorityHarmonicVolume(maxHarmonics, harmonicVolumeTable)) {
            addNewHarmonic(volumeTable, iteratorHierarchy, harmonicVolumeTable);
        }
    }

    private double getNewHarmonicVolume(HashMap<Note, Double> volumeTable, PriorityQueue<Note> iteratorHierarchy) {
        try {
            Note highestValueNote = iteratorHierarchy.peek();
            Fraction nextHarmonicAsFraction = iteratorTable.get(highestValueNote).peek();

            Harmonic highestValueHarmonic = new Harmonic(highestValueNote, nextHarmonicAsFraction, volumeTable.get(highestValueNote));
            return highestValueHarmonic.getVolume(volumeTable.get(highestValueNote));
        }
        catch(NullPointerException e){
            return 0.;
        }
    }

    private void addNewHarmonic(HashMap<Note, Double> volumeTable, PriorityQueue<Note> iteratorHierarchy, HashMap<Harmonic, Double> harmonicVolumeTable) {
        try {
            Note highestValueNote = iteratorHierarchy.poll();
            Fraction nextHarmonicAsFraction = iteratorTable.get(highestValueNote).next();
            iteratorHierarchy.add(highestValueNote);

            Harmonic highestValueHarmonic = new Harmonic(highestValueNote, nextHarmonicAsFraction, volumeTable.get(highestValueNote));
            double newHarmonicVolume = highestValueHarmonic.getVolume(volumeTable.get(highestValueNote));

            harmonicBuffer.addHarmonicToHarmonicBuffer(newHarmonicVolume, highestValueNote, highestValueHarmonic, harmonicVolumeTable);
        }
        catch(NullPointerException e){
        }
    }

    public LinkedList<Pair<Harmonic, Double>> getCurrentHarmonicHierarchy(long currentSampleCount, HashSet<Note> liveNotes, int maxHarmonics) {
        HashMap<Note, Double> volumeTable = getVolumes(currentSampleCount, liveNotes);

        PriorityQueue<Note> iteratorHierarchy = rebuildIteratorHierarchy(volumeTable, liveNotes);

        HashMap<Harmonic, Double> harmonicVolumeTable = harmonicBuffer.rebuildHarmonicHierarchy(volumeTable);

        addNewHarmonicsToBuffer(volumeTable, iteratorHierarchy, harmonicVolumeTable, maxHarmonics);

        return harmonicBuffer.getHarmonicHierarchy(harmonicVolumeTable);
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

    private void addNote(Note note) {
        MemoableIterator MemoableIterator = new MemoableIterator();
        iteratorTable.put(note, MemoableIterator);
    }

}
