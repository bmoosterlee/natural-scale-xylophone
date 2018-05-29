import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedList;

public class GUI extends JPanel implements Runnable, MouseListener {
    private final Buckets harmonicsBuckets;
    NoteEnvironment noteEnvironment;
    HarmonicCalculator harmonicCalculator;

    public static final int WIDTH = 800*2;
    public static final int HEIGHT = 600;
    final double centerFrequency = 2 * 261.63;
    final double octaveRange = 3.;
    final double lowerBound;
    final double upperBound;
    final double frequencyRange;

    Image offScreen;
    Graphics offScreenGraphics;
    public static final long FRAME_TIME = 1000 / 60;

    public GUI(NoteEnvironment noteEnvironment, HarmonicCalculator harmonicCalculator){
        this.noteEnvironment = noteEnvironment;
        this.harmonicCalculator = harmonicCalculator;

        lowerBound = centerFrequency/octaveRange;
        upperBound = centerFrequency*octaveRange;

        frequencyRange = upperBound-lowerBound;
        
        JFrame frame = new JFrame("FrameDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        frame.getContentPane().add(this);
        frame.addMouseListener(this);

        frame.pack();

        frame.setVisible(true);

        offScreen = createImage(WIDTH, HEIGHT);
        offScreenGraphics = offScreen.getGraphics();

        harmonicsBuckets = new Buckets(WIDTH);
    }

    @Override
    public void paintComponent(Graphics g){
        g.drawImage(offScreen, 0, 0, null);
    }

    private void renderNotes() {
        LinkedList<Note> liveNotes = (LinkedList<Note>) noteEnvironment.getLiveNotes().clone();

        offScreenGraphics.setColor(Color.blue);
        for(Note note : liveNotes) {
            double frequency = note.getFrequency();
            int x = (int) ((Math.log(frequency)/Math.log(2) - Math.log(lowerBound)/Math.log(2)) / (Math.log(upperBound)/Math.log(2)-Math.log(lowerBound)/Math.log(2)) * WIDTH);
            int y = (int)(HEIGHT*(0.05+0.95*noteEnvironment.getVolume(note, noteEnvironment.getExpectedSampleCount())));
            g.drawRect(x, HEIGHT-y, 1, y);
        }
    }

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        long startTime;

        while(true) {
            startTime = System.nanoTime();
            TimeKeeper timeKeeper = PerformanceTracker.startTracking("GUI repaint");

            tick(startTime);

            PerformanceTracker.stopTracking(timeKeeper);
            long timeLeftInFrame = getTimeLeftInFrame(startTime);

            if (timeLeftInFrame > 0) {
                try {
                    Thread.sleep(timeLeftInFrame);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void tick(long startTime) {
        offScreenGraphics.clearRect(0, 0, WIDTH, HEIGHT);

        addHarmonicsToBuckets(startTime);
        renderHarmonicsBuckets();

        renderNotes();
        repaint();
    }

    private void addHarmonicsToBuckets(long startTime) {
        harmonicsBuckets.clear();
        long sampleCountAtFrame = noteEnvironment.getExpectedSampleCount();
        long timeLeftInFrame = getTimeLeftInFrame(startTime);
        
        while (timeLeftInFrame > 10) {
            Harmonic harmonic = harmonicCalculator.getNextHarmonic(sampleCountAtFrame);
            addToBucket(harmonic);

            timeLeftInFrame = getTimeLeftInFrame(startTime);
        }
    }

    private void renderHarmonicsBuckets() {
        offScreenGraphics.setColor(Color.gray);

        for(int i = 0; i<WIDTH; i++) {
            double value = harmonicsBuckets.getValue(i);
            int x = i;
            int y = (int)(HEIGHT*(0.05+0.95* value));
            offScreenGraphics.drawRect(x, HEIGHT - y, 1, y);
        }
    }

    private long getTimeLeftInFrame(long startTime) {
        long currentTime;
        currentTime = System.nanoTime();
        long timePassed = (currentTime - startTime) / 1000000;
        return FRAME_TIME - timePassed;
    }

    private void addToBucket(Harmonic harmonic) {
        if(harmonic==null){
            return;
        }

        double frequency = harmonic.getFrequency();
        int x = (int) ((Math.log(frequency)/Math.log(2) - Math.log(lowerBound)/Math.log(2)) / (Math.log(upperBound)/Math.log(2)-Math.log(lowerBound)/Math.log(2)) * WIDTH);
        if(x<0 || x>=WIDTH){
            return;
        }
//        double sonanceValue = harmonic.getSonanceValue(sampleCountAtFrame)/harmonic.tonic.getVolume(sampleCountAtFrame) * 0.5*100000./(100000.+(sampleCountAtFrame- harmonic.tonic.getStartingSampleCount()));
        double sonanceValue = harmonic.getSonanceValue();
        harmonicsBuckets.fill(x, sonanceValue);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        double frequency = Math.exp(((double)e.getX()/WIDTH)*(Math.log(upperBound)-Math.log(lowerBound))+Math.log(lowerBound));
        noteEnvironment.addNote(new Note(frequency, noteEnvironment.getExpectedSampleCount()));
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
}
