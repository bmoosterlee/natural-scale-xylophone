import javax.swing.*;
import java.awt.*;

public class GUI {

    public GUI(){
        JFrame frame = new JFrame("FrameDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(800, 600));
        frame.getContentPane().add(panel);

        frame.pack();

        frame.setVisible(true);
    }

}
