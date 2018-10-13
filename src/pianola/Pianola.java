package pianola;/*todo write a history tracker of when notes were played. Take average of notes played to form smoothed peaks
 * todo find maxima of these smoothed values. Perform fourier analysis on this signal, also obtaining the phases of
 * todo a frequency. find harmonic image of each frequency, multiply image by the phase of the tonic.
 * todo add all these phase dependent harmonic images together, and display the rhythmic value in real time.
 * todo let the pianola take the rhymthic harmonic value in real time and play that.
 * todo use a lookahead of the length of the shortest frame for the pianola, and play the maximum within that frame*/

import gui.GUI;
import frequency.Frequency;
import gui.spectrum.SpectrumWindow;
import gui.spectrum.state.SpectrumManager;
import gui.spectrum.state.SpectrumState;
import main.BoundedBuffer;
import main.OutputPort;
import sound.SampleTicker;
import time.PerformanceTracker;
import time.Ticker;
import time.TimeInNanoSeconds;
import time.TimeKeeper;
import notes.state.NoteManager;

import java.util.AbstractMap;

public class Pianola {
    final PianolaPattern pianolaPattern;
    SampleTicker sampleTicker;
    NoteManager noteManager;

    private OutputPort<AbstractMap.SimpleImmutableEntry<Long, Frequency>> playedNotes;

    public Pianola(SampleTicker sampleTicker, BoundedBuffer<SpectrumState> inputBuffer, NoteManager noteManager, SpectrumWindow spectrumWindow, TimeInNanoSeconds frame_time, BoundedBuffer<AbstractMap.SimpleImmutableEntry<Long, Frequency>> outputBuffer) {
        this.sampleTicker = sampleTicker;
        this.noteManager = noteManager;

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
                playedNotes.produce(new AbstractMap.SimpleImmutableEntry<>(sampleTicker.getExpectedTickCount(), frequency));
            }
            catch(NullPointerException | InterruptedException ignored){

            }
        }
    }

}
