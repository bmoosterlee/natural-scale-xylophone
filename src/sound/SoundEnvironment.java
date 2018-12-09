package sound;

import component.Flusher;
import component.Pulse;
import component.buffer.*;
import component.orderer.OrderStampedPacket;
import component.orderer.Orderer;
import frequency.Frequency;
import main.OrderStampedPacketPairer;
import mixer.state.AmplitudeState;
import mixer.state.VolumeAmplitude;
import mixer.state.VolumeAmplitudeState;
import mixer.state.VolumeState;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.List;
import java.util.Map;

public class SoundEnvironment {

    public static InputCallable<BoundedBuffer<Map<Frequency, VolumeAmplitude>, OrderStampedPacket<Map<Frequency, VolumeAmplitude>>>> buildPipe(int SAMPLE_SIZE_IN_BITS, SampleRate sampleRate, int sampleLookahead){
        return new InputCallable<>() {
            private SourceDataLine sourceDataLine;
            private int sampleSize;
            private double marginalSampleSize;

            @Override
            public void call(BoundedBuffer<Map<Frequency, VolumeAmplitude>, OrderStampedPacket<Map<Frequency, VolumeAmplitude>>> inputBuffer) {
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

                BoundedBuffer<Byte, OrderStampedPacket<Byte>> fittedAmplitudes = inputBuffer
                        .performMethod(VolumeAmplitudeState::toDouble, 100, "sound environment - volume amplitude to signal")
                        .<Byte, OrderStampedPacket<Byte>>performMethod(this::fitAmplitude, 100, "sound environment - fit amplitude")
                        .connectTo(Orderer.buildPipe(100, "sound environment - sample orderer"))
                        .resize(Math.max(1, sampleLookahead));

                SimpleBuffer<Pulse, SimplePacket<Pulse>> batchPulses = new SimpleBuffer<>(1, "sound environment - batch pulse");
                batchPulses
                        .performMethod(Flusher.flushOrConsume(fittedAmplitudes), "sound environment - flush fitted amplitudes")
                        .performMethod(((PipeCallable<List<Byte>, byte[]>) input -> {
                            int size = input.size();
                            byte[] array = new byte[size];
                            for(int i = 0; i<size; i++){
                                array[i] = input.get(i);
                            }
                            return array;
                        }).toSequential(), "sound environment - byte list to byte array")
                        .performMethod(((PipeCallable<byte[], Pulse>) input -> {
                            writeToBuffer(input);
                            return new Pulse();
                        }).toSequential())
                        .relayTo(batchPulses);

                try {
                    batchPulses.createOutputPort().produce(new SimplePacket<>(new Pulse()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            private byte fitAmplitude(double amplitude) {
                return (byte) (sampleSize * amplitude / 2);
            }

            private void writeToBuffer(byte amplitude) {
                byte[] ar = {amplitude};
                writeToBuffer(ar);
            }

            private void writeToBuffer(byte[] ar) {
                sourceDataLine.write(ar, 0, ar.length);
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

    public static void buildComponent(SimpleBuffer<VolumeState, OrderStampedPacket<VolumeState>> volumeInputBuffer, BoundedBuffer<AmplitudeState, OrderStampedPacket<AmplitudeState>> amplitudeInputBuffer, int sample_size_in_bits, SampleRate sampleRate, int sampleLookahead) {
        OrderStampedPacketPairer.buildComponent(
                volumeInputBuffer,
                amplitudeInputBuffer, "sound environment - pair volume and amplitude")
                .toOverwritable()
                        .<Map<Frequency, VolumeAmplitude>, OrderStampedPacket<Map<Frequency, VolumeAmplitude>>>
                        performMethod(input ->
                        VolumeAmplitudeState.build(
                                input.getKey(),
                                input.getValue()),
                        100,
                        "sound environment - merge volume and amplitude state")
                .connectTo(buildPipe(sample_size_in_bits, sampleRate, sampleLookahead));
    }
}