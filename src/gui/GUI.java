package gui;

import gui.buckets.Buckets;
import component.BoundedBuffer;
import component.InputPort;
import component.PipeComponent;
import time.PerformanceTracker;
import time.TimeKeeper;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUI extends JPanel implements Runnable {
    private final int HEIGHT = 600;
    private final double yScale = HEIGHT * 0.95;
    private final double margin = HEIGHT * 0.05;

    private final InputPort<Map<Integer, Integer>> newHarmonics;
    private final InputPort<Map<Integer, Integer>> newNotes;
    private final InputPort<Integer> newCursorX;

    private int oldCursorX;

    public GUI(BoundedBuffer<Buckets> newHarmonicsBuffer, BoundedBuffer<Buckets> newNotesBuffer, BoundedBuffer<Integer> newCursorXBuffer, int width){
        int capacity = 10;
        BoundedBuffer<Map<Integer, Double>> harmonicsVolumesOutputBuffer = new BoundedBuffer<>(capacity, "harmonics to volumes");
        new PipeComponent<>(newHarmonicsBuffer, harmonicsVolumesOutputBuffer, GUI::bucketsToVolumes);

        BoundedBuffer<Map<Integer, Double>> noteVolumesOutputBuffer = new BoundedBuffer<>(capacity, "notes to volumes");
        new PipeComponent<>(newNotesBuffer, noteVolumesOutputBuffer, GUI::bucketsToVolumes);

        BoundedBuffer<Map<Integer, Integer>> harmonicsYsOutputBuffer = new BoundedBuffer<>(capacity, "harmonics to ys");
        new PipeComponent<>(harmonicsVolumesOutputBuffer, harmonicsYsOutputBuffer, input -> volumesToYs(input, yScale, margin));

        BoundedBuffer<Map<Integer, Integer>> noteYsOutputBuffer = new BoundedBuffer<>(capacity, "notes to ys");
        new PipeComponent<>(noteVolumesOutputBuffer, noteYsOutputBuffer, input -> volumesToYs(input, yScale, margin));

        newHarmonics = new InputPort<>(harmonicsYsOutputBuffer);
        newNotes = new InputPort<>(noteYsOutputBuffer);
        newCursorX = new InputPort<>(newCursorXBuffer);

        JFrame frame = new JFrame("Natural scale xylophone");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(width, HEIGHT));
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
        try {
            Map<Integer, Integer> harmonics = newHarmonics.consume();
            Map<Integer, Integer> notes = newNotes.consume();
            java.util.List<Integer> newCursorXs = newCursorX.flush();

            TimeKeeper totalTimeKeeper = PerformanceTracker.startTracking("render");
            int x = getCurrentX(newCursorXs);

            super.paintComponent(g);
            renderHarmonicsBuckets(g, harmonics);
            renderNoteBuckets(g, notes);
            renderCursorLine(g, x);
            PerformanceTracker.stopTracking(totalTimeKeeper);

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

    private void renderNoteBuckets(Graphics g, Map<Integer, Integer> ys) {
        g.setColor(Color.blue);

        renderBuckets(g, ys);
    }

    private void renderBuckets(Graphics g, Map<Integer, Integer> ys) {
        for(Integer index : ys.keySet()){
            int y = ys.get(index);
            g.drawRect(index, HEIGHT - y, 1, y);
        }
    }

    private static Map<Integer, Integer> volumesToYs(Map<Integer, Double> volumes, double yScale, double margin) {
        Map<Integer, Integer> yValues = new HashMap<>();
        for(Integer index : volumes.keySet()) {
            int y = (int) (volumes.get(index) * yScale + margin);
            yValues.put(index, y);
        }
        return yValues;
    }

    private static Map<Integer, Double> bucketsToVolumes(Buckets buckets) {
        Map<Integer, Double> volumes = new HashMap<>();
        for(Integer index : buckets.getIndices()) {
            volumes.put(index, buckets.getValue(index).getVolume());
        }
        return volumes;
    }

    private void renderCursorLine(Graphics g, int x) {
        g.setColor(Color.green);

        g.drawRect(x, 0, 1, HEIGHT);
    }

    private void renderHarmonicsBuckets(Graphics g, Map<Integer, Integer> ys) {
        g.setColor(Color.gray);

        renderBuckets(g, ys);
    }
}
