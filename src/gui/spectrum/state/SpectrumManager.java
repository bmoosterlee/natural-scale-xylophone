package gui.spectrum.state;

import frequency.state.FrequencyManager;
import gui.buckets.Buckets;
import gui.buckets.PrecalculatedBucketHistory;
import gui.spectrum.SpectrumWindow;
import harmonics.HarmonicCalculator;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import notes.envelope.EnvelopeManager;
import time.PerformanceTracker;
import time.TimeInNanoSeconds;
import time.TimeKeeper;

public class SpectrumManager implements Runnable {
    private SpectrumWindow spectrumWindow;
    private FrequencyManager frequencyManager;
    private EnvelopeManager envelopeManager;
    private HarmonicCalculator harmonicCalculator;

    private SpectrumState spectrumState;

    private InputPort<SpectrumInput> newSpectrumInput;
    private OutputPort<SpectrumState> newSpectrumState;

    public SpectrumManager(SpectrumWindow spectrumWindow, FrequencyManager frequencyManager, EnvelopeManager envelopeManager, HarmonicCalculator harmonicCalculator, BoundedBuffer<SpectrumInput> inputBuffer, BoundedBuffer<SpectrumState> outputBuffer) {
        this.spectrumWindow = spectrumWindow;
        this.frequencyManager = frequencyManager;
        this.envelopeManager = envelopeManager;
        this.harmonicCalculator = harmonicCalculator;

        newSpectrumInput = new InputPort<>(inputBuffer);
        newSpectrumState = new OutputPort<>(outputBuffer);

        setSpectrumState(new SpectrumState(new Buckets(), new Buckets(), new PrecalculatedBucketHistory(200)));

        start();
    }

    private void setSpectrumState(SpectrumState spectrumState) {
        this.spectrumState = spectrumState;
        try {
            newSpectrumState.produce(spectrumState);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void tick() {
        try {
            SpectrumInput spectrumInput = newSpectrumInput.consume();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("create spectrum snapshot");
            SpectrumStateBuilder spectrumStateBuilder = new SpectrumStateBuilder(spectrumWindow, frequencyManager, envelopeManager, harmonicCalculator, spectrumState, spectrumInput.getExpectedTickCount());
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("build spectrum snapshot");
            while (TimeInNanoSeconds.now().lessThan(spectrumInput.getFrameEndTime())) {
                if (spectrumStateBuilder.update()) break;
            }
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("finish building spectrum snapshot");
            setSpectrumState(spectrumStateBuilder.finish());
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