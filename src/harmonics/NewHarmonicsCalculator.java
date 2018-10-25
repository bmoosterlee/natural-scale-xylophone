package harmonics;

import frequency.Frequency;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;

class NewHarmonicsCalculator {
    private final Map<Frequency, Double> volumes;
    private final CurrentTable<MemoableIterator> iterators;
    private final PriorityQueue<Frequency> hierarchy;

    NewHarmonicsCalculator(){
        this(new HashMap<>(), new CurrentTable<>(MemoableIterator::new), new PriorityQueue<>());
    }

    private NewHarmonicsCalculator(Map<Frequency, Double> volumes, CurrentTable<MemoableIterator> iterators, PriorityQueue<Frequency> hierarchy) {
        this.volumes = volumes;
        this.iterators = iterators;
        this.hierarchy = hierarchy;
    }

    NewHarmonicsCalculator update(Set<Frequency> liveFrequencies, Map<Frequency, Double> newVolumes) {
        CurrentTable<MemoableIterator> newIterators = iterators.update(liveFrequencies);

        PriorityQueue<Frequency> newHierarchy = new PriorityQueue<>((o1, o2) -> -Double.compare(
                Harmonic.getHarmonicValue(newVolumes.get(o1), newIterators.get(o1).peek()),
                Harmonic.getHarmonicValue(newVolumes.get(o2), newIterators.get(o2).peek()))
        );
        newHierarchy.addAll(liveFrequencies);

        return new NewHarmonicsCalculator(newVolumes, newIterators, newHierarchy);
    }

    private Frequency poll() {
        Frequency highestValueFrequency = hierarchy.poll();
        hierarchy.add(highestValueFrequency);
        return highestValueFrequency;
    }

    double getNextHarmonicValue() {
        try {
            Frequency highestValueFrequency = hierarchy.peek();
            Fraction nextHarmonicAsFraction = iterators.get(highestValueFrequency).peek();

            return Harmonic.getHarmonicValue(volumes.get(highestValueFrequency), nextHarmonicAsFraction);
        }
        catch(NullPointerException e){
            return 0.;
        }
    }

    public SimpleImmutableEntry<Frequency, SimpleImmutableEntry<Harmonic, Double>> next() {
        Frequency highestValueFrequency = poll();
        Fraction highestValueHarmonicAsFraction = iterators.get(highestValueFrequency).next();

        Harmonic highestValueHarmonic = new Harmonic(highestValueFrequency, highestValueHarmonicAsFraction);
        double highestValue = highestValueHarmonic.getVolume(volumes.get(highestValueFrequency));

        return new SimpleImmutableEntry<>(highestValueFrequency, new SimpleImmutableEntry<>(highestValueHarmonic, highestValue));
    }
}
