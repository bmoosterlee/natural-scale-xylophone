package gui;

import component.*;
import component.buffer.BoundedBuffer;
import component.buffer.CallableWithArguments;
import component.buffer.SimpleBuffer;
import component.buffer.RunningPipeComponent;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public class CursorMover extends RunningPipeComponent<Pulse, Integer> {

    public CursorMover(SimpleBuffer<Pulse> inputBuffer, SimpleBuffer<Integer> outputBuffer, JPanel guiPanel) {
        super(inputBuffer, outputBuffer, toMethod(buildPipe(guiPanel)));
    }

    public static CallableWithArguments<BoundedBuffer<Pulse>, BoundedBuffer<Integer>> buildPipe(JPanel guiPanel) {
        return new CallableWithArguments<>() {
            Integer storedX = 0;
            boolean readyForNewOutput = true;

            @Override
            public BoundedBuffer<Integer> call(BoundedBuffer<Pulse> inputBuffer) {
                guiPanel.addMouseMotionListener(new CursorListener());

                return inputBuffer.performMethod(input -> getCursorX());
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

            private Integer getCursorX () {
                Integer result = storedX;
                readyForNewOutput = true;
                return result;
            }
        };
    }

}
