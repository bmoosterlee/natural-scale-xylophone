package gui;

import frequency.Frequency;
import harmonics.HarmonicCalculator;
import main.Observer;
import notes.state.FrequencyManager;
import sound.SampleTicker;
import notes.state.NoteManager;
import time.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.*;

public class GUI extends JPanel {
//    performancetrack everything. Optimize pianola for variable speed, or better performance at higher speed.
//    let the performance lead the way.

//todo fix aliasing with buckets by storing what harmony frequencies were added at which buckets, and their volumes.
//todo then either pick the highest frequency, or find the weighted average frequency when calling getFrequency.
//todo solve rounding issues with translating back and forth from x and frequency. One way is using an averaging
//function over the frequencies, which we then throw into buckets by sampling.
//When we get the maxima in the pianola, we use the function and an algorithm instead of the buckets
    public final SpectrumWindow spectrumWindow;
    private final MouseListener mouseListener;
    private final MouseMotionListener mouseMotionListener;

    public SpectrumSnapshot spectrumSnapshot;
    SampleTicker sampleTicker;
    //TODO do we want to split the entire project into state objects and immutable objects?

    public static final int WIDTH = 800*2;
    public static final int HEIGHT = 600;
    public static final double yScale = HEIGHT * 0.95;
    public static final double margin = HEIGHT * 0.05;
    //todo change use of ticker pattern such that getTimeLeftInFrame isn't used externally anymore
    //todo this could be done by having a tick wrapper which waits for the next tick. Until the next
    //todo tick hasn't arrived yet, we iterate. As soon as the next tick arrives, we finish the last tick.
    //todo we then start the next tick.
    //todo this does not seem like a task for a state object, as we need to keep the tick state
    //todo stored somewhere.

    public Ticker ticker;
    public TimeInNanoSeconds startTime;
    public Frequency mouseFrequency;

    public BucketsAverager harmonicsBucketsAverager = new BucketsAverager(10);

    public GUI(SampleTicker sampleTicker, HarmonicCalculator harmonicCalculator, NoteManager noteManager, FrequencyManager frequencyManager){
        GUI.this.sampleTicker = sampleTicker;

        ticker = new Ticker(new TimeInSeconds(1).toNanoSeconds().divide(60));
        ticker.getTickObservable().add((Observer<Long>) event -> tick());

        spectrumWindow = new SpectrumWindow(frequencyManager, harmonicCalculator);

        mouseListener = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {
                Frequency frequency = spectrumWindow.getFrequency(e.getX());
                clickFrequency(noteManager, frequency);
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
        };
        mouseMotionListener = new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {

            }

            @Override
            public void mouseMoved(MouseEvent e) {
                moveMouse(spectrumWindow.getFrequency(e.getX()));
            }
        };

        JFrame frame = new JFrame("FrameDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        frame.setContentPane(GUI.this);
        frame.addMouseListener(mouseListener);
        frame.addMouseMotionListener(mouseMotionListener);
        frame.pack();
        frame.setVisible(true);

    }

    public void start(){
        ticker.start();
    }

    private void tick() {
        startTime = TimeInNanoSeconds.now();
        repaint();
    }

    private void moveMouse(Frequency newFrequency) {
        mouseFrequency = newFrequency;
    }

    private void clickFrequency(NoteManager noteManager, Frequency frequency) {
        noteManager.addNote(frequency);
    }

    @Override
    public void paintComponent(Graphics g){
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("paintComponent");
        super.paintComponent(g);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("paintComponent 1");

        SpectrumSnapshotBuilder spectrumSnapshotBuilder = spectrumWindow.createBuilder(getSampleTicker().getExpectedTickCount());
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("paintComponent 2");
        while (ticker.getTimeLeftInFrame(startTime).toMilliSeconds().getValue() > 1) {
            if (spectrumSnapshotBuilder.update()) break;
        }
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("paintComponent 3");
        spectrumSnapshot = spectrumSnapshotBuilder.finish();
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("paintComponent 4");
        Buckets harmonicsBuckets = spectrumSnapshot.harmonicsBuckets.averageBuckets(harmonicsBucketsAverager);
        renderNoteBuckets(g, spectrumSnapshot.noteBuckets);

        renderCursorLine(g);
        PerformanceTracker.stopTracking(timeKeeper);
    }

    private void renderNoteBuckets(Graphics g, Buckets buckets) {
        g.setColor(Color.blue);

        renderBuckets(g, buckets);
    }

    private void renderBuckets(Graphics g, Buckets buckets) {
        Iterator<Map.Entry<Integer, Bucket>> iterator = buckets.iterator();
        while(iterator.hasNext()) {
            Map.Entry<Integer, Bucket> pair = iterator.next();
            int x = pair.getKey();
            int y = (int) (pair.getValue().volume * yScale + margin);
            g.drawRect(x, HEIGHT - y, 1, y);
        }
    }

    private void renderCursorLine(Graphics g) {
        g.setColor(Color.green);
        try {
            int x = spectrumWindow.getX(mouseFrequency);
            g.drawRect(x, 0, 1, HEIGHT);
        }
        catch(NullPointerException ignored){

        }
    }

    private void renderHarmonicsBuckets(Graphics g, Buckets buckets) {
        g.setColor(Color.gray);

        renderBuckets(g, buckets);
    }

    public int getX(Frequency frequency) {
        return spectrumWindow.getX(frequency);
    }

    public Frequency getFrequency(double x) {
        return spectrumWindow.getFrequency(x);
    }

    public SampleTicker getSampleTicker() {
        return sampleTicker;
    }

}
