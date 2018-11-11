package gui;

import component.*;
import frequency.Frequency;
import spectrum.SpectrumWindow;
import spectrum.buckets.Buckets;
import spectrum.buckets.BucketsAverager;
import time.PerformanceTracker;
import time.TimeKeeper;

import javax.swing.*;
import java.awt.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUI extends TickablePipeComponent<SimpleImmutableEntry<Buckets, Buckets>, Frequency> {

    public GUI(BoundedBuffer<SimpleImmutableEntry<Buckets, Buckets>> inputBuffer, BoundedBuffer<Frequency> outputBuffer, SpectrumWindow spectrumWindow, int width, int inaudibleFrequencyMargin){
        super(inputBuffer, outputBuffer, build(spectrumWindow, width, inaudibleFrequencyMargin));
    }

    public static CallableWithArguments<SimpleImmutableEntry<Buckets, Buckets>, Frequency> build(SpectrumWindow spectrumWindow, int width, int inaudibleFrequencyMargin){
        return new CallableWithArguments<>() {
            private final int height = 600;
            private final double yScale = height * 0.95;
            private final double margin = height * 0.05;

            private final GUIPanel guiPanel;

            private final InputPort<Integer> newCursorX;
            private int oldCursorX;

            private OutputPort<SimpleImmutableEntry<Buckets, Buckets>> methodInputPort;
            private InputPort<Frequency> methodOutputPort;

            class GUIPanel extends JPanel {
                private final InputPort<Map<Integer, Integer>> newNotes;
                private final InputPort<Map<Integer, Integer>> newHarmonics;

                GUIPanel(BoundedBuffer<Map<Integer, Integer>> noteSpectrumBuffer, BoundedBuffer<Map<Integer, Integer>> harmonicSpectrumBuffer) {
                    newNotes = new InputPort<>(noteSpectrumBuffer);
                    newHarmonics = new InputPort<>(harmonicSpectrumBuffer);
                }

                @Override
                public void paintComponent(Graphics g) {
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
                    for (Integer index : ys.keySet()) {
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

            {
                int capacity = 100;

                BoundedBuffer<SimpleImmutableEntry<Buckets, Buckets>> methodInputBuffer = new BoundedBuffer<>(1, "gui - method input");
                methodInputPort = methodInputBuffer.createOutputPort();

                BoundedBuffer<SimpleImmutableEntry<Buckets, Buckets>>[] spectrumBroadcast = methodInputBuffer.broadcast(2).toArray(new BoundedBuffer[0]);

                BoundedBuffer<Buckets> noteSpectrumBuffer = new BoundedBuffer<>(1, "gui - note spectrum");
                BoundedBuffer<Buckets> harmonicSpectrumBuffer = new BoundedBuffer<>(1, "gui - harmonic spectrum");
                new Unpairer<>(spectrumBroadcast[1], noteSpectrumBuffer, harmonicSpectrumBuffer);

                BoundedBuffer<Map<Integer, Integer>> harmonicsYsOutputBuffer =
                        harmonicSpectrumBuffer
                                .performMethod(BucketsAverager.average(inaudibleFrequencyMargin))
                                .performMethod(GUI::bucketsToVolumes)
                                .performMethod(input -> volumesToYs(input, yScale, margin));
                BoundedBuffer<Map<Integer, Integer>> noteYsOutputBuffer =
                        noteSpectrumBuffer
                                .performMethod(GUI::bucketsToVolumes)
                                .performMethod(input -> volumesToYs(input, yScale, margin));
                guiPanel = new GUIPanel(noteYsOutputBuffer, harmonicsYsOutputBuffer);

                methodOutputPort =
                    TickableOutputComponent.buildOutputBuffer(NoteClicker.build(spectrumWindow, guiPanel),
                    1,
                    "note clicker - output")
                    .createInputPort();

                BoundedBuffer<Pulse> frameTickBuffer =
                        spectrumBroadcast[0]
                                .performMethod(input -> new Pulse());

                newCursorX =
                    frameTickBuffer
                    .performMethod(CursorMover.build(guiPanel))
                    .createInputPort();

                JFrame frame = new JFrame("Natural scale xylophone");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                guiPanel.setPreferredSize(new Dimension(width, height));
                frame.setContentPane(guiPanel);
                frame.pack();
                frame.setVisible(true);
            }

            private Frequency clickNotes(SimpleImmutableEntry<Buckets, Buckets> input) {
                try {
                    methodInputPort.produce(input);

                    List<Integer> newCursorXs = newCursorX.flush();
                    try {
                        oldCursorX = newCursorXs.get(0);
                    } catch (IndexOutOfBoundsException ignored) {
                    }
                    guiPanel.repaint();

                    return methodOutputPort.consume();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public Frequency call(SimpleImmutableEntry<Buckets, Buckets> input) {
                return clickNotes(input);
            }
        };
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

}
