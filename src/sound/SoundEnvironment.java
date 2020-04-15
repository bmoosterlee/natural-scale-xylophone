package sound;

import component.Flusher;
import component.Pairer;
import component.Pulse;
import component.buffer.*;

import javax.sound.sampled.*;

public class SoundEnvironment {

    private static AudioFormat af;
    private static int sampleSize;
    private static double marginalSampleSize;

    public static InputCallable<BoundedBuffer<Double, SimplePacket<Double>>> buildPipe(int SAMPLE_SIZE_IN_BITS, SampleRate sampleRate, int sampleLookahead) {
        return new InputCallable<>() {
            private SourceDataLine sourceDataLine;


            @Override
            public void call(BoundedBuffer<Double, SimplePacket<Double>> inputBuffer) {
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

                BoundedBuffer<Byte, SimplePacket<Byte>> fittedAmplitudes = inputBuffer
                        .<Byte, SimplePacket<Byte>>performMethod(this::fitAmplitude, 100, "sound environment - fit amplitude");

                SimpleBuffer<Pulse, SimplePacket<Pulse>> batchPulses = new SimpleBuffer<>(1, "sound environment - batch pulse");
                batchPulses
                        .performMethod(Flusher.flushOrConsume(fittedAmplitudes), "sound environment - flush fitted amplitudes")
                        .performMethod(input -> {
                            int size = input.size();
                            byte[] array = new byte[size];
                            for (int i = 0; i < size; i++) {
                                array[i] = input.get(i);
                            }
                            return array;
                        }, "sound environment - byte list to byte array")
                        .performMethod(input -> {
                            writeToBuffer(input);
                            return new Pulse();
                        })
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

    public static void buildComponent(BoundedBuffer<Double[], SimplePacket<Double[]>> volumeInputBuffer, BoundedBuffer<Double[], SimplePacket<Double[]>> amplitudeInputBuffer, int sample_size_in_bits, SampleRate sampleRate, int sampleLookahead) {
        Pairer.pair(
                volumeInputBuffer,
                amplitudeInputBuffer,
                100,
                "sound environment - pair volume and amplitude")
                .<Double, SimplePacket<Double>>performMethod(input -> {
                            double volumeAmplitudes = 0.;
                            for (int i = 0; i < input.getValue().length; i++) {
                                volumeAmplitudes +=
                                        input.getKey()[i] *
                                                input.getValue()[i];
                            }
                            return volumeAmplitudes;
                        },
                        100,
                        "sound environment - merge volume and amplitude to signal")
                .connectTo(buildPipe(sample_size_in_bits, sampleRate, sampleLookahead));
    }

    public static PipeCallable<BoundedBuffer<Long, SimplePacket<Long>> ,BoundedBuffer<Double, SimplePacket<Double>>> buildPipeSource(int SAMPLE_SIZE_IN_BITS, SampleRate sampleRate, int sampleLookahead) {
        return new PipeCallable<>() {

            private TargetDataLine targetDataLine;

            @Override
            public BoundedBuffer<Double, SimplePacket<Double>> call(BoundedBuffer<Long, SimplePacket<Long>> inputBuffer) {

                af = new AudioFormat((float) sampleRate.sampleRate, SAMPLE_SIZE_IN_BITS, 1, true, false);
                sampleSize = (int) (Math.pow(2, SAMPLE_SIZE_IN_BITS) - 1);
                marginalSampleSize = 1. / Math.pow(2, SAMPLE_SIZE_IN_BITS);

                DataLine.Info info = new DataLine.Info(TargetDataLine.class, af);

                try {
                    targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
                    targetDataLine.open(af);
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                }
                targetDataLine.start();

//                return inputBuffer.performMethod(input -> read(), 100, "sound environment - get audio input")
//                        .performMethod(this::buildAmplitude, 100, "sound environment - build amplitude");

                SimpleBuffer<Pulse, SimplePacket<Pulse>> batchPulses = new SimpleBuffer<>(1, "sound environment - batch pulse source");
                SimpleBuffer<Double, SimplePacket<Double>> output = new SimpleBuffer<>(1, "sound environment - source data buffer");
                OutputPort<Double, SimplePacket<Double>> outputPort = output.createOutputPort();
                batchPulses
                        .performMethod(Flusher.flushOrConsume(inputBuffer), "sound environment - flush sample counts")
                        .performMethod(input -> {
                            for(byte sample : read(input.size())){
                                try {
                                    outputPort.produce(new SimplePacket<>(buildAmplitude(sample)));
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            return new Pulse();
                        })
                        .relayTo(batchPulses);

                try {
                    batchPulses.createOutputPort().produce(new SimplePacket<>(new Pulse()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return output;
            }

            private byte read() {
                byte[] ar = new byte[1];
                targetDataLine.read(ar, 0, 1);
                return ar[0];
            }

            private byte[] read(int size) {
                byte[] ar = new byte[size];
                targetDataLine.read(ar, 0, size);
                return ar;
            }

            private double buildAmplitude(byte signal) {
                return signal * 2.0 / sampleSize;
            }

        };
    }
}