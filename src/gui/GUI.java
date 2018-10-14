package gui;

import frequency.Frequency;
import gui.buckets.Buckets;
import gui.buckets.BucketsAverager;
import gui.spectrum.SpectrumWindow;
import gui.spectrum.state.SpectrumData;
import gui.spectrum.state.SpectrumState;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import time.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class GUI extends JPanel implements Runnable {
    public final SpectrumWindow spectrumWindow;

    private final int WIDTH;
    private final int HEIGHT = 600;
    private final double yScale = HEIGHT * 0.95;
    private final double margin = HEIGHT * 0.05;

    private final BucketsAverager harmonicsBucketsAverager = new BucketsAverager(10);

    private Frequency mouseFrequency;

    private final InputPort<SpectrumState> newSpectrumState;
    private final OutputPort<Frequency> clickedFrequencies;

    public GUI(BoundedBuffer<SpectrumState> newSpectrumStateBuffer, BoundedBuffer<Frequency> newNotesBuffer){

        WIDTH = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        spectrumWindow = new SpectrumWindow(WIDTH);

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

        start();
    }

    private void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        while(true){
            tick();
        }
    }

    private void tick() {
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

        try {
            SpectrumState spectrumState = newSpectrumState.consume();
            
            timeKeeper = PerformanceTracker.startTracking("render harmonicsBuckets");
            Buckets harmonicsBuckets = spectrumState.harmonicsBuckets.averageBuckets(harmonicsBucketsAverager);
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
