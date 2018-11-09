package spectrum.harmonics;

import java.util.ArrayList;
import java.util.Iterator;

class FractionMemoizer {
    private final ArrayList<Fraction> calculatedFractions;
    private int lastCalculatedIndex;
    private final Iterator<Fraction> iterator;

    public FractionMemoizer(Iterator<Fraction> iterator) {
        this.iterator = iterator;
        this.calculatedFractions = new ArrayList<>();
        this.lastCalculatedIndex = -1;
    }

    private Fraction getCalculatedFraction(int index){
        return calculatedFractions.get(index);
    }

    private Fraction calculateNextFraction() {
        Fraction newFraction  = iterator.next();
        calculatedFractions.add(newFraction);
        this.lastCalculatedIndex = lastCalculatedIndex + 1;
        return newFraction;
    }

    public Fraction getFraction(int index) {
        if(index > lastCalculatedIndex){
            return calculateNextFraction();
        }
        else{
            return getCalculatedFraction(index);
        }
    }
}