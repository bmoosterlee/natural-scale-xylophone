import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedList;

public class GUI extends JPanel implements Runnable, MouseListener {
    private final Buckets harmonicsBuckets;
    NoteEnvironment noteEnvironment;
    HarmonicCalculator harmonicCalculator;

    public static final int WIDTH = 800*2;
    public static final int HEIGHT = 600;
    public static final double yScale = HEIGHT * 0.95;
    public static final double margin = HEIGHT * 0.05;
    final double centerFrequency = 2 * 261.63;
    final double octaveRange = 3.;
    final double lowerBound;
    final double upperBound;
    private final double logFrequencyMultiplier;
    private final double logFrequencyAdditive;
    private final double xMultiplier;

    public static final long FRAME_TIME = 1000 / 60;
    public long startTime;

    public GUI(NoteEnvironment noteEnvironment, HarmonicCalculator harmonicCalculator){
        this.noteEnvironment = noteEnvironment;
        this.harmonicCalculator = harmonicCalculator;

        lowerBound = centerFrequency/octaveRange;
        upperBound = centerFrequency*octaveRange;

        double logLowerBound = Math.log(lowerBound);
        double logUpperBound = Math.log(upperBound);
        logFrequencyMultiplier = WIDTH / (logUpperBound - logLowerBound);
        logFrequencyAdditive = logLowerBound * WIDTH / (logUpperBound - logLowerBound);
        xMultiplier = (logUpperBound - logLowerBound) / WIDTH;

        JFrame frame = new JFrame("FrameDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        frame.setContentPane(this);
        frame.addMouseListener(this);

        frame.pack();

        frame.setVisible(true);

        harmonicsBuckets = new Buckets(WIDTH);
    }

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);

        TimeKeeper timeKeeper = PerformanceTracker.startTracking("GUI tick");
        g.clearRect(0, 0, WIDTH, HEIGHT);
        renderHarmonicsBuckets(g);
        renderNotes(g);

        addHarmonicsToBuckets(startTime);
        PerformanceTracker.stopTracking(timeKeeper);
    }

    private void renderNotes(Graphics g) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("renderNotes");
        LinkedList<Note> liveNotes = (LinkedList<Note>) noteEnvironment.getLiveNotes().clone();

        g.setColor(Color.blue);
        for(Note note : liveNotes) {
            int x = (int) (Math.log(note.getFrequency()) * logFrequencyMultiplier - logFrequencyAdditive);
            int y = (int)(noteEnvironment.getVolume(note, noteEnvironment.getExpectedSampleCount()) * yScale + margin);
            g.drawRect(x, HEIGHT-y, 1, y);
        }
        PerformanceTracker.stopTracking(timeKeeper);
    }

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        while(true) {
            TimeKeeper timeKeeper = PerformanceTracker.startTracking("GUI loop");
            long startTime = System.nanoTime();
            tick(startTime);

            TimeKeeper sleepTimeKeeper = PerformanceTracker.startTracking("GUI sleep");
            long timeLeftInFrame = getTimeLeftInFrame(startTime);
            if (timeLeftInFrame > 0) {
                try {
                    Thread.sleep(timeLeftInFrame);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            PerformanceTracker.stopTracking(sleepTimeKeeper);

            PerformanceTracker.stopTracking(timeKeeper);
        }
    }

    private void tick(long startTime) {
        this.startTime = startTime;
        repaint();
    }

    private void addHarmonicsToBuckets(long startTime) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("addHarmonicsToBuckets");
        harmonicsBuckets.clear();
        long sampleCountAtFrame = noteEnvironment.getExpectedSampleCount();

        while (getTimeLeftInFrame(startTime) > 1) {
            Harmonic harmonic = harmonicCalculator.getNextHarmonic(sampleCountAtFrame);
            addToBucket(harmonic);
        }
        PerformanceTracker.stopTracking(timeKeeper);
    }

    private void renderHarmonicsBuckets(Graphics g) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("renderHarmonicsBuckets");
        g.setColor(Color.gray);

        for(int x = 0; x<WIDTH; x++) {
            int y = (int)(harmonicsBuckets.getValue(x) * yScale + margin);
            g.drawRect(x, HEIGHT - y, 1, y);
        }

        PerformanceTracker.stopTracking(timeKeeper);
    }

    private long getTimeLeftInFrame(long startTime) {
        long currentTime;
        currentTime = System.nanoTime();
        long timePassed = (currentTime - startTime) / 1000000;
        return FRAME_TIME - timePassed;
    }

    private void addToBucket(Harmonic harmonic) {
        if(harmonic==null){
            return;
        }

        int x = (int) (Math.log(harmonic.getFrequency()) * logFrequencyMultiplier - logFrequencyAdditive);
        if(x<0 || x>=WIDTH){
            return;
        }
//        double sonanceValue = harmonic.getSonanceValue(sampleCountAtFrame)/harmonic.tonic.getVolume(sampleCountAtFrame) * 0.5*100000./(100000.+(sampleCountAtFrame- harmonic.tonic.getStartingSampleCount()));
        double sonanceValue = harmonic.getSonanceValue();
        harmonicsBuckets.fill(x, sonanceValue);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        double frequency = Math.exp((double) e.getX() * xMultiplier) * lowerBound;
        noteEnvironment.addNote(new Note(frequency, noteEnvironment.getExpectedSampleCount()));
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
