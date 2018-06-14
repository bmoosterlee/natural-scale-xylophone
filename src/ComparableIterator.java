import java.util.Iterator;

public class ComparableIterator implements Comparable<ComparableIterator>, Iterator<Fraction> {

    private final Iterator<Fraction> iterator;
    private Fraction currentHarmonicAsFraction;
    private final double noteVolume;

    public ComparableIterator(double noteVolume){
        iterator = new MemoizedIterator();
        this.noteVolume = noteVolume;
        currentHarmonicAsFraction = iterator.next();
    }

    public boolean hasNext(){
        return true;
    }

    public Fraction next(){
        Fraction currentHarmonicAsFraction = this.currentHarmonicAsFraction;
        this.currentHarmonicAsFraction = iterator.next();
        return currentHarmonicAsFraction;
    }

    @Override
    public int compareTo(ComparableIterator o) {
        return -Double.compare(Harmonic.getSonanceValue(noteVolume, currentHarmonicAsFraction), Harmonic.getSonanceValue(o.noteVolume, o.currentHarmonicAsFraction));
    }

}
