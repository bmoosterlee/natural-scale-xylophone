import java.util.HashMap;
import java.util.PriorityQueue;

public class HarmonicCalculator {

    private final NoteEnvironment noteEnvironment;
    long lastSampleCount = -1;

    PriorityQueue<ComparableIterator> harmonicHierarchy;
    private HashMap<ComparableIterator, Note> lookupTable;
    private HashMap<Note, Double> volumeTable;

    public HarmonicCalculator(NoteEnvironment noteEnvironment){
        this.noteEnvironment = noteEnvironment;
        harmonicHierarchy = new PriorityQueue<>();
    }

    private long getLastSampleCount() {
        return lastSampleCount;
    }


    public Harmonic getNextHarmonic(long currentSampleCount) {
        Harmonic highestValueHarmonic;
        if(currentSampleCount>lastSampleCount) {
            lastSampleCount = currentSampleCount;
            lookupTable = new HashMap<>();
            volumeTable = new HashMap<>();
            harmonicHierarchy.clear();
            for(Note note : noteEnvironment.getLiveNotes()) {
                double volume = noteEnvironment.getVolume(note, getLastSampleCount());
                ComparableIterator comparableIterator = new ComparableIterator(volume);
                lookupTable.put(comparableIterator, note);
                volumeTable.put(note, volume);
                harmonicHierarchy.add(comparableIterator);
            }
        }
        if (harmonicHierarchy.isEmpty()) {
            return null;
        }

        ComparableIterator highestValueHarmonicCalculator = harmonicHierarchy.poll();
        Note highestValueNote = lookupTable.get(highestValueHarmonicCalculator);
        highestValueHarmonic = new Harmonic(highestValueNote, highestValueHarmonicCalculator.next());
        highestValueHarmonic.noteVolume = volumeTable.get(highestValueNote);
        harmonicHierarchy.add(highestValueHarmonicCalculator);

        return highestValueHarmonic;
    }

}
