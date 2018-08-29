import javafx.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.*;

public class GUI extends JPanel implements Runnable, MouseListener, MouseMotionListener {
    private Buckets noteBuckets;
    final BucketHistory bucketHistory = new BucketHistory(100);
    NoteEnvironment noteEnvironment;
    private NoteManager noteManager;
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

    public GUI(NoteEnvironment noteEnvironment, HarmonicCalculator harmonicCalculator){
        this.noteEnvironment = noteEnvironment;
        noteManager = noteEnvironment.noteManager;
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

        noteBuckets = new Buckets();
    }

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);

        NoteFrequencySnapshot noteFrequencySnapshot = noteManager.getSnapshot();
        NoteSnapshot noteSnapshot = noteFrequencySnapshot.noteSnapshot;
        HashSet<Note> liveNotes = noteSnapshot.liveNotes;
        HashMap<Note, Envelope> envelopes = noteSnapshot.envelopes;
        FrequencySnapshot frequencySnapshot = noteFrequencySnapshot.frequencySnapshot;
        Set<Double> liveFrequencies = frequencySnapshot.liveFrequencies;
        Map<Double, Set<Note>> frequencyNoteTable = frequencySnapshot.frequencyNoteTable;

        Map<Note, Double> volumeTable = noteManager.getVolumeTable(noteEnvironment.getExpectedSampleCount(), liveNotes, envelopes);
        Map<Double, Double> frequencyVolumeTable = noteManager.getFrequencyVolumeTable(frequencyNoteTable, volumeTable);

        Buckets newHarmonicsBuckets = getNewHarmonicsBuckets(liveFrequencies, frequencyVolumeTable);
        bucketHistory.addNewBuckets(newHarmonicsBuckets);
        renderHarmonicsBuckets(g, bucketHistory.getTimeAveragedBuckets().averageBuckets(10));

        noteBuckets = getNewNoteBuckets(liveFrequencies, frequencyVolumeTable);
        renderNoteBuckets(g, noteBuckets);

        renderCursorLine(g);
    }

    private Buckets getNewNoteBuckets(Set<Double> liveFrequencies, Map<Double, Double> volumeTable) {
        Set<Pair<Integer, Double>> noteVolumes = new HashSet<>();
        for(Double frequency : liveFrequencies){
            int x = getX(frequency);
            if(x<0 || x>=WIDTH){
                continue;
            }
            noteVolumes.add(new Pair(x, volumeTable.get(frequency)));
        }
        return new Buckets(noteVolumes);
    }

    //todo rename harmonicsBuckets to harmonicBuckets
    private Buckets getNewHarmonicsBuckets(Set<Double> liveFrequencies, Map<Double, Double> frequencyVolumeTable) {
        Iterator<Pair<Harmonic, Double>> harmonicHierarchyIterator =
                harmonicCalculator.getHarmonicHierarchyIterator(liveFrequencies, frequencyVolumeTable, 1000);

        Set<Pair<Integer, Double>> newPairs = new HashSet<>();
        while (getTimeLeftInFrame(startTime) > 1 && harmonicHierarchyIterator.hasNext()) {
            Pair<Harmonic, Double> nextHarmonicVolumePair = harmonicHierarchyIterator.next();
            if(nextHarmonicVolumePair==null){
                break;
            }

            newPairs.add(new Pair<>(getX(nextHarmonicVolumePair.getKey().getFrequency()),
                                    nextHarmonicVolumePair.getValue()));
        }

        return new Buckets(newPairs).clip(0, WIDTH);
    }

    private void renderNoteBuckets(Graphics g, Buckets buckets) {
        g.setColor(Color.blue);

        renderBuckets(g, buckets);
    }

    private void renderBuckets(Graphics g, Buckets buckets) {
        Iterator<Map.Entry<Integer, Double>> iterator = buckets.iterator();
        while(iterator.hasNext()) {
            Map.Entry<Integer, Double> pair = iterator.next();
            int x = pair.getKey();
            int y = (int) (pair.getValue() * yScale + margin);
            g.drawRect(x, HEIGHT - y, 1, y);
        }
    }

    private void renderCursorLine(Graphics g) {
        if(!calculatedMouseFrequency){
            mouseFrequency = getFrequency(mouseX);
            calculatedMouseFrequency = true;
        }
        g.setColor(Color.green);
        int x = getX(mouseFrequency);
        g.drawRect(x, 0, 1, HEIGHT);
    }

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        while(true) {
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
        }
    }

    private void tick(long startTime) {
        this.startTime = startTime;
        repaint();
    }

    private void renderHarmonicsBuckets(Graphics g, Buckets buckets) {
        g.setColor(Color.gray);

        renderBuckets(g, buckets);
    }

    private long getTimeLeftInFrame(long startTime) {
        long currentTime;
        currentTime = System.nanoTime();
        long timePassed = (currentTime - startTime);
        return (FRAME_TIME - timePassed)/ 1000000;
    }

    int getX(double frequency) {
        return (int) (Math.log(frequency) * logFrequencyMultiplier - logFrequencyAdditive);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        noteManager.addNote(getFrequency(e.getX()));
    }

    double getFrequency(double x) {
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

    public Buckets getNoteBuckets() {
        return noteBuckets;
    }

    public void setNoteBuckets(Buckets noteBuckets) {
        this.noteBuckets = noteBuckets;
    }
}
