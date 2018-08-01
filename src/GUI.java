import javafx.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashSet;
import java.util.LinkedList;

public class GUI extends JPanel implements Runnable, MouseListener, MouseMotionListener {
    private Buckets harmonicsBuckets;
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

    public static final long FRAME_TIME = 1000000000 / 60;
    public long startTime;
    public int mouseX;
    boolean calculatedMouseFrequency;
    public double mouseFrequency;
    public final int AVERAGING_WIDTH = 10;

    public GUI(NoteEnvironment noteEnvironment, HarmonicCalculator harmonicCalculator){
        this.noteEnvironment = noteEnvironment;
        this.harmonicCalculator = harmonicCalculator;

        lowerBound = centerFrequency/Math.pow(2, octaveRange/2);
        upperBound = centerFrequency*Math.pow(2, octaveRange/2);

        double logLowerBound = Math.log(lowerBound);
        double logUpperBound = Math.log(upperBound);
        double logRange = logUpperBound - logLowerBound;
        logFrequencyMultiplier = WIDTH / logRange;
        logFrequencyAdditive = logLowerBound * WIDTH / logRange;
        xMultiplier = logRange / WIDTH;

        JFrame frame = new JFrame("FrameDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        frame.setContentPane(this);
        frame.addMouseListener(this);
        frame.addMouseMotionListener(this);

        frame.pack();

        frame.setVisible(true);

        harmonicsBuckets = new Buckets(WIDTH);
    }

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);

        HashSet<Note> liveNotes = noteEnvironment.getLiveNotes();

        Buckets newHarmonicsBuckets = decayHarmonicsBuckets(harmonicsBuckets);

        LinkedList<Pair<Harmonic, Double>> harmonicHierarchyAsList =
                harmonicCalculator.getHarmonicHierarchyAsList(noteEnvironment.getExpectedSampleCount(), liveNotes, 1000);

        while (getTimeLeftInFrame(startTime) > 1) {
            Pair<Harmonic, Double> nextHarmonicVolumePair = harmonicHierarchyAsList.poll();
            if(nextHarmonicVolumePair==null){
                break;
            }

            Buckets nextHarmonicsBuckets = createBuckets(nextHarmonicVolumePair, newHarmonicsBuckets.getLength());
            Buckets averagedBuckets = averageBuckets(nextHarmonicsBuckets);
            newHarmonicsBuckets = newHarmonicsBuckets.add(averagedBuckets);
        }

        harmonicsBuckets = newHarmonicsBuckets;
        renderHarmonicsBuckets(g, harmonicsBuckets);

        renderNotes(g, liveNotes);
        renderCursorLine(g);
    }

    private Buckets createBuckets(Pair<Harmonic, Double> pair, int length) {
        Buckets buckets = new Buckets(length);
        addToBucket(buckets, pair.getKey(), pair.getValue());
        return buckets;
    }

    private static Buckets decayHarmonicsBuckets(Buckets harmonicsBuckets) {
        Buckets newHarmonicsBuckets = new Buckets(harmonicsBuckets.getLength());
        for(int x = 0; x<harmonicsBuckets.getLength(); x++) {
            newHarmonicsBuckets.put(x, 0.95 * harmonicsBuckets.getValue(x));
        }
        return newHarmonicsBuckets;
    }

    private void renderNotes(Graphics g, HashSet<Note> liveNotes) {
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("renderNotes");

        g.setColor(Color.blue);
        for(Note note : liveNotes) {
            int x = (int) (Math.log(note.getFrequency()) * logFrequencyMultiplier - logFrequencyAdditive);
            int y = (int)(note.getVolume(noteEnvironment.getExpectedSampleCount()) * yScale + margin);
            g.drawRect(x, HEIGHT-y, 1, y);
        }
        PerformanceTracker.stopTracking(timeKeeper);
    }

    private void renderCursorLine(Graphics g) {
        if(!calculatedMouseFrequency){
            mouseFrequency = getFrequency(mouseX);
            calculatedMouseFrequency = true;
        }
        g.setColor(Color.green);
        int x = (int) (Math.log(mouseFrequency) * logFrequencyMultiplier - logFrequencyAdditive);
        g.drawRect(x, 0, 1, HEIGHT);
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
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("GUI tick");
        this.startTime = startTime;
        repaint();
        PerformanceTracker.stopTracking(timeKeeper);
    }

    private void renderHarmonicsBuckets(Graphics g, Buckets renderBuckets) {
        g.setColor(Color.gray);

        for(int x = 0; x<renderBuckets.getLength(); x++) {
            int y = (int) (renderBuckets.getValue(x) * yScale + margin);
            g.drawRect(x, HEIGHT - y, 1, y);
        }
    }

    private Buckets averageBuckets(Buckets harmonicsBuckets) {
        Buckets averagedBuckets = new Buckets(WIDTH);
        for(int x = 0; x<WIDTH; x++) {
            averagedBuckets.fill(x, harmonicsBuckets.getValue(x));

            for(int i = 1; i< AVERAGING_WIDTH; i++) {
                double value = harmonicsBuckets.getValue(x) * (AVERAGING_WIDTH - i) / AVERAGING_WIDTH;
                try {
                    averagedBuckets.fill(x - i, value);
                } catch (ArrayIndexOutOfBoundsException e) {

                }
                try {
                    averagedBuckets.fill(x + i, value);
                } catch (ArrayIndexOutOfBoundsException e) {

                }
            }
        }
        return averagedBuckets;
    }

    private long getTimeLeftInFrame(long startTime) {
        long currentTime;
        currentTime = System.nanoTime();
        long timePassed = (currentTime - startTime);
        return (FRAME_TIME - timePassed)/ 1000000;
    }

    private void addToBucket(Buckets harmonicsBuckets, Harmonic harmonic, double harmonicVolume) {
        if(harmonic==null){
            return;
        }

        int x = (int) (Math.log(harmonic.getFrequency()) * logFrequencyMultiplier - logFrequencyAdditive);
        if(x<0 || x>=WIDTH){
            return;
        }
        harmonicsBuckets.put(x, harmonicsBuckets.getValue(x) + 0.025 * harmonicVolume);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        noteEnvironment.addNote(getFrequency(e.getX()), noteEnvironment.getExpectedSampleCount());
    }

    private double getFrequency(double x) {
        return Math.exp(x * xMultiplier) * lowerBound;
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

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        calculatedMouseFrequency = false;
    }
}
