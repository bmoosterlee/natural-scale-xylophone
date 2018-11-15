package gui;

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
        super(inputBuffer, outputBuffer, build(spectrumWindow, guiPanel));
    }

    public static CallableWithArguments<Pulse, List<Frequency>> build(SpectrumWindow spectrumWindow, JPanel guiPanel) {
        return new CallableWithArguments<>() {
            private final OutputPort<Frequency> methodInputPort;
            private final InputPort<Frequency> methodOutputPort;

            {
                BoundedBuffer<Frequency> clickedFrequencies = new SimpleBuffer<>(100, "clickedFrequencies");
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

            private List<Frequency> clickFrequency() {
                try {
                    return methodOutputPort.flush();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public List<Frequency> call(Pulse input) {
                return clickFrequency();
            }
        };
    }
}
