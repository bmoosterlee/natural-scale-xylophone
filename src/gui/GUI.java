package gui;

import frequency.Frequency;
import gui.buckets.Buckets;
import gui.buckets.BucketsAverager;
import gui.spectrum.state.SpectrumData;
import gui.spectrum.state.SpectrumState;
import gui.spectrum.SpectrumWindow;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import sound.SampleTicker;
import time.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class GUI extends JPanel {
//    performancetrack everything. Optimize pianola for variable speed, or better performance at higher speed.
//    let the performance lead the way.

//todo solve rounding issues with translating back and forth from x and frequency. One way is using an averaging
//function over the frequencies, which we then throw into buckets by sampling.
//When we get the maxima in the pianola, we use the function and an algorithm instead of the buckets
    public final SpectrumWindow spectrumWindow;
    //TODO do we want to split the entire project into state manager objects and immutable objects?

    final int WIDTH;
    private final int HEIGHT = 600;
    private final double yScale = HEIGHT * 0.95;
    private final double margin = HEIGHT * 0.05;
    //todo change use of ticker pattern such that getTimeLeftInFrame isn't used externally anymore
    //todo this could be done by having a tick wrapper which waits for the next tick. Until the next
    //todo tick hasn't arrived yet, we iterate. As soon as the next tick arrives, we finish the last tick.
    //todo we then start the next tick.
    //todo this does not seem like a task for a state object, as we need to keep the tick state
    //todo stored somewhere.
    private final BucketsAverager harmonicsBucketsAverager = new BucketsAverager(10);

    private final Ticker ticker;

    private final SampleTicker sampleTicker;
    private Frequency mouseFrequency;

    private TimeInNanoSeconds frameEndTime;

    private OutputPort<SpectrumData> spectrumInput;
    private InputPort<SpectrumState> newSpectrumState;
    private OutputPort<Frequency> clickedFrequencies;

    public GUI(SampleTicker sampleTicker, BoundedBuffer<SpectrumData> spectrumInputBuffer, BoundedBuffer<SpectrumState> newSpectrumStateBuffer, BoundedBuffer<Frequency> newNotesBuffer){
        this.sampleTicker = sampleTicker;

        WIDTH = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        spectrumWindow = new SpectrumWindow(WIDTH);

        spectrumInput = new OutputPort<>(spectrumInputBuffer);
        newSpectrumState = new InputPort<>(newSpectrumStateBuffer);
        clickedFrequencies = new OutputPort<>(newNotesBuffer);

        MouseListener mouseListener = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {
                try {
                    clickedFrequencies.produce(spectrumWindow.getFrequency(e.getX()));
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
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

        ticker = new Ticker(new TimeInSeconds(1).toNanoSeconds().divide(60));
        ticker.getTickObservable().add(event -> tick());
        ticker.start();
    }

    private void tick() {
        frameEndTime = ticker.getFrameEndTime(TimeInNanoSeconds.now());
        repaint();
    }

    private void moveMouse(Frequency newFrequency) {
        mouseFrequency = newFrequency;
    }

    @Override
    public void paintComponent(Graphics g){
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("super paintComponent");
        super.paintComponent(g);
        PerformanceTracker.stopTracking(timeKeeper);

        long sampleCount = sampleTicker.getExpectedTickCount();

        try {
            spectrumInput.produce(new SpectrumData(frameEndTime));

            SpectrumState spectrumState = newSpectrumState.consume();

            Buckets harmonicsBuckets = spectrumState.harmonicsBuckets.averageBuckets(harmonicsBucketsAverager);

            timeKeeper = PerformanceTracker.startTracking("render harmonicsBuckets");
            renderHarmonicsBuckets(g, harmonicsBuckets);
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("render noteBuckets");
            renderNoteBuckets(g, spectrumState.noteBuckets);
            PerformanceTracker.stopTracking(timeKeeper);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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

}
