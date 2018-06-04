import java.util.ArrayList;

public class NoteHarmonicCalculator implements Comparable<NoteHarmonicCalculator>{

    Note note;
    Fraction currentFraction;
    int index;
    ArrayList<Fraction> calculatedFractions;
    int lastCalculatedIndex;
    RelativelyPrimeFractionIterator iterator;
    double noteVolume;

    public NoteHarmonicCalculator(Note note, double noteVolume){
        this.note = note;
        iterator = new RelativelyPrimeFractionIterator();
        calculatedFractions = new ArrayList<>();
        index = 0;
        lastCalculatedIndex = -1;
        currentFraction = getNextHarmonic();
        this.noteVolume = noteVolume;
    }

    public Harmonic poll(){
        Harmonic currentHarmonic = new Harmonic(note, currentFraction);
        currentHarmonic.noteVolume = noteVolume;

        index++;
        currentFraction = getNextHarmonic();
        return currentHarmonic;
    }

    private Fraction getNextHarmonic() {
        if(index>lastCalculatedIndex){
            Fraction newFraction  = iterator.next();
            calculatedFractions.add(newFraction);
            lastCalculatedIndex++;
            return newFraction;
        }
        else{
            return calculatedFractions.get(index);
        }
    }

    public Harmonic peek(){
        Harmonic currentHarmonic = new Harmonic(note, currentFraction);
        currentHarmonic.noteVolume = noteVolume;
        return currentHarmonic;
    }

    @Override
    public int compareTo(NoteHarmonicCalculator o) {
        return peek().compareTo(o.peek());
    }
}
