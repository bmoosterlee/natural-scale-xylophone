import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class GUI extends JPanel{
    NoteEnvironment noteEnvironment;
    HarmonicCalculator harmonicCalculator;

    public GUI(NoteEnvironment noteEnvironment, HarmonicCalculator harmonicCalculator){
        this.noteEnvironment = noteEnvironment;
        this.harmonicCalculator = harmonicCalculator;

        JFrame frame = new JFrame("FrameDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setPreferredSize(new Dimension(800, 600));
        frame.getContentPane().add(this);

        frame.pack();

        frame.setVisible(true);
    }

    @Override
    public void paintComponent(Graphics g){
        Random random = new Random();
        for(int x = 0; x<100; x++){
            for(int y = 0; y<100; y++){
                g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                g.drawRect(x, y, 1, 1);
            }
        }
    }

}
