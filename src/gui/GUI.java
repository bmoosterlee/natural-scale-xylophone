package gui;

import harmonics.HarmonicCalculator;
import main.*;
import notes.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.*;

public class GUI extends JPanel implements Runnable, MouseListener, MouseMotionListener {
    public final SpectrumWindow spectrumWindow;
    public SpectrumSnapshot spectrumSnapshot;

    NoteEnvironment noteEnvironment;
    private NoteManager noteManager;

    public static final int WIDTH = 800*2;
    public static final int HEIGHT = 600;
    public static final double yScale = HEIGHT * 0.95;
    public static final double margin = HEIGHT * 0.05;

    public static final long FRAME_TIME = 1000000000 / 60;
    public long startTime;
    public int mouseX;
    boolean calculatedMouseFrequency;
    public double mouseFrequency;

    public GUI(NoteEnvironment noteEnvironment, HarmonicCalculator harmonicCalculator){
        this.noteEnvironment = noteEnvironment;
        noteManager = noteEnvironment.noteManager;

        spectrumWindow = new SpectrumWindow(noteManager, harmonicCalculator);

        JFrame frame = new JFrame("FrameDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        frame.setContentPane(this);
        frame.addMouseListener(this);
        frame.addMouseMotionListener(this);
        frame.pack();
        frame.setVisible(true);

    }

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);

        SpectrumSnapshotBuilder spectrumSnapshotBuilder = spectrumWindow.createBuilder(getNoteEnvironment().getExpectedSampleCount());
        while (getTimeLeftInFrame(startTime) > 1) {
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
        if(!calculatedMouseFrequency){
            mouseFrequency = spectrumWindow.getFrequency(mouseX);
            calculatedMouseFrequency = true;
        }
        g.setColor(Color.green);
        int x = spectrumWindow.getX(mouseFrequency);
        g.drawRect(x, 0, 1, HEIGHT);
    }

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        while(true) {
            startTime = System.nanoTime();
            tick();

            TimeKeeper sleepTimeKeeper = PerformanceTracker.startTracking("gui.GUI sleep");
            long timeLeftInFrame = getTimeLeftInFrame(startTime);
            if (timeLeftInFrame > 0) {
                try {
                    Thread.sleep(timeLeftInFrame);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            PerformanceTracker.stopTracking(sleepTimeKeeper);
        }
    }

    private void tick() {
        repaint();
    }

    private void renderHarmonicsBuckets(Graphics g, Buckets buckets) {
        g.setColor(Color.gray);

        renderBuckets(g, buckets);
    }

    public long getTimeLeftInFrame(long startTime) {
        long currentTime;
        currentTime = System.nanoTime();
        long timePassed = (currentTime - startTime);
        return (FRAME_TIME - timePassed)/ 1000000;
    }

    public int getX(double frequency) {
        return spectrumWindow.getX(frequency);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        noteManager.addNote(spectrumWindow.getFrequency(e.getX()));
    }

    public double getFrequency(double x) {
        return spectrumWindow.getFrequency(x);
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

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        calculatedMouseFrequency = false;
    }

    public NoteEnvironment getNoteEnvironment() {
        return noteEnvironment;
    }

}
