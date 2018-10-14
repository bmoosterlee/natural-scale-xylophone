package gui.spectrum.state;

import frequency.Frequency;
import gui.buckets.Buckets;
import gui.buckets.PrecalculatedBucketHistory;
import gui.spectrum.SpectrumWindow;
import harmonics.Harmonic;
import harmonics.HarmonicCalculator;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import notes.state.VolumeState;
import time.PerformanceTracker;
import time.TimeInNanoSeconds;
import time.TimeKeeper;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SpectrumManager implements Runnable {
    private final SpectrumWindow spectrumWindow;
    private final HarmonicCalculator harmonicCalculator;

    private SpectrumState spectrumState;

    private final InputPort<TimeInNanoSeconds> frameEndTimeInput;
    private final InputPort<VolumeState> volumeStateInput;
    private final OutputPort<Buckets> notesOutput;
    private final OutputPort<Buckets> harmonicsOutput;

    public SpectrumManager(SpectrumWindow spectrumWindow, HarmonicCalculator harmonicCalculator, BoundedBuffer<TimeInNanoSeconds> frameEndTimeInputBuffer, BoundedBuffer<VolumeState> volumeStateBuffer, BoundedBuffer<Buckets> notesOutputBuffer, BoundedBuffer<Buckets> harmonicsOutputBuffer) {
        this.spectrumWindow = spectrumWindow;
        this.harmonicCalculator = harmonicCalculator;

        frameEndTimeInput = new InputPort<>(frameEndTimeInputBuffer);
        volumeStateInput = new InputPort<>(volumeStateBuffer);
        notesOutput = new OutputPort<>(notesOutputBuffer);
        harmonicsOutput = new OutputPort<>(harmonicsOutputBuffer);

        spectrumState = new SpectrumState(new Buckets(), new Buckets(), new PrecalculatedBucketHistory(200));

        start();
    }

    private void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        while(true){
            tick();
        }
    }

    private void tick() {
        try {
            TimeInNanoSeconds frameEndTime = frameEndTimeInput.consume();
            VolumeState volumeState = volumeStateInput.consume();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("create spectrum snapshot");
            Map<Frequency, Double> volumes = volumeState.volumes;
            Set<Frequency> liveFrequencies = volumes.keySet();
            Iterator<Map.Entry<Harmonic, Double>> harmonicHierarchyIterator = harmonicCalculator.getHarmonicHierarchyIterator(liveFrequencies, volumes);
            SpectrumStateBuilder spectrumStateBuilder = new SpectrumStateBuilder(spectrumWindow, liveFrequencies, volumes, spectrumState, harmonicHierarchyIterator);
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("build spectrum snapshot");
            while (TimeInNanoSeconds.now().lessThan(frameEndTime)) {
                if (spectrumStateBuilder.update()) break;
            }
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("finish building spectrum snapshot");
            spectrumState = spectrumStateBuilder.finish();
            PerformanceTracker.stopTracking(timeKeeper);

            notesOutput.produce(spectrumState.noteBuckets);
            harmonicsOutput.produce(spectrumState.harmonicsBuckets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}