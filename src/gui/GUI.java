package gui;

import harmonics.HarmonicCalculator;
import notes.*;
import notes.state.Observer;
import notes.state.SampleTicker;
import notes.state.NoteManager;
import notes.state.Ticker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.*;

public class GUI extends JPanel {
    public final SpectrumWindow spectrumWindow;
    private final MouseListener mouseListener;
    private final MouseMotionListener mouseMotionListener;

    public SpectrumSnapshot spectrumSnapshot;
    SampleTicker sampleTicker;

    public static final int WIDTH = 800*2;
    public static final int HEIGHT = 600;
    public static final double yScale = HEIGHT * 0.95;
    public static final double margin = HEIGHT * 0.05;

    public Ticker ticker;
    public long startTime;
    public Frequency mouseFrequency;

    public GUI(SampleTicker sampleTicker, HarmonicCalculator harmonicCalculator, NoteManager noteManager){
        GUI.this.sampleTicker = sampleTicker;

        ticker = new Ticker((long) (1000000000 / 60));
        ticker.getTickObservable().add((Observer<Long>) event -> tick());

        spectrumWindow = new SpectrumWindow(noteManager, harmonicCalculator);

        mouseListener = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {
                Frequency frequency = spectrumWindow.getFrequency(e.getX());
                clickFrequency(noteManager, frequency);
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
        mouseMotionListener = new MouseMotionListener() {
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

    }

    public void start(){
        ticker.start();
    }

    private void tick() {
        startTime = System.nanoTime();
        repaint();
    }

    private void moveMouse(Frequency newFrequency) {
        mouseFrequency = newFrequency;
    }

    private void clickFrequency(NoteManager noteManager, Frequency frequency) {
        noteManager.addNote(frequency);
    }

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);

        SpectrumSnapshotBuilder spectrumSnapshotBuilder = spectrumWindow.createBuilder(getSampleTicker().getExpectedTickCount());
        while (ticker.getTimeLeftInFrame(startTime) > 1) {
            if (spectrumSnapshotBuilder.update()) break;
        }
        spectrumSnapshot = spectrumSnapshotBuilder.finish();

        renderHarmonicsBuckets(g, spectrumSnapshot.harmonicsBuckets.averageBuckets(10));
        renderNoteBuckets(g, spectrumSnapshot.noteBuckets);

        renderCursorLine(g);
    }

    private void renderNoteBuckets(Graphics g, Buckets buckets) {
        g.setColor(Color.blue);

        renderBuckets(g, buckets);
    }

    private void renderBuckets(Graphics g, Buckets buckets) {
        Iterator<Map.Entry<Integer, Double>> iterator = buckets.iterator();
        while(iterator.hasNext()) {
            Map.Entry<Integer, Double> pair = iterator.next();
            int x = pair.getKey();
            int y = (int) (pair.getValue() * yScale + margin);
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

    public int getX(Frequency frequency) {
        return spectrumWindow.getX(frequency);
    }

    public Frequency getFrequency(double x) {
        return spectrumWindow.getFrequency(x);
    }

    public SampleTicker getSampleTicker() {
        return sampleTicker;
    }

}
