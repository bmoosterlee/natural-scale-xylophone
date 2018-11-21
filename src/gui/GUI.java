package gui;

import component.Pulse;
import component.buffer.*;
import frequency.Frequency;
import spectrum.SpectrumWindow;
import spectrum.buckets.Buckets;
import spectrum.buckets.BucketsAverager;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class GUI {
    private final int height = 600;
    private final double yScale = height * 0.95;
    private final double margin = height * 0.05;

    private final InputPort<Map<Integer, Integer>> newNotesPort;
    private final InputPort<Map<Integer, Integer>> newHarmonicsPort;
    private final InputPort<Integer> newCursorXPort;

    private final GUIPanel guiPanel;

    public GUI(SimpleBuffer<Buckets> noteInputBuffer, SimpleBuffer<Buckets> harmonicInputBuffer, SimpleBuffer<java.util.List<Frequency>> outputBuffer, SpectrumWindow spectrumWindow, int width, int inaudibleFrequencyMargin) {
        LinkedList<SimpleBuffer<Buckets>> noteSpectrumBroadcast =
                new LinkedList<>(noteInputBuffer.broadcast(3, "GUI note spectrum - broadcast"));

        newHarmonicsPort =
                harmonicInputBuffer
                        .connectTo(BucketsAverager.buildPipe(inaudibleFrequencyMargin))
                        .performMethod(GUI::bucketsToVolumes, "buckets to volumes - harmonics")
                        .performMethod(input2 -> volumesToYs(input2, yScale, margin), "volumes to ys - harmonics")
                        .createInputPort();
        newNotesPort =
                noteSpectrumBroadcast.poll()
                        .performMethod(GUI::bucketsToVolumes, "buckets to volumes - notes")
                        .performMethod(input2 -> volumesToYs(input2, yScale, margin), "volumes to ys - notes")
                        .resize(20)
                        .createInputPort();

        guiPanel = new GUIPanel();

        newCursorXPort =
                noteSpectrumBroadcast.poll()
                .performMethod(input1 -> new Pulse(), "harmonics - spectrum to pulse")
                .connectTo(CursorMover.buildPipe(guiPanel)).resize(20).createInputPort();

        noteSpectrumBroadcast.poll()
            .performMethod(input -> new Pulse(), "note - spectrum to pulse")
            .connectTo(NoteClicker.buildPipe(spectrumWindow, guiPanel))
        .relayTo(outputBuffer);

        JFrame frame = new JFrame("Natural scale xylophone");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        guiPanel.setPreferredSize(new Dimension(width, height));
        frame.setContentPane(guiPanel);
        frame.pack();
        frame.setVisible(true);

        new SimpleTickRunner(new AbstractComponent() {
            @Override
            protected void tick() {
                guiPanel.repaint();
            }

            @Override
            protected Collection<InputPort> getInputPorts() {
                return Arrays.asList(newHarmonicsPort, newNotesPort, newCursorXPort);
            }

            @Override
            protected Collection<OutputPort> getOutputPorts() {
                return Collections.emptyList();
            }
        }).start();
    }

    class GUIPanel extends JPanel {

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            try {
                Map<Integer, Integer> harmonics = newHarmonicsPort.consume();
                renderHarmonicsBuckets(g, harmonics);

                Map<Integer, Integer> notes = newNotesPort.consume();
                renderNoteBuckets(g, notes);

                Integer cursorX = newCursorXPort.consume();
                renderCursorLine(g, cursorX);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void renderBuckets(Graphics g, Map<Integer, Integer> ys) {
        for (Integer index : ys.keySet()) {
            int y = ys.get(index);
            g.drawLine(index, height, index, height-y);
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

        g.drawLine(x, 0, x, height);
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
