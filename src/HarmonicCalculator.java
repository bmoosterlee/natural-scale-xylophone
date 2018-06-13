import java.util.LinkedList;
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
        noteEnvironment.addNoteObservable.addObserver((Observer<Note>) note -> {
            synchronized(harmonicHierarchy) {
                prioritizeNoteHarmonicCalculator(note);
            }
        });
        noteEnvironment.removeNoteObservable.addObserver((Observer<Note>) note -> {
            synchronized(harmonicHierarchy) {
                for(NoteHarmonicCalculator calculator : harmonicHierarchy){
                    if(calculator.getNote() == note){
                        harmonicHierarchy.remove(calculator);
                        break;
                    }
                }
            }
        });
    }

    private long getLastSampleCount() {
        return lastSampleCount;
    }

    public Harmonic getNextHarmonic(long currentSampleCount) {
        Harmonic highestValueHarmonic;
        synchronized(harmonicHierarchy) {
            if (harmonicHierarchy.isEmpty()) {
                return null;
            }
            if(currentSampleCount>lastSampleCount) {
                lastSampleCount = currentSampleCount;
                rebuildHarmonicHierarchy();
            }

            NoteHarmonicCalculator highestValueHarmonicCalculator = harmonicHierarchy.poll();
            highestValueHarmonic = highestValueHarmonicCalculator.poll();
            harmonicHierarchy.add(highestValueHarmonicCalculator);
        }

        return highestValueHarmonic;
    }

    private void rebuildHarmonicHierarchy() {
        LinkedList<Note> liveNotes = new LinkedList<>();
        while(!harmonicHierarchy.isEmpty()){
            liveNotes.add(harmonicHierarchy.poll().getNote());
        }
        for(Note note : liveNotes) {
            prioritizeNoteHarmonicCalculator(note);
        }
    }

    private void prioritizeNoteHarmonicCalculator(Note note) {
        harmonicHierarchy.add(new NoteHarmonicCalculator(note, noteEnvironment.getVolume(note, getLastSampleCount()), fractionCalculator));
    }

}
