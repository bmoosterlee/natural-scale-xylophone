package spectrum.harmonics;

import component.buffer.*;
import frequency.Frequency;
import mixer.state.VolumeState;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class HarmonicCalculator<A extends Packet<VolumeState>, B extends Packet<Iterator<Entry<Harmonic, Double>>>> extends MethodPipeComponent<VolumeState, Iterator<Entry<Harmonic, Double>>, A, B> {

    public HarmonicCalculator(SimpleBuffer<VolumeState, A> inputBuffer, SimpleBuffer<Iterator<Entry<Harmonic, Double>>, B> outputBuffer, int maxHarmonics){
         super(inputBuffer, outputBuffer, calculateHarmonics(maxHarmonics));
    }

    public static PipeCallable<VolumeState, Iterator<Entry<Harmonic, Double>>> calculateHarmonics(int maxHarmonics1){
        return new PipeCallable<>() {
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
