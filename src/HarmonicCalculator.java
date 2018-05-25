import java.util.LinkedList;
import java.util.PriorityQueue;

public class HarmonicCalculator {

    private final NoteEnvironment noteEnvironment;
    long lastSampleCount = -1;

    PriorityQueue<NoteHarmonicCalculator> noteHarmonicCalculators;
    Observer addNoteObserver;
    Observer removeNoteObserver;

    public HarmonicCalculator(NoteEnvironment noteEnvironment){
        this.noteEnvironment = noteEnvironment;

        noteHarmonicCalculators = new PriorityQueue<>();
        noteEnvironment.addNoteObservable.addObserver((Observer<Note>) note -> {
            synchronized(noteHarmonicCalculators) {
                noteHarmonicCalculators.add(new NoteHarmonicCalculator(note, lastSampleCount));
            }
        });
        noteEnvironment.removeNoteObservable.addObserver((Observer<Note>) note -> {
            synchronized(noteHarmonicCalculators) {
                for(NoteHarmonicCalculator calculator : noteHarmonicCalculators){
                    if(calculator.note == note){
                        noteHarmonicCalculators.remove(calculator);
                        break;
                    }
                }
            }
        });
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
                    liveNotes.add(noteHarmonicCalculators.poll().note);
                }
                for(Note note : liveNotes) {
                    noteHarmonicCalculators.add(new NoteHarmonicCalculator(note, currentSampleCount));
                }
            }

            NoteHarmonicCalculator highestValueHarmonicCalculator = noteHarmonicCalculators.poll();
            highestValueHarmonic = highestValueHarmonicCalculator.poll();
            noteHarmonicCalculators.add(highestValueHarmonicCalculator);
        }

        return highestValueHarmonic;
    }

    //Find the next fraction by increasing the denominator and cycling through the numerators
    //we then reduce each fraction using euclid's algorithm.
    //After reducing, we need to check whether the fraction was already used.
    //This is easy however, since any fraction that can be reduced has already been used,
    //since we go through the fractions in order of denominator.
    //Therefore, if euclid's algorithm's solution is 1, we have a new unique fraction.
    //We thus only need to return a boolean for euclid's algorithm.

    //Since we loop through the same fractions each tick, it might be beneficial to store these
    //fractions, and only calculate new ones when we run out of the list.
    //If we are able to calculate the performance strain upon calculating a new fraction,
    //and the performance strain of not having the memory reserved for the last stored fraction,
    //we could throw away fractions that are not being used at the moment.

    //Denominator shows the common period of the two notes, and thus defines the 'sonance' value

}
