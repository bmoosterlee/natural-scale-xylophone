package wave.state;

import frequency.state.FrequencyManager;
import frequency.state.FrequencyState;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import sound.SampleRate;

public class WaveManager implements Runnable {
    private final FrequencyManager frequencyManager;

    private WaveState waveState;
    private long updatedToSample = -1;

    private InputPort<Long> sampleCountInput;
    private OutputPort<WaveState> waveStateOutput;

    public WaveManager(FrequencyManager frequencyManager, SampleRate sampleRate, BoundedBuffer<Long> inputBuffer, BoundedBuffer<WaveState> outputBuffer) {
        this.frequencyManager = frequencyManager;

        sampleCountInput = new InputPort<>(inputBuffer);
        waveStateOutput = new OutputPort<>(outputBuffer);

        waveState = new WaveState(sampleRate);

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
            Long consume = sampleCountInput.consume();

            WaveState newWaveState = getWaveState(consume);

            waveStateOutput.produce(newWaveState);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private WaveState getWaveState(long sampleCount) {
        if(sampleCount>updatedToSample) {
            FrequencyState frequencyState = frequencyManager.getFrequencyState(sampleCount);
            waveState = waveState.update(frequencyState.getFrequencies(), frequencyState.buckets);
            waveState = waveState.update(sampleCount);
            updatedToSample = sampleCount;
        }
        return waveState;
    }
}