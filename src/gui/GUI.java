package gui;

import gui.buckets.Buckets;
import gui.buckets.BucketsAverager;
import gui.spectrum.SpectrumWindow;
import gui.spectrum.state.SpectrumState;
import main.BoundedBuffer;
import main.InputPort;
import time.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GUI extends JPanel implements Runnable {
    public final SpectrumWindow spectrumWindow;

    private final int WIDTH;
    private final int HEIGHT = 600;
    private final double yScale = HEIGHT * 0.95;
    private final double margin = HEIGHT * 0.05;

    private final BucketsAverager harmonicsBucketsAverager = new BucketsAverager(10);

    private final InputPort<SpectrumState> newSpectrumState;
    private final InputPort<Integer> newCursorX;

    private int oldCursorX;

    public GUI(BoundedBuffer<SpectrumState> newSpectrumStateBuffer, BoundedBuffer<Integer> newCursorXBuffer){

        WIDTH = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        spectrumWindow = new SpectrumWindow(WIDTH);

        newSpectrumState = new InputPort<>(newSpectrumStateBuffer);
        newCursorX = new InputPort<>(newCursorXBuffer);

        JFrame frame = new JFrame("FrameDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        frame.setContentPane(this);
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

    @Override
    public void paintComponent(Graphics g){
        TimeKeeper timeKeeper = PerformanceTracker.startTracking("super paintComponent");
        super.paintComponent(g);
        PerformanceTracker.stopTracking(timeKeeper);

        try {
            SpectrumState spectrumState = newSpectrumState.consume();
            java.util.List<Integer> newCursorXs = newCursorX.flush();

            int x = getCurrentX(newCursorXs);

            timeKeeper = PerformanceTracker.startTracking("average harmonicsBuckets");
            Buckets harmonicsBuckets = spectrumState.harmonicsBuckets.averageBuckets(harmonicsBucketsAverager);
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("render harmonicsBuckets");
            renderHarmonicsBuckets(g, harmonicsBuckets);
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("render noteBuckets");
            renderNoteBuckets(g, spectrumState.noteBuckets);
            PerformanceTracker.stopTracking(timeKeeper);

            timeKeeper = PerformanceTracker.startTracking("render cursor");
            renderCursorLine(g, x);
            PerformanceTracker.stopTracking(timeKeeper);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private int getCurrentX(List<Integer> newCursorXs) {
        int x;
        try {
            x = newCursorXs.get(0);
            oldCursorX = x;
        }
        catch(IndexOutOfBoundsException ignored){
            x = oldCursorX;
        }
        return x;
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

    private void renderCursorLine(Graphics g, int x) {
        g.setColor(Color.green);

        g.drawRect(x, 0, 1, HEIGHT);
    }

    private void renderHarmonicsBuckets(Graphics g, Buckets buckets) {
        g.setColor(Color.gray);

        renderBuckets(g, buckets);
    }
}
