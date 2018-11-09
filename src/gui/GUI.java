package gui;

import component.*;
import component.Tickable;
import frequency.Frequency;
import spectrum.buckets.Buckets;
import spectrum.buckets.BucketsAverager;
import spectrum.SpectrumWindow;
import component.Pulse;
import time.PerformanceTracker;
import time.TimeKeeper;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class GUI extends Tickable {

    private final int height = 600;
    private final double yScale = height * 0.95;
    private final double margin = height * 0.05;

    private final GUIPanel guiPanel;

    private final InputPort<Integer> newCursorX;

    private int oldCursorX;

    public GUI(BoundedBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> inputBuffer, BoundedBuffer<Frequency> outputBuffer, SpectrumWindow spectrumWindow, int width, int inaudibleFrequencyMargin){
        int capacity = 100;

        BoundedBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> tickSpectrumBuffer = new BoundedBuffer<>(1, "gui - spectrum 1");
        BoundedBuffer<AbstractMap.SimpleImmutableEntry<Buckets, Buckets>> spectrumBuffer = new BoundedBuffer<>(1, "gui - spectrum 2");
        new Broadcast<>(inputBuffer, Arrays.asList(tickSpectrumBuffer, spectrumBuffer));

        BoundedBuffer<Pulse> frameTickBuffer = new BoundedBuffer<>(1, "gui - frame tick");
        new TickablePipeComponent<>(tickSpectrumBuffer, frameTickBuffer, input -> new Pulse());

        BoundedBuffer<Buckets> noteSpectrumBuffer = new BoundedBuffer<>(1, "gui - note spectrum");
        BoundedBuffer<Buckets> harmonicSpectrumBuffer = new BoundedBuffer<>(1, "gui - harmonic spectrum");
        new Unpairer<>(spectrumBuffer, noteSpectrumBuffer, harmonicSpectrumBuffer);

        BoundedBuffer<Buckets> guiAveragedHarmonicsBucketsBuffer = new BoundedBuffer<>(capacity, "Gui averaged harmonics buffer");
        new BucketsAverager(inaudibleFrequencyMargin, harmonicSpectrumBuffer, guiAveragedHarmonicsBucketsBuffer);

        BoundedBuffer<Map<Integer, Double>> harmonicsVolumesOutputBuffer = new BoundedBuffer<>(capacity, "harmonics to volumes");
        new TickablePipeComponent<>(guiAveragedHarmonicsBucketsBuffer, harmonicsVolumesOutputBuffer, GUI::bucketsToVolumes);

        BoundedBuffer<Map<Integer, Double>> noteVolumesOutputBuffer = new BoundedBuffer<>(capacity, "notes to volumes");
        new TickablePipeComponent<>(noteSpectrumBuffer, noteVolumesOutputBuffer, GUI::bucketsToVolumes);

        BoundedBuffer<Map<Integer, Integer>> harmonicsYsOutputBuffer = new BoundedBuffer<>(capacity, "harmonics to ys");
        new TickablePipeComponent<>(harmonicsVolumesOutputBuffer, harmonicsYsOutputBuffer, input -> volumesToYs(input, yScale, margin));

        BoundedBuffer<Map<Integer, Integer>> noteYsOutputBuffer = new BoundedBuffer<>(capacity, "notes to ys");
        new TickablePipeComponent<>(noteVolumesOutputBuffer, noteYsOutputBuffer, input -> volumesToYs(input, yScale, margin));

        guiPanel = new GUIPanel(noteYsOutputBuffer, harmonicsYsOutputBuffer);
        guiPanel.addMouseListener(new NoteClicker(outputBuffer, spectrumWindow));
        BoundedBuffer<Integer> cursorXBuffer = new OverwritableBuffer<>(capacity);
        guiPanel.addMouseMotionListener(new CursorMover(frameTickBuffer, cursorXBuffer));

        newCursorX = cursorXBuffer.createInputPort();

        JFrame frame = new JFrame("Natural scale xylophone");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        guiPanel.setPreferredSize(new Dimension(width, height));
        frame.setContentPane(guiPanel);
        frame.pack();
        frame.setVisible(true);

        start();
    }

    @Override
    protected void tick() {
        try {
            List<Integer> newCursorXs = newCursorX.flush();
            try {
                oldCursorX = newCursorXs.get(0);
            }
            catch(IndexOutOfBoundsException ignored){
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        guiPanel.repaint();
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

    private class GUIPanel extends JPanel {
        private final InputPort<Map<Integer, Integer>> newNotes;
        private final InputPort<Map<Integer, Integer>> newHarmonics;

        GUIPanel(BoundedBuffer<Map<Integer, Integer>> noteYsOutputBuffer, BoundedBuffer<Map<Integer, Integer>> harmonicsYsOutputBuffer){
            newNotes = new InputPort<>(noteYsOutputBuffer);
            newHarmonics = new InputPort<>(harmonicsYsOutputBuffer);
        }

        @Override
        public void paintComponent(Graphics g){
            try {
                Map<Integer, Integer> notes = newNotes.consume();
                Map<Integer, Integer> harmonics = newHarmonics.consume();

                TimeKeeper totalTimeKeeper = PerformanceTracker.startTracking("render");
                super.paintComponent(g);
                renderHarmonicsBuckets(g, harmonics);
                renderNoteBuckets(g, notes);
                renderCursorLine(g, oldCursorX);
                PerformanceTracker.stopTracking(totalTimeKeeper);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void renderBuckets(Graphics g, Map<Integer, Integer> ys) {
            for(Integer index : ys.keySet()){
                int y = ys.get(index);
                g.drawRect(index, height - y, 1, y);
            }
        }

        private void renderHarmonicsBuckets(Graphics g, Map<Integer, Integer> ys) {
            g.setColor(Color.gray);

            renderBuckets(g, ys);
        }

        private void renderNoteBuckets(Graphics g, Map<Integer, Integer> ys) {
            g.setColor(Color.blue);

            renderBuckets(g, ys);
        }

        private void renderCursorLine(Graphics g, int x) {
            g.setColor(Color.green);

            g.drawRect(x, 0, 1, height);
        }
    }
}
