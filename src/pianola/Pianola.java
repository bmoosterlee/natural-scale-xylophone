package pianola;/*todo write a history tracker of when notes were played. Take average of notes played to form smoothed peaks
 * todo find maxima of these smoothed values. Perform fourier analysis on this signal, also obtaining the phases of
 * todo a frequency. find harmonic image of each frequency, multiply image by the phase of the tonic.
 * todo add all these phase dependent harmonic images together, and display the rhythmic value in real time.
 * todo let the pianola take the rhythmic harmonic value in real time and play that.
 * todo use a lookahead of the length of the shortest frame for the pianola, and play the maximum within that frame*/

import frequency.Frequency;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import main.Pulse;
import pianola.patterns.PianolaPattern;
import time.TimeInNanoSeconds;

public class Pianola implements Runnable{
    private final PianolaPattern pianolaPattern;

    private final InputPort<Pulse> pulseInput;
    private final OutputPort<Frequency> playedNotes;

    public Pianola(PianolaPattern pianolaPattern, BoundedBuffer<Pulse> inputBuffer, BoundedBuffer<Frequency> outputBuffer) {
        this.pianolaPattern = pianolaPattern;

        pulseInput = new InputPort<>(inputBuffer);
        playedNotes = new OutputPort<>(outputBuffer);

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
            pulseInput.consume();

            for(Frequency frequency : pianolaPattern.playPattern()){
                try {
                    playedNotes.produce(frequency);
                }
                catch(NullPointerException ignored){

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
