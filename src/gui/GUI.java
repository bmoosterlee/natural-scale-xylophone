package gui;

import component.Pulse;
import component.buffer.*;
import frequency.Frequency;
import spectrum.SpectrumWindow;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class GUI<N extends Packet<Double[]>, H extends Packet<Double[]>, O extends Packet<java.util.List<Frequency>>> {
    private final int height = 600;
    private final double yScale = height * 0.95;
    private final double margin = height * 0.05;

    private final GUIPanel guiPanel;
    private final InputPort<Map<Integer, Integer>, Packet<Map<Integer, Integer>>> harmonicInputPort;
    private final InputPort<Map<Integer, Integer>, Packet<Map<Integer, Integer>>> noteInputPort;
    private final InputPort<Integer, Packet<Integer>> cursorInputPort;
    private final OutputPort repaintPort;

    public GUI(BoundedBuffer<Double[], N> noteInputBuffer, BoundedBuffer<Double[], H> harmonicInputBuffer, SimpleBuffer<java.util.List<Frequency>, O> outputBuffer, SpectrumWindow spectrumWindow, int inaudibleFrequencyMargin) {
        LinkedList<SimpleBuffer<Double[], ? extends Packet<Double[]>>> harmonicSpectrumBroadcast =
                new LinkedList<>(harmonicInputBuffer.broadcast(3, "GUI - tick broadcast"));

        guiPanel = new GUIPanel();

        harmonicInputPort = harmonicSpectrumBroadcast.poll()
                .performMethod(input -> doubleArrayToMap(spectrumWindow, input), "buckets to volumes - harmonics")
                .performMethod(input2 -> volumesToYs(input2, yScale, margin), "volumes to ys - harmonics").createInputPort();

        noteInputPort = noteInputBuffer
                .performMethod(input -> doubleArrayToMap(spectrumWindow, input), "buckets to volumes - notes")
                .performMethod(input2 -> volumesToYs(input2, yScale, margin), "volumes to ys - notes")
                .createInputPort();

        cursorInputPort = harmonicSpectrumBroadcast.poll()
                .performMethod(input -> {return new Pulse();})
                .connectTo(CursorMover.buildPipe(guiPanel)).createInputPort();

        harmonicSpectrumBroadcast.poll()
            .toOverwritable("gui - note clicker - dump input overflow")
            .performMethod(input -> {return new Pulse();})
            .connectTo(NoteClicker.buildPipe(spectrumWindow, guiPanel))
        .relayTo(outputBuffer);

        JFrame frame = new JFrame("Natural scale xylophone");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        guiPanel.setPreferredSize(new Dimension(spectrumWindow.width, height));
        frame.setContentPane(guiPanel);
        frame.pack();
        frame.setVisible(true);

        repaintPort = new OutputPort<>();

        repaintPort.getBuffer().performMethod((InputCallable<Pulse>) input -> guiPanel.repaint());

    }

    private Map<Integer, Double> doubleArrayToMap(SpectrumWindow spectrumWindow, Double[] input) {
        Map<Integer, Double> volumes = new HashMap<>();
        for(int i = 0; i<spectrumWindow.width; i++) {
            volumes.put(i, input[i]);
        }
        return volumes;
    }

    class GUIPanel extends JPanel {

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            try {
                renderHarmonicsBuckets(g, harmonicInputPort.consume().unwrap());
                renderNoteBuckets(g, noteInputPort.consume().unwrap());
                renderCursorLine(g, cursorInputPort.consume().unwrap());
                repaintPort.produce(new SimplePacket<>(new Pulse()));
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

}
