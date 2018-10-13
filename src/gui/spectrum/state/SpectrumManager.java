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
    private SpectrumWindow spectrumWindow;
    private HarmonicCalculator harmonicCalculator;

    private SpectrumState spectrumState;

    private InputPort<SpectrumInput> spectrumData;
    private InputPort<VolumeState> volumeStateInput;
    private OutputPort<SpectrumState> newSpectrumState;

    public SpectrumManager(SpectrumWindow spectrumWindow, HarmonicCalculator harmonicCalculator, BoundedBuffer<SpectrumInput> spectrumInputBuffer, BoundedBuffer<VolumeState> volumeStateBuffer, BoundedBuffer<SpectrumState> outputBuffer) {
        this.spectrumWindow = spectrumWindow;
        this.harmonicCalculator = harmonicCalculator;

        spectrumData = new InputPort<>(spectrumInputBuffer);
        volumeStateInput = new InputPort<>(volumeStateBuffer);
        newSpectrumState = new OutputPort<>(outputBuffer);

        this.spectrumState = new SpectrumState(new Buckets(), new Buckets(), new PrecalculatedBucketHistory(200));

        start();
    }

    public void tick() {
        try {
            SpectrumInput spectrumInput = spectrumData.consume();
            VolumeState volumeState = volumeStateInput.consume();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("create spectrum snapshot");
            SpectrumStateBuilder spectrumStateBuilder = new SpectrumStateBuilder(spectrumWindow, volumeState.volumes, harmonicCalculator, spectrumState, volumeState.sampleCount);
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("build spectrum snapshot");
            TimeInNanoSeconds frameEndTime = spectrumInput.getFrameEndTime();
            while (TimeInNanoSeconds.now().lessThan(frameEndTime)) {
                if (spectrumStateBuilder.update()) break;
            }
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("finish building spectrum snapshot");
            SpectrumState newSpectrumState = spectrumStateBuilder.finish();
            this.spectrumState = newSpectrumState;
            try {
                this.newSpectrumState.produce(spectrumState);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            PerformanceTracker.stopTracking(timeKeeper);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(true){
            tick();
        }
    }

    public void start() {
        new Thread(this).start();
    }
}