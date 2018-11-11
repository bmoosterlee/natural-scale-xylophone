package gui;

import component.*;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public class CursorMover extends TickablePipeComponent<Pulse, Integer> {

    public CursorMover(BoundedBuffer<Pulse> inputBuffer, BoundedBuffer<Integer> outputBuffer, JPanel guiPanel) {
        super(inputBuffer, outputBuffer, build(guiPanel));
    }

    public static CallableWithArguments<Pulse, Integer> build(JPanel guiPanel) {
        return new CallableWithArguments<>() {
            private Integer storedX;

            {
                guiPanel.addMouseMotionListener(new CursorListener());
            }

            class CursorListener implements MouseMotionListener {
                @Override
                public void mouseDragged(MouseEvent e) {

                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    if (storedX == null) {
                        storedX = e.getX();
                    }
                }
            }

            private Integer sendCursor() {
                Integer oldX = storedX;
                storedX = null;
                return oldX;
            }

            @Override
            public Integer call(Pulse input) {
                return sendCursor();
            }
        };
    }

}
