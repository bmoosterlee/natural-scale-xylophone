package spectrum.harmonics;

import component.BoundedBuffer;
import component.CallableWithArguments;
import component.TickablePipeComponent;
import frequency.Frequency;
import mixer.state.VolumeState;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class HarmonicCalculator extends TickablePipeComponent<VolumeState, Iterator<Entry<Harmonic, Double>>> {

    public HarmonicCalculator(int maxHarmonics, BoundedBuffer<VolumeState> inputBuffer, BoundedBuffer<Iterator<Map.Entry<Harmonic, Double>>> outputBuffer){
         super(inputBuffer, outputBuffer, calculateHarmonics(maxHarmonics));
    }

    public static CallableWithArguments<VolumeState, Iterator<Map.Entry<Harmonic, Double>>> calculateHarmonics(int maxHarmonics1){
        return new CallableWithArguments<>() {
            private NewHarmonicsCalculator newHarmonicsCalculator;
            private MemoizedHighValueHarmonics memoizedHighValueHarmonics;

            private final int maxHarmonics;

            {
                this.maxHarmonics = maxHarmonics1;

                newHarmonicsCalculator = new NewHarmonicsCalculator();
                memoizedHighValueHarmonics = new MemoizedHighValueHarmonics();
            }

            private Iterator<Entry<Harmonic, Double>> calculateHarmonics(VolumeState volumeState) {
                Map<Frequency, Double> volumes = volumeState.volumes;
                Set<Frequency> liveFrequencies = volumes.keySet();
                Iterator<Entry<Harmonic, Double>> harmonicHierarchyIterator = getHarmonicHierarchyIterator(liveFrequencies, volumes);
                return harmonicHierarchyIterator;
            }

            private Iterator<Entry<Harmonic, Double>> getHarmonicHierarchyIterator(Set<Frequency> liveFrequencies, Map<Frequency, Double> volumes) {
                newHarmonicsCalculator = newHarmonicsCalculator.update(liveFrequencies, volumes);
                memoizedHighValueHarmonics = memoizedHighValueHarmonics.update(liveFrequencies, volumes);

                addNewHarmonicsToHarmonicsStorage();

                return memoizedHighValueHarmonics.getHarmonicHierarchyAsList().iterator();
            }

            private void addNewHarmonicsToHarmonicsStorage() {
                while (newHarmonicsCalculator.getNextHarmonicValue() > memoizedHighValueHarmonics.getHarmonicValue(maxHarmonics)) {
                    addNewHarmonic();
                }
            }

            private void addNewHarmonic() {
                try {
                    SimpleImmutableEntry<Frequency, SimpleImmutableEntry<Harmonic, Double>> highestValuePair = newHarmonicsCalculator.next();

                    Frequency highestValueFrequency = highestValuePair.getKey();
                    Harmonic highestValueHarmonic = highestValuePair.getValue().getKey();
                    Double highestValue = highestValuePair.getValue().getValue();

                    memoizedHighValueHarmonics.addHarmonic(highestValueFrequency, highestValueHarmonic, highestValue);
                } catch (NullPointerException ignored) {
                }
            }

            @Override
            public Iterator<Entry<Harmonic, Double>> call(VolumeState input) {
                return calculateHarmonics(input);
            }
        };
    }

}
