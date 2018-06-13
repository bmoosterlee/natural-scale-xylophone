import java.util.LinkedList;
import java.util.PriorityQueue;

public class HarmonicCalculator {

    private final NoteEnvironment noteEnvironment;
    long lastSampleCount = -1;

    PriorityQueue<NoteHarmonicCalculator> noteHarmonicCalculators;
    private final FractionCalculator fractionCalculator;

    public HarmonicCalculator(NoteEnvironment noteEnvironment){
        this.noteEnvironment = noteEnvironment;

        fractionCalculator = new FractionCalculator();

        noteHarmonicCalculators = new PriorityQueue<>();
        noteEnvironment.addNoteObservable.addObserver((Observer<Note>) note -> {
            synchronized(noteHarmonicCalculators) {
                prioritizeNoteHarmonicCalculator(note);
            }
        });
        noteEnvironment.removeNoteObservable.addObserver((Observer<Note>) note -> {
            synchronized(noteHarmonicCalculators) {
                for(NoteHarmonicCalculator calculator : noteHarmonicCalculators){
                    if(calculator.getNote() == note){
                        noteHarmonicCalculators.remove(calculator);
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
        synchronized(noteHarmonicCalculators) {
            if (noteHarmonicCalculators.isEmpty()) {
                return null;
            }
            if(currentSampleCount>lastSampleCount) {
                lastSampleCount = currentSampleCount;
                LinkedList<Note> liveNotes = new LinkedList<>();
                while(!noteHarmonicCalculators.isEmpty()){
                    liveNotes.add(noteHarmonicCalculators.poll().getNote());
                }
                for(Note note : liveNotes) {
                    prioritizeNoteHarmonicCalculator(note);
                }
            }

            NoteHarmonicCalculator highestValueHarmonicCalculator = noteHarmonicCalculators.poll();
            highestValueHarmonic = highestValueHarmonicCalculator.poll();
            noteHarmonicCalculators.add(highestValueHarmonicCalculator);
        }

        return highestValueHarmonic;
    }

    private void prioritizeNoteHarmonicCalculator(Note note) {
        noteHarmonicCalculators.add(new NoteHarmonicCalculator(note, noteEnvironment.getVolume(note, getLastSampleCount()), fractionCalculator));
    }

}
