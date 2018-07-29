import java.util.Iterator;

public class MemoableIterator implements Iterator<Fraction> {

    private final Iterator<Fraction> iterator;
    Fraction currentHarmonicAsFraction;

    public MemoableIterator(){
        iterator = new MemoizedIterator();
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

    public Fraction peek(){
        return currentHarmonicAsFraction;
    }

}
