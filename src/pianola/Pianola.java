package pianola;/*todo write a history tracker of when notes were played. Take average of notes played to form smoothed peaks
 * todo find maxima of these smoothed values. Perform fourier analysis on this signal, also obtaining the phases of
 * todo a frequency. find harmonic image of each frequency, multiply image by the phase of the tonic.
 * todo add all these phase dependent harmonic images together, and display the rhythmic value in real time.
 * todo let the pianola take the rhythmic harmonic value in real time and play that.
 * todo use a lookahead of the length of the shortest frame for the pianola, and play the maximum within that frame*/

import frequency.Frequency;
import gui.buckets.Buckets;
import gui.spectrum.SpectrumWindow;
import main.BoundedBuffer;
import main.OutputPort;
import time.Ticker;
import time.TimeInNanoSeconds;

public class Pianola {
    private final PianolaPattern pianolaPattern;

    private final OutputPort<Frequency> playedNotes;

    public Pianola(SpectrumWindow spectrumWindow, TimeInNanoSeconds frame_time, BoundedBuffer<Buckets> notesBuffer, BoundedBuffer<Buckets> harmonicsBuffer, BoundedBuffer<Frequency> outputBuffer) {

        //        pianolaPattern = new Sweep(this, 8, gui.spectrumWindow.getCenterFrequency());
//        pianolaPattern = new PatternPauser(8, new SweepToTarget(spectrumManager, 5, gui.spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow), 5);
        pianolaPattern = new SweepToTargetUpDown(notesBuffer, harmonicsBuffer, 8, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow);
//        pianolaPattern = new SimpleArpeggio(this, spectrumManager, 4);

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
