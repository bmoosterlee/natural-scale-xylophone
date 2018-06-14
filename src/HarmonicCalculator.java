import java.util.PriorityQueue;

public class HarmonicCalculator {

    private final NoteEnvironment noteEnvironment;
    long lastSampleCount = -1;

    PriorityQueue<NoteHarmonicCalculator> harmonicHierarchy;
    private final FractionCalculator fractionCalculator;

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
            harmonicHierarchy.clear();
            for(Note note : noteEnvironment.getLiveNotes()) {
                harmonicHierarchy.add(new NoteHarmonicCalculator(note, noteEnvironment.getVolume(note, getLastSampleCount()), fractionCalculator));
            }
        }
        if (harmonicHierarchy.isEmpty()) {
            return null;
        }

        NoteHarmonicCalculator highestValueHarmonicCalculator = harmonicHierarchy.poll();
        highestValueHarmonic = highestValueHarmonicCalculator.poll();
        harmonicHierarchy.add(highestValueHarmonicCalculator);

        return highestValueHarmonic;
    }

}
