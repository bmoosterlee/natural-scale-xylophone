package gui;

import component.BoundedBuffer;
import component.OutputPort;
import component.Pulse;
import component.TimedConsumerComponent;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public class CursorMover extends TimedConsumerComponent implements MouseMotionListener {
    private Integer storedX;

    private final OutputPort<Integer> cursorX;

    public CursorMover(BoundedBuffer<Pulse> inputBuffer, BoundedBuffer<Integer> outputBuffer) {
        super(inputBuffer);
        cursorX = new OutputPort<>(outputBuffer);
    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if(storedX==null) {
            storedX = e.getX();
        }
    }

    @Override
    protected void timedTick() {
        try {
            try {
                cursorX.produce(storedX);
            }
            catch(NullPointerException ignored){
            }
            storedX = null;
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }
}
