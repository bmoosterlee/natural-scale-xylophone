import java.util.Iterator;

public class ComparableIterator implements Comparable<ComparableIterator>, Iterator<Fraction> {

    private final MemoizedIterator memoizedIterator;
    private Fraction currentHarmonicAsFraction;
    private final double noteVolume;

    public ComparableIterator(double noteVolume){
        memoizedIterator = new MemoizedIterator();
        this.noteVolume = noteVolume;
        currentHarmonicAsFraction = memoizedIterator.next();
    }

    public boolean hasNext(){
        return true;
    }

    public Fraction next(){
        Fraction currentHarmonicAsFraction = this.currentHarmonicAsFraction;
        this.currentHarmonicAsFraction = memoizedIterator.next();
        return currentHarmonicAsFraction;
    }

    @Override
    public int compareTo(ComparableIterator o) {
        return -Double.compare(Harmonic.getSonanceValue(noteVolume, currentHarmonicAsFraction), Harmonic.getSonanceValue(o.noteVolume, o.currentHarmonicAsFraction));
    }

}
