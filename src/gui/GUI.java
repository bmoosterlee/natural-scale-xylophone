package gui;

import component.*;
import component.buffer.*;
import component.buffer.RunningOutputComponent;
import component.buffer.RunningPipeComponent;
import frequency.Frequency;
import spectrum.SpectrumWindow;
import spectrum.buckets.Buckets;
import spectrum.buckets.BucketsAverager;

import javax.swing.*;
import java.awt.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class GUI extends RunningPipeComponent<SimpleImmutableEntry<Buckets, Buckets>, java.util.List<Frequency>> {

    public GUI(SimpleBuffer<SimpleImmutableEntry<Buckets, Buckets>> inputBuffer, SimpleBuffer<java.util.List<Frequency>> outputBuffer, SpectrumWindow spectrumWindow, int width, int inaudibleFrequencyMargin){
        super(inputBuffer, outputBuffer, build(spectrumWindow, width, inaudibleFrequencyMargin));
    }

    public static CallableWithArguments<SimpleImmutableEntry<Buckets, Buckets>, java.util.List<Frequency>> build(SpectrumWindow spectrumWindow, int width, int inaudibleFrequencyMargin){
        return new CallableWithArguments<>() {
            private final int height = 600;
            private final double yScale = height * 0.95;
            private final double margin = height * 0.05;

            private final GUIPanel guiPanel;
            
            private OutputPort<SimpleImmutableEntry<Buckets, Buckets>> methodInputPort;
            private InputPort<java.util.List<Frequency>> methodOutputPort;

            class GUIPanel extends JPanel {
                private final InputPort<Map<Integer, Integer>> newNotesPort;
                private final InputPort<Map<Integer, Integer>> newHarmonicsPort;
                private final InputPort<Integer> newCursorXPort;

                GUIPanel(InputPort<Map<Integer, Integer>> newNotesPort, InputPort<Map<Integer, Integer>> newHarmonicsPort, InputPort<Integer> newCursorXPort) {
                    this.newNotesPort = newNotesPort;
                    this.newHarmonicsPort = newHarmonicsPort;
                    this.newCursorXPort = newCursorXPort;
                }

                @Override
                public void paintComponent(Graphics g) {
                    try {
                        Map<Integer, Integer> notes = newNotesPort.consume();
                        Map<Integer, Integer> harmonics = newHarmonicsPort.consume();
                        Integer cursorX = newCursorXPort.consume();

                        super.paintComponent(g);
                        renderHarmonicsBuckets(g, harmonics);
                        renderNoteBuckets(g, notes);
                        renderCursorLine(g, cursorX);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
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
            }

            {
                int capacity = 100;

                SimpleBuffer<SimpleImmutableEntry<Buckets, Buckets>> methodInputBuffer = new SimpleBuffer<>(capacity, "gui - method input");
                methodInputPort = methodInputBuffer.createOutputPort();

                LinkedList<BoundedBuffer<SimpleImmutableEntry<Buckets, Buckets>>> spectrumBroadcast =
                    new LinkedList<>(methodInputBuffer.broadcast(3));

                SimpleBuffer<Buckets> noteSpectrumBuffer = new SimpleBuffer<>(capacity, "gui - note spectrum");
                SimpleBuffer<Buckets> harmonicSpectrumBuffer = new SimpleBuffer<>(capacity, "gui - harmonic spectrum");
                new Unpairer<>(spectrumBroadcast.poll(), noteSpectrumBuffer, harmonicSpectrumBuffer);

                SimpleBuffer<Integer> cursorXBuffer = new SimpleBuffer<>(capacity, "cursorX - output");
                InputPort<Map<Integer, Integer>> newNotesPort =
                    noteSpectrumBuffer
                    .performMethod(GUI::bucketsToVolumes, "buckets to volumes - notes")
                    .performMethod(input2 -> volumesToYs(input2, yScale, margin), "volumes to ys - notes")
                    .createInputPort();
                InputPort<Map<Integer, Integer>> newHarmonicsPort =
                    harmonicSpectrumBuffer
                    .performMethod(BucketsAverager.build(inaudibleFrequencyMargin), "average buckets- harmonics")
                    .performMethod(GUI::bucketsToVolumes, "buckets to volumes - harmonics")
                    .performMethod(input2 -> volumesToYs(input2, yScale, margin), "volumes to ys - harmonics")
                    .createInputPort();
                InputPort<Integer> newCursorXPort = cursorXBuffer.createInputPort();
                guiPanel = new GUIPanel(newNotesPort, newHarmonicsPort, newCursorXPort);

                methodOutputPort =
                    spectrumBroadcast.poll()
                    .performMethod(input -> new Pulse(), "note - spectrum to pulse")
                    .performMethod(NoteClicker.build(spectrumWindow, guiPanel),
                            "note clicker - output")
                    .createInputPort();


                spectrumBroadcast.poll()
                    .performMethod(input1 -> new Pulse(), "harmonics - spectrum to pulse")
                    .performMethod(CursorMover.build(guiPanel))
                    .relayTo(cursorXBuffer);

                JFrame frame = new JFrame("Natural scale xylophone");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                guiPanel.setPreferredSize(new Dimension(width, height));
                frame.setContentPane(guiPanel);
                frame.pack();
                frame.setVisible(true);
            }

            private java.util.List<Frequency> clickNotes(SimpleImmutableEntry<Buckets, Buckets> input) {
                try {
                    methodInputPort.produce(input);

                    guiPanel.repaint();

                    return methodOutputPort.consume();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public java.util.List<Frequency> call(SimpleImmutableEntry<Buckets, Buckets> input) {
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
