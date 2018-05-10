import javax.swing.*;
import java.awt.*;

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

}
