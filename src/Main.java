import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Main {

    public static void main(String[] args){

        byte[] clipBuffer = new byte[ 1 ];
        int sampleRate = 44100;
        double frequency = 440.;

        AudioFormat af = new AudioFormat( (float ) sampleRate, 8, 1, true, false );
        SourceDataLine sdl = null;
        try {
            sdl = AudioSystem.getSourceDataLine( af );
            sdl.open();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        sdl.start();
        int i = 0;
        while(true){
            double amplitude = 100. * 1000./(1000.+i);
            double angle = i / ( (float ) sampleRate / frequency) * 2.0 * Math.PI;
            clipBuffer[ 0 ] = (byte )( Math.sin( angle ) * amplitude );
            sdl.write( clipBuffer, 0, 1 );
            i++;
        }
//        sdl.drain();
//        sdl.stop();

    }

}
