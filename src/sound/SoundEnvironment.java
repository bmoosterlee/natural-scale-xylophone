package sound;

import component.*;
import mixer.state.VolumeAmplitudeState;
import time.PerformanceTracker;
import time.TimeKeeper;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.List;

public class SoundEnvironment extends TickableInputComponent<VolumeAmplitudeState> {

    public SoundEnvironment(BufferInterface<VolumeAmplitudeState> inputBuffer, int SAMPLE_SIZE_IN_BITS, SampleRate sampleRate) {
        super(inputBuffer, build(SAMPLE_SIZE_IN_BITS, sampleRate));
    }

    public static CallableWithArgument<VolumeAmplitudeState> build(int SAMPLE_SIZE_IN_BITS, SampleRate sampleRate){
        return new CallableWithArgument<>() {
            private final SourceDataLine sourceDataLine;
            private final int sampleSize;
            private final double marginalSampleSize;

            private final InputPort<Double> sampleAmplitudeInput;
            private final OutputPort<VolumeAmplitudeState> methodInput;

            {
                sampleSize = (int) (Math.pow(2, SAMPLE_SIZE_IN_BITS) - 1);
                marginalSampleSize = 1. / Math.pow(2, SAMPLE_SIZE_IN_BITS);

                AudioFormat af = new AudioFormat((float) sampleRate.sampleRate, SAMPLE_SIZE_IN_BITS, 1, true, false);
                SourceDataLine newSourceDataLine;
                try {
                    newSourceDataLine = AudioSystem.getSourceDataLine(af);
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                    newSourceDataLine = null;
                }
                sourceDataLine = newSourceDataLine;
                try {
                    sourceDataLine.open();
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                }
                sourceDataLine.start();

                BufferInterface<VolumeAmplitudeState> methodInputBuffer = new BoundedBuffer<>(1, "sound environment - input");
                methodInput = methodInputBuffer.createOutputPort();

                BoundedBuffer<Double> amplitudeBuffer = new BoundedBuffer<>(1, "sound environment - signal");
                new VolumeAmplitudeStateToSignal(methodInputBuffer, amplitudeBuffer);
                sampleAmplitudeInput = new InputPort<>(amplitudeBuffer);
            }

            private void playSound(VolumeAmplitudeState input) {
                try {
                    methodInput.produce(input);
                    Double amplitude = sampleAmplitudeInput.consume();
                    byte fittedAmplitude = fitAmplitude(amplitude);
                    writeToBuffer(fittedAmplitude);
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
                    for (Double amplitude : amplitudes) {
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

//            public boolean isAudible(Double volume) {
//                return volume >= marginalSampleSize;
//            }
//
//            public void close() {
//                sourceDataLine.drain();
//                sourceDataLine.stop();
//            }

            @Override
            public void call(VolumeAmplitudeState input) {
                playSound(input);
            }
        };
    }
}