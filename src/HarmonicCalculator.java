import java.util.HashMap;
import java.util.PriorityQueue;

public class HarmonicCalculator {

    private final NoteEnvironment noteEnvironment;

    PriorityQueue<ComparableIterator> harmonicHierarchy;
    private HashMap<ComparableIterator, Note> lookupTable;
    private HashMap<Note, Double> volumeTable;

    public HarmonicCalculator(NoteEnvironment noteEnvironment){
        this.noteEnvironment = noteEnvironment;
        lookupTable = new HashMap<>();
        volumeTable = new HashMap<>();
        harmonicHierarchy = new PriorityQueue<>();
    }



    public Harmonic getNextHarmonic() {
        if (harmonicHierarchy.isEmpty()) {
            return null;
        }

        ComparableIterator highestValueComparableIterator = harmonicHierarchy.poll();
        Note highestValueNote = lookupTable.get(highestValueComparableIterator);
        Harmonic highestValueHarmonic = new Harmonic(highestValueNote, highestValueComparableIterator.next());
        highestValueHarmonic.noteVolume = volumeTable.get(highestValueNote);
        harmonicHierarchy.add(highestValueComparableIterator);

        return highestValueHarmonic;
    }

    public void reset(long currentSampleCount) {
        lookupTable.clear();
        volumeTable.clear();
        harmonicHierarchy.clear();
        for(Note note : noteEnvironment.getLiveNotes()) {
            double volume = noteEnvironment.getVolume(note, currentSampleCount);
            ComparableIterator comparableIterator = new ComparableIterator(volume);
            lookupTable.put(comparableIterator, note);
            volumeTable.put(note, volume);
            harmonicHierarchy.add(comparableIterator);
        }
    }

}
