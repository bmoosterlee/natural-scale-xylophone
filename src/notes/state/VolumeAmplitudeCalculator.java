package notes.state;

import frequency.Frequency;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import notes.envelope.DeterministicEnvelope;
import notes.envelope.SimpleDeterministicEnvelope;
import notes.envelope.functions.DeterministicFunction;
import notes.envelope.functions.LinearFunctionMemoizer;
import sound.SampleRate;
import time.PerformanceTracker;
import time.TimeInSeconds;
import time.TimeKeeper;
import wave.Wave;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VolumeAmplitudeCalculator implements Runnable {

    private final SampleRate sampleRate;
    private final DeterministicFunction envelopeFunction;

    private final Map<Long, VolumeAmplitudeState> futureVolumeAmplitudes;

    private final InputPort<TimestampedFrequencies> timestampedNewNotesInput;
    private final OutputPort<VolumeAmplitudeState> volumeAmplitudeStateOutput;

    public VolumeAmplitudeCalculator(BoundedBuffer<TimestampedFrequencies> timestampedNewNotesBuffer, BoundedBuffer<VolumeAmplitudeState> volumeAmplitudeStateOutputBuffer, SampleRate sampleRate){
        this.sampleRate = sampleRate;
        envelopeFunction = LinearFunctionMemoizer.ENVELOPE_MEMOIZER.get(sampleRate, 0.10, new TimeInSeconds(0.7));

        futureVolumeAmplitudes = new HashMap<>();

        timestampedNewNotesInput = new InputPort<>(timestampedNewNotesBuffer);
        volumeAmplitudeStateOutput = new OutputPort<>(volumeAmplitudeStateOutputBuffer);

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
            TimestampedFrequencies timestampedNewNotes = timestampedNewNotesInput.consume();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("calculate volumes");
            long sampleCount = timestampedNewNotes.getSampleCount();

            addNotes(sampleCount, timestampedNewNotes.getFrequencies());

            VolumeAmplitudeState currentState = futureVolumeAmplitudes.remove(sampleCount);

            if(currentState==null){
                currentState = new VolumeAmplitudeState(new HashMap<>());
            }
            PerformanceTracker.stopTracking(timeKeeper);

            volumeAmplitudeStateOutput.produce(currentState);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void addNotes(Long sampleCount, Collection<Frequency> newNotes) {
        DeterministicEnvelope envelope = new SimpleDeterministicEnvelope(sampleCount, sampleRate, envelopeFunction);

        Map<Frequency, Wave> waves = new HashMap<>();
        for(Frequency frequency : newNotes){
            waves.put(frequency, new Wave(frequency, sampleRate));
        }

        if(!newNotes.isEmpty()) {
            for (Long i = sampleCount; i < envelope.getEndingSampleCount(); i++) {
                addVolumes(newNotes, i, envelope.getVolume(i), waves);
            }
        }
    }

    private void addVolumes(Collection<Frequency> newNotes, Long i, double volume, Map<Frequency, Wave> waves) {
        VolumeAmplitudeState newVolumeState = getOldVolumeAmplitudeState(i);

        for (Frequency frequency : newNotes) {
            newVolumeState = newVolumeState.add(frequency, volume, waves.get(frequency), i);
        }

        futureVolumeAmplitudes.put(i, newVolumeState);
    }

    private VolumeAmplitudeState getOldVolumeAmplitudeState(Long i) {
        VolumeAmplitudeState oldVolumeState;

        if (futureVolumeAmplitudes.containsKey(i)) {
            oldVolumeState = futureVolumeAmplitudes.get(i);
        } else {
            oldVolumeState = new VolumeAmplitudeState(new HashMap<>());
        }

        return oldVolumeState;
    }

}
