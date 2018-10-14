package gui;

import main.BoundedBuffer;
import main.OutputPort;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public class CursorMover implements MouseMotionListener {
    private final OutputPort<Integer> cursorX;

    public CursorMover(BoundedBuffer<Integer> cursorXBuffer) {
        cursorX = new OutputPort<>(cursorXBuffer);
    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        try {
            cursorX.produce(e.getX());
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }
}
