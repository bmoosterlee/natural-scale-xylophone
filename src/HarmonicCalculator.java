import java.util.HashMap;
import java.util.PriorityQueue;

public class HarmonicCalculator {

    private final NoteEnvironment noteEnvironment;
    long lastSampleCount = -1;

    PriorityQueue<NoteHarmonicCalculator> harmonicHierarchy;
    private final FractionCalculator fractionCalculator;
    private HashMap<NoteHarmonicCalculator, Note> lookupTable;
    private HashMap<Note, Double> volumeTable;

    public HarmonicCalculator(NoteEnvironment noteEnvironment){
        this.noteEnvironment = noteEnvironment;
        fractionCalculator = new FractionCalculator();
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
                NoteHarmonicCalculator noteHarmonicCalculator = new NoteHarmonicCalculator(volume, fractionCalculator);
                lookupTable.put(noteHarmonicCalculator, note);
                volumeTable.put(note, volume);
                harmonicHierarchy.add(noteHarmonicCalculator);
            }
        }
        if (harmonicHierarchy.isEmpty()) {
            return null;
        }

        NoteHarmonicCalculator highestValueHarmonicCalculator = harmonicHierarchy.poll();
        Note highestValueNote = lookupTable.get(highestValueHarmonicCalculator);
        highestValueHarmonic = new Harmonic(highestValueNote, highestValueHarmonicCalculator.poll());
        highestValueHarmonic.noteVolume = volumeTable.get(highestValueNote);
        harmonicHierarchy.add(highestValueHarmonicCalculator);

        return highestValueHarmonic;
    }

}
