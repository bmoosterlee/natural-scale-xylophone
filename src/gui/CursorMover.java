package gui;

import component.*;
import component.buffer.CallableWithArguments;
import component.buffer.SimpleBuffer;
import component.buffer.RunningPipeComponent;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public class CursorMover extends RunningPipeComponent<Pulse, Integer> {

    public CursorMover(SimpleBuffer<Pulse> inputBuffer, SimpleBuffer<Integer> outputBuffer, JPanel guiPanel) {
        super(inputBuffer, outputBuffer, build(guiPanel));
    }

    public static CallableWithArguments<Pulse, Integer> build(JPanel guiPanel) {
        return new CallableWithArguments<>() {
            private Integer storedX = 0;
            boolean readyForNewOutput = true;

            {
                guiPanel.addMouseMotionListener(new CursorListener());
            }

            class CursorListener implements MouseMotionListener {
                @Override
                public void mouseDragged(MouseEvent e) {

                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    if (readyForNewOutput) {
                        readyForNewOutput = false;
                        storedX = e.getX();
                    }
                }
            }

            private Integer getCursorX() {
                Integer result = storedX;
                readyForNewOutput = true;
                return result;
            }

            @Override
            public Integer call(Pulse input) {
                return getCursorX();
            }
        };
    }

}
