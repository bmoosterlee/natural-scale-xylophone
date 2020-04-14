package gui;

import component.*;
import component.buffer.*;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public class CursorMover {

    public static <A extends Packet<Pulse>, B extends Packet<Integer>> PipeCallable<BoundedBuffer<Pulse, A>, BoundedBuffer<Integer, B>> buildPipe(JPanel guiPanel) {
        return new PipeCallable<>() {
            Integer storedX = 0;
            boolean readyForNewOutput = true;

            @Override
            public BoundedBuffer<Integer, B> call(BoundedBuffer<Pulse, A> inputBuffer) {
                guiPanel.addMouseMotionListener(new CursorListener());

                return inputBuffer.performMethod(input -> getCursorX(), "cursor location");
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
