package sound;

import component.buffer.*;
import mixer.state.VolumeAmplitudeState;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class SoundEnvironment extends MethodInputComponent<VolumeAmplitudeState> {

    public SoundEnvironment(SimpleBuffer<VolumeAmplitudeState> inputBuffer, int SAMPLE_SIZE_IN_BITS, SampleRate sampleRate) {
        super(inputBuffer, MethodInputComponent.toMethod(buildPipe(SAMPLE_SIZE_IN_BITS, sampleRate)));
    }

    public static CallableWithArgument<BoundedBuffer<VolumeAmplitudeState>> buildPipe(int SAMPLE_SIZE_IN_BITS, SampleRate sampleRate){
        return new CallableWithArgument<>() {
            private SourceDataLine sourceDataLine;
            private int sampleSize;
            private double marginalSampleSize;

            @Override
            public void call(BoundedBuffer<VolumeAmplitudeState> inputBuffer) {
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

                inputBuffer
                .performMethod(VolumeAmplitudeStateToSignal.build(), "volume amplitude to signal")
                .performMethod(input -> fitAmplitude(input), "fit amplitude")
                .performInputMethod(input -> writeToBuffer(input));
            }

            private byte fitAmplitude(double amplitude) {
                return (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, (byte) Math.floor(sampleSize * amplitude / 2)));
            }

            private void writeToBuffer(byte amplitude) {
                sourceDataLine.write(new byte[]{amplitude}, 0, 1);
            }

//            public boolean isAudible(Double volume) {
//                return volume >= marginalSampleSize;
//            }
//
//            public void close() {
//                sourceDataLine.drain();
//                sourceDataLine.stop();
//            }
        };
    }
}