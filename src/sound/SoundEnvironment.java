package sound;

import component.buffer.*;
import component.orderer.OrderStampedPacket;
import component.orderer.Orderer;
import mixer.state.VolumeAmplitudeState;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class SoundEnvironment {

    public static InputCallable<BoundedBuffer<VolumeAmplitudeState, OrderStampedPacket<VolumeAmplitudeState>>> buildPipe(int SAMPLE_SIZE_IN_BITS, SampleRate sampleRate){
        return new InputCallable<>() {
            private SourceDataLine sourceDataLine;
            private int sampleSize;
            private double marginalSampleSize;

            @Override
            public void call(BoundedBuffer<VolumeAmplitudeState, OrderStampedPacket<VolumeAmplitudeState>> inputBuffer) {
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
                        .performMethod(VolumeAmplitudeState::toDouble, "volume amplitude to signal")
                        .<Byte, OrderStampedPacket<Byte>>performMethod(this::fitAmplitude, "fit amplitude")
                .connectTo(Orderer.buildPipe())
                .performMethod(((InputCallable<Byte>)
                        this::writeToBuffer)
                .toSequential());
            }

            private byte fitAmplitude(double amplitude) {
                return (byte) (sampleSize * amplitude / 2);
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