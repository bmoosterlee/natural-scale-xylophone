import java.util.ArrayList;

public class FractionCalculator {
    private ArrayList<Fraction> calculatedFractions;
    private int lastCalculatedIndex;
    private RelativelyPrimeFractionIterator iterator;

    public FractionCalculator() {
        setIterator(new RelativelyPrimeFractionIterator());
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

    public RelativelyPrimeFractionIterator getIterator() {
        return iterator;
    }

    public void setIterator(RelativelyPrimeFractionIterator iterator) {
        this.iterator = iterator;
    }

    public Fraction getCalculatedFraction(int index){
        return getCalculatedFractions().get(index);
    }

    public Fraction calculateNextFraction() {
        Fraction newFraction  = getIterator().next();
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