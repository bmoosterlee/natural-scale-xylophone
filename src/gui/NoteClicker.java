package gui;

import component.*;
import frequency.Frequency;
import spectrum.SpectrumWindow;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.concurrent.Callable;

public class NoteClicker extends TickableOutputComponent<Frequency> {

    public NoteClicker(SimpleBuffer<Frequency> newNoteBuffer, SpectrumWindow spectrumWindow, JPanel guiPanel) {
        super(newNoteBuffer, build(spectrumWindow, guiPanel));
    }

    public static Callable<Frequency> build(SpectrumWindow spectrumWindow, JPanel guiPanel) {
        return new Callable<>() {
            private final OutputPort<Frequency> methodInputPort;
            private final InputPort<Frequency> methodOutputPort;

            {
                BoundedBuffer<Frequency> clickedFrequencies = new SimpleBuffer<>(1, "clickedFrequencies");
                methodInputPort = clickedFrequencies.createOutputPort();
                methodOutputPort = clickedFrequencies.createInputPort();

                guiPanel.addMouseListener(new MyMouseListener());
            }

            class MyMouseListener implements MouseListener {
                @Override
                public void mouseClicked(MouseEvent e) {

                }

                @Override
                public void mousePressed(MouseEvent e) {
                    try {
                        methodInputPort.produce(spectrumWindow.getFrequency(e.getX()));
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

            private Frequency clickFrequency() {
                try {
                    return methodOutputPort.consume();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public Frequency call() {
                return clickFrequency();
            }
        };
    }
}
