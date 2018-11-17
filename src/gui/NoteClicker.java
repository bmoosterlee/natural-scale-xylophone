package gui;

import component.Flusher;
import component.Pulse;
import component.buffer.*;
import frequency.Frequency;
import spectrum.SpectrumWindow;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

public class NoteClicker extends RunningPipeComponent<Pulse, List<Frequency>> {

    public NoteClicker(SimpleBuffer<Pulse> inputBuffer, SimpleBuffer<List<Frequency>> outputBuffer, SpectrumWindow spectrumWindow, JPanel guiPanel) {
        super(inputBuffer, outputBuffer, toMethod(buildPipe(spectrumWindow, guiPanel)));
    }

    public static CallableWithArguments<BoundedBuffer<Pulse>, BoundedBuffer<List<Frequency>>> buildPipe(SpectrumWindow spectrumWindow, JPanel guiPanel) {
        return new CallableWithArguments<>() {

            private OutputPort<Frequency> methodInputPort;

            @Override
            public BoundedBuffer<List<Frequency>> call(BoundedBuffer<Pulse> inputBuffer) {
                SimpleBuffer<Frequency> clickedFrequencies = new SimpleBuffer<>(1, "note clicker - click");
                methodInputPort = clickedFrequencies.createOutputPort();

                guiPanel.addMouseListener(new MyMouseListener());

                return inputBuffer.performMethod(Flusher.flush(clickedFrequencies));
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

        };
    }
}
