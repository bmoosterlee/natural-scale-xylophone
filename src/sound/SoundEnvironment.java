package sound;

import component.Flusher;
import component.Memorizer;
import component.Pairer;
import component.Separator;
import component.buffer.*;
import main.Main;
import spectrum.SpectrumWindow;

import javax.sound.sampled.*;
import java.util.LinkedList;
import java.util.List;

//https://stackoverflow.com/questions/25798200/java-record-mic-to-byte-array-and-play-sound

public class SoundEnvironment {

    private AudioFormat af;
    private int sampleSize;
    private double marginalSampleSize;

    private SourceDataLine sourceDataLine;
    private int audioOutBufferSize;
    //    public static FloatControl volCtrl;

    public SoundEnvironment(SampleRate sampleRate, int SAMPLE_SIZE_IN_BITS){
        af = new AudioFormat((float) sampleRate.sampleRate, SAMPLE_SIZE_IN_BITS, 1, true, false);
        sampleSize = (int) (Math.pow(2, SAMPLE_SIZE_IN_BITS) - 1);
        marginalSampleSize = 1. / Math.pow(2, SAMPLE_SIZE_IN_BITS);
    }

    public MethodInputComponent<byte[], SimplePacket<byte[]>> audioOut(SimpleBuffer<byte[], SimplePacket<byte[]>> inputBuffer) {
        return new MethodInputComponent<>(inputBuffer,
                input -> sourceDataLine.write(input, 0, input.length));
    }

//            public boolean isAudible(Double volume) {
//                return volume >= marginalSampleSize;
//            }
//
//            public void close() {
//                sourceDataLine.drain();
//                sourceDataLine.stop();
//            }

    public byte fitAmplitude(double amplitude) {
        return (byte) (sampleSize * amplitude / 2);
    }

    public BoundedBuffer<byte[], SimplePacket<byte[]>> prepareAudioForMixer(BoundedBuffer<Complex[], SimplePacket<Complex[]>> volumeBuffer, FFTEnvironment fftEnvironment, SampleRate sampleRate, boolean IFFTSynthesis, SpectrumWindow spectrumWindow) {
        try {
            sourceDataLine = AudioSystem.getSourceDataLine(af);
            sourceDataLine.open(af);
            sourceDataLine.start();
            audioOutBufferSize = (2 * sourceDataLine.getBufferSize() / 20) / 2;
        } catch (
                LineUnavailableException e) {
            e.printStackTrace();
        }

        BoundedBuffer<Double, SimplePacket<Double>> rawAmplitudeBuffer;
        if (IFFTSynthesis) {
            rawAmplitudeBuffer = volumeBuffer.connectTo(fftEnvironment.buildAudioOutPipe(sampleRate));
        } else {
            BoundedBuffer<Double[], SimplePacket<Double[]>> amplitudeSpectrumBuffer = new SimpleBuffer<>(100, "sound environment - audio out - oscillator - amplitude from wave");
            MethodOutputComponent<Double[]> component = AmplitudeCalculator.buildPipe(amplitudeSpectrumBuffer, sampleRate, spectrumWindow);
            LinkedList<SimpleBuffer<Double[], SimplePacket<Double[]>>> amplitudeSpectrumBroadcast = new LinkedList<>(amplitudeSpectrumBuffer.broadcast(2, "sound environment - audio out - oscillator - amplitude broadcast"));

            rawAmplitudeBuffer = Pairer.pair(
                    amplitudeSpectrumBroadcast.poll().connectTo(Memorizer.buildPipe(
                            volumeBuffer
                                    .toOverwritable("sound environment - audio out - oscillator - volume overwrite")
                            , "sound environment - audio out - oscillator - memorize volumes"))
                            .performMethod(Main::toMagnitudeSpectrum, "sound environment - audio out - oscillator - to magnitude spectrum")
                            .performMethod(spectrumWindow::toSpectrumWindow, "sound environment - audio out - oscillator - to spectrum window")
                            .performMethod(Main::reduceNoise, "sound environment - audio out - oscillator - reduce noise")
                            .performMethod(Main::normalize, "sound environment - audio out - oscillator - normalize")
                    ,
                    amplitudeSpectrumBroadcast.poll(),
                    100,
                    "sound environment - pair volume and amplitude")
                    .performMethod(input -> {
                                double volumeAmplitudes = 0.;
                                for (int i = 0; i < input.getValue().length; i++) {
                                    volumeAmplitudes +=
                                            input.getKey()[i] *
                                                    input.getValue()[i];
                                }
                                return volumeAmplitudes;
                            },
                            100,
                            "sound environment - merge volume and amplitude to signal");

            new TickRunningStrategy(component);
        }

        return rawAmplitudeBuffer
                .performMethod(this::fitAmplitude, "sound environment - fit amplitude")
                .resize(audioOutBufferSize, "sound environment - raw audio out resize")
                .connectTo((PipeCallable<BoundedBuffer<Byte, Packet<Byte>>, SimpleBuffer<List<Byte>, SimplePacket<List<Byte>>>>) Flusher::flushToList)
                .performMethod(input -> {
                    byte[] array = new byte[input.size()];
                    for (int i = 0; i < array.length; i++) {
                        array[i] = input.get(i);
                    }
                    return array;
                }, "sound environment - audio out to byte array");
    }

    public MethodOutputComponent<byte[]> audioIn(BoundedBuffer<byte[], SimplePacket<byte[]>> outputBuffer) {
        return new MethodOutputComponent<>(outputBuffer,
                new OutputCallable<>() {
                    private TargetDataLine targetDataLine;
                    byte[] data;

                    {
                        try {
                            targetDataLine = AudioSystem.getTargetDataLine(af);
                            targetDataLine.open(af);
                            targetDataLine.start();
                        } catch (LineUnavailableException e) {
                            e.printStackTrace();
                        }

//                        volCtrl = (FloatControl) targetDataLine.getControl(FloatControl.Type.AUX_SEND);
//                        volCtrl.setValue(1f/ 100f);

                        data = new byte[targetDataLine.getBufferSize() / 20];
                    }

                    @Override
                    public byte[] call() {
                        int numBytesRead = targetDataLine.read(data, 0, data.length);
                        byte[] newArray = new byte[numBytesRead];
                        System.arraycopy(data, 0, newArray, 0, numBytesRead);
                        return newArray;
                    }
                });
    }

    public double buildAmplitude(byte signal) {
        return signal * 2.0 / sampleSize;
    }

    public BufferChainLink<Double, SimplePacket<Double>> prepareAudioFromMixer(SimpleBuffer<byte[], SimplePacket<byte[]>> rawAudioInBuffer) {
        return rawAudioInBuffer
                .performMethod(input -> {
                    List<Byte> byteList = new LinkedList<>();
                    for (byte b : input) {
                        byteList.add(b);
                    }
                    return byteList;
                })
                .connectTo(Separator.buildPipe("main - sound environment separate raw audio byte list"))
                .performMethod(this::buildAmplitude, "main - sound environment build amplitude");
    }
}