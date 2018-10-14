package gui.spectrum.state;

import gui.buckets.Buckets;
import gui.buckets.PrecalculatedBucketHistory;
import gui.spectrum.SpectrumWindow;
import harmonics.HarmonicCalculator;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import notes.state.VolumeState;
import time.PerformanceTracker;
import time.TimeInNanoSeconds;
import time.TimeKeeper;

public class SpectrumManager implements Runnable {
    private final SpectrumWindow spectrumWindow;
    private final HarmonicCalculator harmonicCalculator;

    private SpectrumState spectrumState;

    private final InputPort<TimeInNanoSeconds> frameEndTimeInput;
    private final InputPort<VolumeState> volumeStateInput;
    private final OutputPort<SpectrumState> spectrumStateOutput;

    public SpectrumManager(SpectrumWindow spectrumWindow, HarmonicCalculator harmonicCalculator, BoundedBuffer<TimeInNanoSeconds> frameEndTimeInputBuffer, BoundedBuffer<VolumeState> volumeStateBuffer, BoundedBuffer<SpectrumState> outputBuffer) {
        this.spectrumWindow = spectrumWindow;
        this.harmonicCalculator = harmonicCalculator;

        frameEndTimeInput = new InputPort<>(frameEndTimeInputBuffer);
        volumeStateInput = new InputPort<>(volumeStateBuffer);
        spectrumStateOutput = new OutputPort<>(outputBuffer);

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
            SpectrumStateBuilder spectrumStateBuilder = new SpectrumStateBuilder(spectrumWindow, volumeState.volumes, harmonicCalculator, spectrumState);
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("build spectrum snapshot");
            while (TimeInNanoSeconds.now().lessThan(frameEndTime)) {
                if (spectrumStateBuilder.update()) break;
            }
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("finish building spectrum snapshot");
            spectrumState = spectrumStateBuilder.finish();
            PerformanceTracker.stopTracking(timeKeeper);

            spectrumStateOutput.produce(spectrumState);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}