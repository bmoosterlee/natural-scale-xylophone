package sound;

import main.BoundedBuffer;
import main.InputPort;
import time.PerformanceTracker;
import time.TimeKeeper;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.LinkedList;
import java.util.List;

public class SoundEnvironment implements Runnable{
    private final SampleRate sampleRate;
    private final SourceDataLine sourceDataLine;
    private final int sampleSize;
    private final double marginalSampleSize;

    private final InputPort<Double> sampleAmplitudeInput;

    public SoundEnvironment(int SAMPLE_SIZE_IN_BITS, int SAMPLE_RATE, BoundedBuffer<Double> inputBuffer) throws LineUnavailableException {
        this.sampleRate = new SampleRate(SAMPLE_RATE);

        sampleSize = (int) (Math.pow(2, SAMPLE_SIZE_IN_BITS) - 1);
        marginalSampleSize = 1. / Math.pow(2, SAMPLE_SIZE_IN_BITS);

        sampleAmplitudeInput = new InputPort<>(inputBuffer);

        AudioFormat af = new AudioFormat((float) sampleRate.sampleRate, SAMPLE_SIZE_IN_BITS, 1, true, false);
        sourceDataLine = AudioSystem.getSourceDataLine(af);
        sourceDataLine.open();
        sourceDataLine.start();

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
            Double amplitude = sampleAmplitudeInput.consume();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("fit amplitude");
            byte fittedAmplitude = fitAmplitude(amplitude);
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("write to buffer");
            writeToBuffer(fittedAmplitude);
            PerformanceTracker.stopTracking(timeKeeper);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void writeToBuffer(byte amplitude) {
        sourceDataLine.write(new byte[]{amplitude}, 0, 1);
    }

    private void tickBatch() {
        try {
            List<Double> amplitudes = sampleAmplitudeInput.flushOrConsume();
            int size = amplitudes.size();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("fit amplitude");
            int i = 0;
            byte[] fittedAmplitudes = new byte[size];
            for(Double amplitude : amplitudes) {
                fittedAmplitudes[i++] = fitAmplitude(amplitude);
            }
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("write to buffer");
            writeToBuffer(fittedAmplitudes, size);
            PerformanceTracker.stopTracking(timeKeeper);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void writeToBuffer(byte[] amplitudes, int size) {
        sourceDataLine.write(amplitudes, 0, size);
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