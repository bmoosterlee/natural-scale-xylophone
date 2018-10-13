package pianola;/*todo write a history tracker of when notes were played. Take average of notes played to form smoothed peaks
 * todo find maxima of these smoothed values. Perform fourier analysis on this signal, also obtaining the phases of
 * todo a frequency. find harmonic image of each frequency, multiply image by the phase of the tonic.
 * todo add all these phase dependent harmonic images together, and display the rhythmic value in real time.
 * todo let the pianola take the rhymthic harmonic value in real time and play that.
 * todo use a lookahead of the length of the shortest frame for the pianola, and play the maximum within that frame*/

import frequency.Frequency;
import gui.spectrum.SpectrumWindow;
import gui.spectrum.state.SpectrumState;
import main.BoundedBuffer;
import main.OutputPort;
import time.Ticker;
import time.TimeInNanoSeconds;

public class Pianola {
    private final PianolaPattern pianolaPattern;

    private OutputPort<Frequency> playedNotes;

    public Pianola(BoundedBuffer<SpectrumState> inputBuffer, SpectrumWindow spectrumWindow, TimeInNanoSeconds frame_time, BoundedBuffer<Frequency> outputBuffer) {

//        pianolaPattern = new Sweep(this, 8, gui.spectrumWindow.getCenterFrequency());
//        pianolaPattern = new SweepToTarget(this, 8, gui.spectrumWindow.getCenterFrequency(), 2.0);
//        pianolaPattern = new SweepToTargetUpDown(this, 8, gui.spectrumWindow.getCenterFrequency(), 2.0);
        pianolaPattern = new SweepToTargetUpDown(inputBuffer, 8, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow);

        playedNotes = new OutputPort<>(outputBuffer);

        Ticker ticker = new Ticker(frame_time);
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
