import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class GUI extends JPanel implements Runnable{
    NoteEnvironment noteEnvironment;
    HarmonicCalculator harmonicCalculator;

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    final double centerFrequency = 261.63;
    final double centerFrequency = 2 * 261.63;
    final double octaveRange = 2.;
    final double lowerBound;
    final double upperBound;

    public GUI(NoteEnvironment noteEnvironment, HarmonicCalculator harmonicCalculator){
        this.noteEnvironment = noteEnvironment;
        this.harmonicCalculator = harmonicCalculator;

        lowerBound = centerFrequency/octaveRange;
        upperBound = centerFrequency*octaveRange;

        JFrame frame = new JFrame("FrameDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        frame.getContentPane().add(this);

        frame.pack();

        frame.setVisible(true);
    }

    @Override
    public void paintComponent(Graphics g){
        Random random = new Random();
        for(int x = 0; x<WIDTH; x++){
            for(int y = 0; y<HEIGHT; y++){
                g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                g.drawRect(x, y, 1, 1);
            }
        }
    }

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        long frameTime = 1000/60;
        long startTime = System.nanoTime();
        long currentTime;

        while(true) {
            repaint();

            currentTime = System.nanoTime();
            long timePassed = (currentTime-startTime)/1000000;
            startTime = currentTime;
            long waitTime = frameTime-timePassed;

            if(waitTime>0){
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
