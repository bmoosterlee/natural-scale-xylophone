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

public class NoteClicker {

    public static <A extends Packet<Pulse>, B extends Packet<List<Frequency>>> PipeCallable<BoundedBuffer<Pulse, A>, BoundedBuffer<List<Frequency>, B>> buildPipe(SpectrumWindow spectrumWindow, JPanel guiPanel) {
        return new PipeCallable<>() {

            private SimpleBuffer<Frequency, SimplePacket<Frequency>> outputBuffer;
            private OutputPort<Frequency, SimplePacket<Frequency>> clickedFrequenciesPort;

            @Override
            public BoundedBuffer<List<Frequency>, B> call(BoundedBuffer<Pulse, A> inputBuffer) {
                guiPanel.addMouseListener(new MyMouseListener());

                outputBuffer = new SimpleBuffer<>(new OverwritableStrategy<>("note clicker - output"));
                clickedFrequenciesPort = outputBuffer.createOutputPort();

                return inputBuffer.performMethod(
                        Flusher.flush(
                                outputBuffer),
                        "note clicker - clicked frequencies");
            }

            class MyMouseListener implements MouseListener {
                @Override
                public void mouseClicked(MouseEvent e) {

                }

                @Override
                public void mousePressed(MouseEvent e) {
                    try {
                        clickedFrequenciesPort.produce(new SimplePacket<>(spectrumWindow.getFrequency(e.getX())));
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
