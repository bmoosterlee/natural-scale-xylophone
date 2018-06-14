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
        ComparableIterator highestValueComparableIterator = harmonicHierarchy.poll();
        try {
            Fraction nextHarmonicAsFraction = highestValueComparableIterator.next();
            harmonicHierarchy.add(highestValueComparableIterator);

            Note highestValueNote = lookupTable.get(highestValueComparableIterator);
            return new Harmonic(highestValueNote, nextHarmonicAsFraction, volumeTable.get(highestValueNote));
        }
        catch(NullPointerException e){
            return null;
        }
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
