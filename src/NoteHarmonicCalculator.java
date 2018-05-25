public class NoteHarmonicCalculator implements Comparable<NoteHarmonicCalculator>{

    Note note;
    Harmonic currentHarmonic;
    RelativelyPrimeFractionIterator iterator;
    long lastSampleCount;

    public NoteHarmonicCalculator(Note note, long sampleCount){
        this.note = note;
        iterator = new RelativelyPrimeFractionIterator();
        currentHarmonic = new Harmonic(note, iterator.next());
        lastSampleCount = sampleCount;
    }

    public Harmonic poll(){
        Harmonic tempCurrentHarmonic = currentHarmonic;
        currentHarmonic = new Harmonic(note, iterator.next());
        return tempCurrentHarmonic;
    }

    public Harmonic peek(){
        currentHarmonic.lastSampleCount = lastSampleCount;
        return currentHarmonic;
    }

    @Override
    public int compareTo(NoteHarmonicCalculator o) {
        return peek().compareTo(o.peek());
    }
}
