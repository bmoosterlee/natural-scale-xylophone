package pianola;/*todo write a history tracker of when notes were played. Take average of notes played to form smoothed peaks
 * todo find maxima of these smoothed values. Perform fourier analysis on this signal, also obtaining the phases of
 * todo a frequency. find harmonic image of each frequency, multiply image by the phase of the tonic.
 * todo add all these phase dependent harmonic images together, and display the rhythmic value in real time.
 * todo let the pianola take the rhymthic harmonic value in real time and play that.
 * todo use a lookahead of the length of the shortest frame for the pianola, and play the maximum within that frame*/

import gui.GUI;
import frequency.Frequency;
import sound.SampleTicker;
import time.PerformanceTracker;
import time.TimeKeeper;
import notes.state.NoteManager;

public class Pianola implements Runnable {
    final PianolaPattern pianolaPattern;
    SampleTicker sampleTicker;
    GUI gui;
    NoteManager noteManager;

    public long startTime;
    public final long FRAME_TIME;

    public Pianola(SampleTicker sampleTicker, GUI gui, NoteManager noteManager, long frame_time) {
        this.sampleTicker = sampleTicker;
        this.gui = gui;
        this.noteManager = noteManager;
        FRAME_TIME = frame_time;

//        pianolaPattern = new SimpleArpeggio(this, 5);
//         pianolaPattern = new Sweep(this, 8, gui.spectrumWindow.getCenterFrequency());
        pianolaPattern = new SweepToTargetUpDown(this, 8, gui.spectrumWindow.getCenterFrequency(), 2.0);
//        pianolaPattern = new SweepToTarget(this, 8, gui.spectrumWindow.getCenterFrequency(), 2.0);
    }

    public void start(){
        new Thread(this).start();
    }
    
    @Override
    public void run() {
        while(true) {
            long startTime = System.nanoTime();
            tick(startTime);

            TimeKeeper sleepTimeKeeper = PerformanceTracker.startTracking("pianola.Pianola sleep");
            long timeLeftInFrame = getTimeLeftInFrame(startTime);
            if (timeLeftInFrame > 0) {
                try {
                    Thread.sleep(timeLeftInFrame);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            PerformanceTracker.stopTracking(sleepTimeKeeper);
        }
    }

    private void tick(long startTime) {
        this.startTime = startTime;

        for(Frequency frequency : pianolaPattern.playPattern()){
            try {
                noteManager.addNote(frequency);
            }
            catch(NullPointerException ignored){

            };
        }
    }

    private long getTimeLeftInFrame(long startTime) {
        long currentTime;
        currentTime = System.nanoTime();
        long timePassed = (currentTime - startTime);
        return (FRAME_TIME - timePassed)/ 1000000;
    }

    public GUI getGui() {
        return gui;
    }
}
