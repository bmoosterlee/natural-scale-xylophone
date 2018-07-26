import java.util.Iterator;

public class ComparableIterator implements Comparable<ComparableIterator>, Iterator<Fraction> {

    private final Iterator<Fraction> iterator;
    private Fraction currentHarmonicAsFraction;
    public double noteVolume;

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
        return -Double.compare(Harmonic.getHarmonicValue(noteVolume, currentHarmonicAsFraction), Harmonic.getHarmonicValue(o.noteVolume, o.currentHarmonicAsFraction));
    }

}
