package sound;

import main.BoundedBuffer;
import main.InputPort;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class SoundEnvironment implements Runnable{
    private final int SAMPLE_SIZE_IN_BITS;
    private final SampleRate sampleRate;
    private SourceDataLine sourceDataLine;
    private final int sampleSize;
    private final double marginalSampleSize;

    private final InputPort<Double> sampleAmplitudeInput;

    public SoundEnvironment(int SAMPLE_SIZE_IN_BITS, int SAMPLE_RATE, BoundedBuffer<Double> inputBuffer) {
        this.SAMPLE_SIZE_IN_BITS = SAMPLE_SIZE_IN_BITS;
        this.sampleRate = new SampleRate(SAMPLE_RATE);

        sampleSize = (int) (Math.pow(2, SAMPLE_SIZE_IN_BITS) - 1);
        marginalSampleSize = 1. / Math.pow(2, SAMPLE_SIZE_IN_BITS);

        sampleAmplitudeInput = new InputPort<>(inputBuffer);

        initialize();

        start();
    }

    private void initialize() {
        AudioFormat af = new AudioFormat((float) sampleRate.sampleRate, SAMPLE_SIZE_IN_BITS, 1, true, false);
        sourceDataLine = null;
        try {
            sourceDataLine = AudioSystem.getSourceDataLine(af);
            sourceDataLine.open();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        sourceDataLine.start();
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
            Double amplitude = sampleAmplitudeInput.consume();
            writeToBuffer(amplitude);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void writeToBuffer(double amplitude) {
        sourceDataLine.write(new byte[]{fitAmplitude(amplitude)}, 0, 1);
    }

    private byte fitAmplitude(double amplitude) {
        return (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, (byte) Math.floor(sampleSize * amplitude / 2)));
    }

    public boolean isAudible(Double volume) {
        return volume >= marginalSampleSize;
    }

    public SampleRate getSampleRate() {
        return sampleRate;
    }

    public void close() {
        sourceDataLine.drain();
        sourceDataLine.stop();
    }
}