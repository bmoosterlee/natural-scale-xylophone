package notes.state;

import main.SampleRate;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class SoundEnvironment {
    final int SAMPLE_SIZE_IN_BITS;
    final SampleRate sampleRate;
    SourceDataLine sourceDataLine;
    final int sampleSize;
    final double marginalSampleSize;

    public SoundEnvironment(int SAMPLE_SIZE_IN_BITS, int SAMPLE_RATE) {
        this.SAMPLE_SIZE_IN_BITS = SAMPLE_SIZE_IN_BITS;
        this.sampleRate = new SampleRate(SAMPLE_RATE);

        sampleSize = (int) (Math.pow(2, SAMPLE_SIZE_IN_BITS) - 1);
        marginalSampleSize = 1. / Math.pow(2, SAMPLE_SIZE_IN_BITS);

        initialize();
    }

    void writeToBuffer(double amplitude) {
        sourceDataLine.write(new byte[]{fitAmplitude(amplitude)}, 0, 1);
    }

    byte fitAmplitude(double amplitude) {
        return (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, (byte) Math.floor(sampleSize * amplitude / 2)));
    }

    public boolean isAudible(Double volume) {
        return volume >= marginalSampleSize;
    }

    void initialize() {
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

    public SampleRate getSampleRate() {
        return sampleRate;
    }

    public void close() {
        sourceDataLine.drain();
        sourceDataLine.stop();
    }
}