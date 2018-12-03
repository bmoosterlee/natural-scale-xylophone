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

public class GUI<N extends Packet<Buckets>, H extends Packet<Buckets>, O extends Packet<java.util.List<Frequency>>> {
    private final int height = 600;
    private final double yScale = height * 0.95;
    private final double margin = height * 0.05;

    private final GUIPanel guiPanel;
    private final OutputPort<Graphics, SimplePacket<Graphics>> graphicsPort;

    public GUI(SimpleBuffer<Buckets, N> noteInputBuffer, SimpleBuffer<Buckets, H> harmonicInputBuffer, SimpleBuffer<java.util.List<Frequency>, O> outputBuffer, SpectrumWindow spectrumWindow, int inaudibleFrequencyMargin) {
        LinkedList<SimpleBuffer<Buckets, N>> noteSpectrumBroadcast =
                new LinkedList<>(noteInputBuffer.broadcast(3, "GUI note spectrum - broadcast"));

        guiPanel = new GUIPanel();

        graphicsPort = new OutputPort<>();

        graphicsPort.getBuffer()
        .pairWith(
            harmonicInputBuffer
            .connectTo(BucketsAverager.buildPipe(inaudibleFrequencyMargin))
            .performMethod(GUI::bucketsToVolumes, "buckets to volumes - harmonics")
            .performMethod(input2 -> volumesToYs(input2, yScale, margin), "volumes to ys - harmonics"))
        .performMethod(input -> {renderHarmonicsBuckets(input.getKey(), input.getValue()); return input.getKey();})
        .pairWith(
                noteSpectrumBroadcast.poll()
                .performMethod(GUI::bucketsToVolumes, "buckets to volumes - notes")
                .performMethod(input2 -> volumesToYs(input2, yScale, margin), "volumes to ys - notes")
                .resize(20))
        .performMethod(input -> {renderNoteBuckets(input.getKey(), input.getValue()); return input.getKey();})
        .pairWith(
                noteSpectrumBroadcast.poll()
                .performMethod(input1 -> new Pulse(), "harmonics - spectrum to pulse")
                .connectTo(CursorMover.buildPipe(guiPanel)).resize(20))
        .performMethod(input -> {renderCursorLine(input.getKey(), input.getValue()); return input.getKey();});

        noteSpectrumBroadcast.poll()
            .performMethod(input -> new Pulse(), "note - spectrum to pulse")
            .connectTo(NoteClicker.buildPipe(spectrumWindow, guiPanel))
        .relayTo(outputBuffer);

        JFrame frame = new JFrame("Natural scale xylophone");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        guiPanel.setPreferredSize(new Dimension(spectrumWindow.width, height));
        frame.setContentPane(guiPanel);
        frame.pack();
        frame.setVisible(true);

        new SimpleTickRunner(new AbstractComponent() {
            @Override
            protected void tick() {
                guiPanel.repaint();
            }

            @Override
            protected Collection<BoundedBuffer> getInputBuffers() {
                return Collections.singleton(graphicsPort.getBuffer());
            }

            @Override
            protected Collection<BoundedBuffer> getOutputBuffers() {
                return Collections.emptyList();
            }

            @Override
            public Boolean isParallelisable(){
                return false;
            }
        }).start();
    }

    class GUIPanel extends JPanel {

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            try {
                graphicsPort.produce(new SimplePacket<>(g));
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
