import java.util.ArrayList;
import java.util.Iterator;

public class FractionMemoizer {
    private ArrayList<Fraction> calculatedFractions;
    private int lastCalculatedIndex;
    private final Iterator<Fraction> iterator;

    public FractionMemoizer(Iterator<Fraction> iterator) {
        this.iterator = iterator;
        setCalculatedFractions(new ArrayList<>());
        setLastCalculatedIndex(-1);
    }

    public ArrayList<Fraction> getCalculatedFractions() {
        return calculatedFractions;
    }

    public void setCalculatedFractions(ArrayList<Fraction> calculatedFractions) {
        this.calculatedFractions = calculatedFractions;
    }

    public int getLastCalculatedIndex() {
        return lastCalculatedIndex;
    }

    public void setLastCalculatedIndex(int lastCalculatedIndex) {
        this.lastCalculatedIndex = lastCalculatedIndex;
    }

    public Fraction getCalculatedFraction(int index){
        return getCalculatedFractions().get(index);
    }

    public Fraction calculateNextFraction() {
        Fraction newFraction  = iterator.next();
        getCalculatedFractions().add(newFraction);
        setLastCalculatedIndex(getLastCalculatedIndex() + 1);
        return newFraction;
    }

    public Fraction getFraction(int index) {
        if(index > getLastCalculatedIndex()){
            return calculateNextFraction();
        }
        else{
            return getCalculatedFraction(index);
        }
    }
}