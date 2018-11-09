package gui;

import frequency.Frequency;
import spectrum.SpectrumWindow;
import component.BoundedBuffer;
import component.OutputPort;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class NoteClicker implements MouseListener {
    private final OutputPort<Frequency> clickedFrequencies;
    private final SpectrumWindow spectrumWindow;

    public NoteClicker(BoundedBuffer<Frequency> newNoteBuffer, SpectrumWindow spectrumWindow) {
        this.spectrumWindow = spectrumWindow;
        clickedFrequencies = new OutputPort<>(newNoteBuffer);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        try {
            clickedFrequencies.produce(spectrumWindow.getFrequency(e.getX()));
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
