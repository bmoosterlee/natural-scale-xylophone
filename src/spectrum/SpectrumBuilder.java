package spectrum;

import component.Pulse;
import component.TimedConsumer;
import component.buffer.*;
import frequency.Frequency;
import sound.VolumeState;
import sound.VolumeStateMap;
import spectrum.harmonics.Harmonic;
import spectrum.harmonics.HarmonicCalculator;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class SpectrumBuilder {

    public static <A extends Packet<Double[]>, B extends Packet<VolumeState>> SimpleImmutableEntry<BoundedBuffer<Double[], A>, BoundedBuffer<Double[], SimplePacket<Double[]>>> buildComponent(BoundedBuffer<Pulse, ? extends Packet<Pulse>> frameTickBuffer, BoundedBuffer<VolumeState, B> inputBuffer, SpectrumWindow spectrumWindow) {
        BufferChainLink<VolumeState, B> volumeStatePacketBufferChainLink = frameTickBuffer
                .performMethod(TimedConsumer.consumeFrom(inputBuffer), "spectrum builder - consume from volume state buffer");
        LinkedList<BoundedBuffer<VolumeState, B>> volumeBroadcast =
                new LinkedList<>(
                        volumeStatePacketBufferChainLink
                                .broadcast(2, "spectrum builder - volume broadcast"));

        return new SimpleImmutableEntry<>(
                buildNoteSpectrumPipe(volumeBroadcast.poll(), spectrumWindow),
                buildHarmonicSpectrumPipe(volumeBroadcast.poll(), spectrumWindow));
    }

    private static <A extends Packet<Double[]>, B extends Packet<VolumeState>> BufferChainLink<Double[], A> buildNoteSpectrumPipe(BoundedBuffer<VolumeState, B> inputBuffer, SpectrumWindow spectrumWindow) {
        return inputBuffer
                .performMethod(input -> input.volumes, "build note spectrum");
    }

    private static <B extends Packet<VolumeState>, C extends Packet<VolumeStateMap>> BoundedBuffer<Double[], SimplePacket<Double[]>> buildHarmonicSpectrumPipe(BoundedBuffer<VolumeState, B> volumeBuffer, SpectrumWindow spectrumWindow) {
        int maxHarmonics = 200;

        Double[][] harmonics = new Double[spectrumWindow.width][spectrumWindow.width];
        PipeCallable<VolumeStateMap, Iterator<Map.Entry<Harmonic, Double>>> volumeStateMapIteratorPipeCallable = HarmonicCalculator.calculateHarmonics(maxHarmonics);
        for (int i = 0; i < spectrumWindow.width; i++) {
            HashMap<Frequency, Double> temp = new HashMap<>();
            temp.put(spectrumWindow.staticFrequencyWindow.get(i), 1.);
            Double[] harmonicsForThisIndex = new Double[spectrumWindow.width];
            for (int j = 0; j < spectrumWindow.width; j++) {
                harmonicsForThisIndex[j] = 0.;
            }
            volumeStateMapIteratorPipeCallable.call(new VolumeStateMap(temp)).forEachRemaining(harmonicWithVolume -> {
                Frequency frequency = harmonicWithVolume.getKey().getHarmonicFrequency();
                int x = (spectrumWindow.getX(frequency));
                if(x >= 0 && x < spectrumWindow.width) {
                    harmonicsForThisIndex[x] += harmonicWithVolume.getValue();
                }
            });
            harmonics[i] = harmonicsForThisIndex;
        }
        
        return volumeBuffer.performMethod(input -> {
            Double[] harmonicsForThisVolumeSpectrum = new Double[spectrumWindow.width];
            for(int i = 0; i<spectrumWindow.width; i++){
                harmonicsForThisVolumeSpectrum[i] = 0.;
            }
            for(int i = 0; i<spectrumWindow.width; i++){
                for(int j = 0; j<spectrumWindow.width; j++) {
                    harmonicsForThisVolumeSpectrum[j] += input.volumes[i] * harmonics[i][j];
                }
            }
            return harmonicsForThisVolumeSpectrum;
        }, 200, "spectrum builder - calculate harmonics");
    }

    private static <A extends Packet<Map.Entry<Harmonic, Double>>, B extends Packet<Iterator<Map.Entry<Harmonic, Double>>>> BoundedBuffer<Map.Entry<Harmonic, Double>, A> calculateHarmonicsContinuously(BoundedBuffer<Iterator<Map.Entry<Harmonic, Double>>, B> harmonicsIteratorBuffer, int maxHarmonics) {
        SimpleBuffer<Map.Entry<Harmonic, Double>, A> outputBuffer = new SimpleBuffer<>(maxHarmonics, "spectrum builder - harmonic calculation");

        new TickRunningStrategy(new AbstractPipeComponent<>(harmonicsIteratorBuffer.createInputPort(), outputBuffer.createOutputPort()){
            @Override
            protected void tick() {
                try {
                    B harmonicsIterator = input.consume();
                    while (input.isEmpty() && harmonicsIterator.unwrap().hasNext()) {
                        output.produce(harmonicsIterator.transform(Iterator::next));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });

        return outputBuffer;
    }

}