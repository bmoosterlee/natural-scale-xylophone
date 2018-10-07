package gui;

import frequency.Frequency;
import gui.buckets.Buckets;
import gui.buckets.BucketsAverager;
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

public class GUI extends JPanel {
//    performancetrack everything. Optimize pianola for variable speed, or better performance at higher speed.
//    let the performance lead the way.

//todo fix aliasing with buckets by storing what harmony frequencies were added at which buckets, and their volumes.
//todo then either pick the highest frequency, or find the weighted average frequency when calling getFrequency.
//todo solve rounding issues with translating back and forth from x and frequency. One way is using an averaging
//function over the frequencies, which we then throw into buckets by sampling.
//When we get the maxima in the pianola, we use the function and an algorithm instead of the buckets
    public final SpectrumWindow spectrumWindow;

    public SpectrumState spectrumState;
    private SampleTicker sampleTicker;
    //TODO do we want to split the entire project into state objects and immutable objects?

    static final int WIDTH = 800*2;
    private static final int HEIGHT = 600;
    private static final double yScale = HEIGHT * 0.95;
    private static final double margin = HEIGHT * 0.05;
    //todo change use of ticker pattern such that getTimeLeftInFrame isn't used externally anymore
    //todo this could be done by having a tick wrapper which waits for the next tick. Until the next
    //todo tick hasn't arrived yet, we iterate. As soon as the next tick arrives, we finish the last tick.
    //todo we then start the next tick.
    //todo this does not seem like a task for a state object, as we need to keep the tick state
    //todo stored somewhere.

    private Ticker ticker;
    private TimeInNanoSeconds startTime;
    private Frequency mouseFrequency;

    private BucketsAverager harmonicsBucketsAverager = new BucketsAverager(10);

    public GUI(SampleTicker sampleTicker, HarmonicCalculator harmonicCalculator, NoteManager noteManager, FrequencyManager frequencyManager){
        GUI.this.sampleTicker = sampleTicker;

        ticker = new Ticker(new TimeInSeconds(1).toNanoSeconds().divide(60));
        ticker.getTickObservable().add((Observer<Long>) event -> tick());

        spectrumWindow = new SpectrumWindow(frequencyManager, harmonicCalculator);

        MouseListener mouseListener = new MouseListener() {
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
        MouseMotionListener mouseMotionListener = new MouseMotionListener() {
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
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("super paintComponent");
        super.paintComponent(g);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("create spectrum snapshot");
        SpectrumStateBuilder spectrumStateBuilder = spectrumWindow.createBuilder(sampleTicker.getExpectedTickCount());
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("build spectrum snapshot");
        while (ticker.getTimeLeftInFrame(startTime).toMilliSeconds().getValue() > 1) {
            if (spectrumStateBuilder.update()) break;
        }
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("finish building spectrum snapshot");
        spectrumState = spectrumStateBuilder.finish();
        PerformanceTracker.stopTracking(timeKeeper);

        Buckets harmonicsBuckets = spectrumState.harmonicsBuckets.averageBuckets(harmonicsBucketsAverager);

        timeKeeper = PerformanceTracker.startTracking("render harmonicsBuckets");
        renderHarmonicsBuckets(g, harmonicsBuckets);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("render noteBuckets");
        renderNoteBuckets(g, spectrumState.noteBuckets);
        PerformanceTracker.stopTracking(timeKeeper);

        timeKeeper = PerformanceTracker.startTracking("render cursor");
        renderCursorLine(g);
        PerformanceTracker.stopTracking(timeKeeper);
    }

    private void renderNoteBuckets(Graphics g, Buckets buckets) {
        g.setColor(Color.blue);

        renderBuckets(g, buckets);
    }

    private void renderBuckets(Graphics g, Buckets buckets) {
        for(Integer x : buckets.getIndices()){
            int y = (int) (buckets.getValue(x).getVolume() * yScale + margin);
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

}
