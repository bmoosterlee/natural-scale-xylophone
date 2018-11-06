package notes.state;

import frequency.Frequency;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import notes.envelope.DeterministicEnvelope;
import notes.envelope.Envelope;
import time.PerformanceTracker;
import time.TimeKeeper;
import wave.Wave;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class VolumeAmplitudeCalculator implements Runnable {

    private final Map<Long, EnvelopeWaveState> futureEnvelopeWaveStates;

    private final InputPort<TimestampedEnvelopeWaves> input;
    private final OutputPort<VolumeAmplitudeState> output;

    public VolumeAmplitudeCalculator(BoundedBuffer<TimestampedEnvelopeWaves> input, BoundedBuffer<VolumeAmplitudeState> output){
        futureEnvelopeWaveStates = new HashMap<>();

        this.input = new InputPort<>(input);
        this.output = new OutputPort<>(output);

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
            TimestampedEnvelopeWaves timestampedNewNotes = input.consume();

            long sampleCount = timestampedNewNotes.getSampleCount();
            DeterministicEnvelope envelope = timestampedNewNotes.getEnvelope();
            Map<Frequency, Wave> waves = timestampedNewNotes.getWaves();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("calculate volumes");
            addEnvelopeWaves(sampleCount, waves.keySet(), envelope, waves);

            VolumeAmplitudeState currentState;
            try {
                currentState = futureEnvelopeWaveStates.remove(sampleCount).calculateValue();
            }
            catch(NullPointerException e){
                currentState = new VolumeAmplitudeState(new HashMap<>());
            }
            PerformanceTracker.stopTracking(timeKeeper);

            output.produce(currentState);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void addEnvelopeWaves(Long sampleCount, Collection<Frequency> newNotes, DeterministicEnvelope envelope, Map<Frequency, Wave> waves) {
        if(!newNotes.isEmpty()) {
            for (Long i = sampleCount; i < envelope.getEndingSampleCount(); i++) {
                addEnvelopeWaves(newNotes, i, envelope, waves);
            }
        }
    }

    private void addEnvelopeWaves(Collection<Frequency> newNotes, Long sampleCount, Envelope envelope, Map<Frequency, Wave> waves) {
        EnvelopeWaveState newVolumeState = getOldVolumeAmplitudeState(sampleCount);

        for (Frequency frequency : newNotes) {
            newVolumeState = newVolumeState.add(frequency, envelope, waves.get(frequency));
        }

        futureEnvelopeWaveStates.put(sampleCount, newVolumeState);
    }

    private EnvelopeWaveState getOldVolumeAmplitudeState(Long sampleCount) {
        EnvelopeWaveState oldVolumeState;

        if (futureEnvelopeWaveStates.containsKey(sampleCount)) {
            oldVolumeState = futureEnvelopeWaveStates.get(sampleCount);
        } else {
            oldVolumeState = new EnvelopeWaveState(sampleCount, new HashMap<>());
        }

        return oldVolumeState;
    }

}
