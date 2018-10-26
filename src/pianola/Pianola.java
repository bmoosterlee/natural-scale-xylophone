package pianola;/*todo write a history tracker of when notes were played. Take average of notes played to form smoothed peaks
 * todo find maxima of these smoothed values. Perform fourier analysis on this signal, also obtaining the phases of
 * todo a frequency. find harmonic image of each frequency, multiply image by the phase of the tonic.
 * todo add all these phase dependent harmonic images together, and display the rhythmic value in real time.
 * todo let the pianola take the rhythmic harmonic value in real time and play that.
 * todo use a lookahead of the length of the shortest frame for the pianola, and play the maximum within that frame*/

import frequency.Frequency;
import main.BoundedBuffer;
import main.OutputPort;
import pianola.patterns.PianolaPattern;
import time.Ticker;
import time.TimeInNanoSeconds;

public class Pianola {
    private final PianolaPattern pianolaPattern;

    private final OutputPort<Frequency> playedNotes;

    public Pianola(PianolaPattern pianolaPattern, TimeInNanoSeconds frameTime, BoundedBuffer<Frequency> outputBuffer) {
        this.pianolaPattern = pianolaPattern;

        playedNotes = new OutputPort<>(outputBuffer);

        Ticker ticker = new Ticker(frameTime);
        ticker.getTickObservable().add(this::tick);
        ticker.start();
    }
    
    private void tick(long startTime) {
        for(Frequency frequency : pianolaPattern.playPattern()){
            try {
                playedNotes.produce(frequency);
            }
            catch(NullPointerException ignored){

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
