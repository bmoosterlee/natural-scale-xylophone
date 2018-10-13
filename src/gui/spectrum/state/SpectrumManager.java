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

    private InputPort<SpectrumData> spectrumDataInput;
    private InputPort<VolumeState> volumeStateInput;
    private OutputPort<SpectrumState> spectrumStateOutput;

    public SpectrumManager(SpectrumWindow spectrumWindow, HarmonicCalculator harmonicCalculator, BoundedBuffer<SpectrumData> spectrumInputBuffer, BoundedBuffer<VolumeState> volumeStateBuffer, BoundedBuffer<SpectrumState> outputBuffer) {
        this.spectrumWindow = spectrumWindow;
        this.harmonicCalculator = harmonicCalculator;

        spectrumDataInput = new InputPort<>(spectrumInputBuffer);
        volumeStateInput = new InputPort<>(volumeStateBuffer);
        spectrumStateOutput = new OutputPort<>(outputBuffer);

        this.spectrumState = new SpectrumState(new Buckets(), new Buckets(), new PrecalculatedBucketHistory(200));

        start();
    }

    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        while(true){
            tick();
        }
    }

    public void tick() {
        try {
            TimeKeeper timeKeeper = PerformanceTracker.startTracking("create spectrum snapshot");
            SpectrumData spectrumData = spectrumDataInput.consume();
            VolumeState volumeState = volumeStateInput.consume();
            SpectrumStateBuilder spectrumStateBuilder = new SpectrumStateBuilder(spectrumWindow, volumeState.volumes, harmonicCalculator, spectrumState, volumeState.sampleCount);
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("build spectrum snapshot");
            TimeInNanoSeconds frameEndTime = spectrumData.getFrameEndTime();
            while (TimeInNanoSeconds.now().lessThan(frameEndTime)) {
                if (spectrumStateBuilder.update()) break;
            }
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("finish building spectrum snapshot");
            spectrumState = spectrumStateBuilder.finish();
            spectrumStateOutput.produce(spectrumState);
            PerformanceTracker.stopTracking(timeKeeper);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}