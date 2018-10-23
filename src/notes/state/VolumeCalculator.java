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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VolumeCalculator implements Runnable {

    private final SampleRate sampleRate;
    private final DeterministicFunction envelopeFunction;

    private final Map<Long, VolumeState> futureVolumes;

    private final InputPort<Long> sampleCountInput;
    private final InputPort<Frequency> newNoteInput;
    private final OutputPort<VolumeState> volumeStateOutput;

    public VolumeCalculator(BoundedBuffer<Long> sampleCountInputBuffer, BoundedBuffer<Frequency> newNoteInputBuffer, BoundedBuffer<VolumeState> volumeStateOutputBuffer, SampleRate sampleRate){
        this.sampleRate = sampleRate;
        envelopeFunction = LinearFunctionMemoizer.ENVELOPE_MEMOIZER.get(sampleRate, 0.10, new TimeInSeconds(0.7));

        futureVolumes = new HashMap<>();

        sampleCountInput = new InputPort<>(sampleCountInputBuffer);
        newNoteInput = new InputPort<>(newNoteInputBuffer);
        volumeStateOutput = new OutputPort<>(volumeStateOutputBuffer);

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
            Long sampleCount = sampleCountInput.consume();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("calculate volumes");
            addNotes(sampleCount);

            VolumeState currentState = futureVolumes.remove(sampleCount);

            if(currentState==null){
                currentState = new VolumeState(new HashMap<>());
            }
            PerformanceTracker.stopTracking(timeKeeper);

            volumeStateOutput.produce(currentState);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void addNotes(Long sampleCount) throws InterruptedException {
        List<Frequency> newNotes = newNoteInput.flush();

        DeterministicEnvelope envelope = new SimpleDeterministicEnvelope(sampleCount, sampleRate, envelopeFunction);

        if(!newNotes.isEmpty()) {
            for (Long i = sampleCount; i < envelope.getEndingSampleCount(); i++) {
                addVolumes(newNotes, envelope, i);
            }
        }
    }

    private void addVolumes(List<Frequency> newNotes, DeterministicEnvelope envelope, Long i) {
        VolumeState newVolumeState = getOldVolumeState(i);

        for (Frequency frequency : newNotes) {
            newVolumeState = newVolumeState.add(frequency, envelope.getVolume(i));
        }

        futureVolumes.put(i, newVolumeState);
    }

    private VolumeState getOldVolumeState(Long i) {
        VolumeState oldVolumeState;

        if (futureVolumes.containsKey(i)) {
            oldVolumeState = futureVolumes.get(i);
        } else {
            oldVolumeState = new VolumeState(new HashMap<>());
        }

        return oldVolumeState;
    }

}
