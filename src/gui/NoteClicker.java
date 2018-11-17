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

            private OutputPort<Frequency> clickedFrequenciesPort;

            @Override
            public BoundedBuffer<List<Frequency>> call(BoundedBuffer<Pulse> inputBuffer) {
                clickedFrequenciesPort = new OutputPort<>("clicked frequency buffer");

                guiPanel.addMouseListener(new MyMouseListener());

                return inputBuffer.performMethod(Flusher.flush(clickedFrequenciesPort.getBuffer()), "clicked frequencies");
            }

            class MyMouseListener implements MouseListener {
                @Override
                public void mouseClicked(MouseEvent e) {

                }

                @Override
                public void mousePressed(MouseEvent e) {
                    try {
                        clickedFrequenciesPort.produce(spectrumWindow.getFrequency(e.getX()));
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
