package component;

import component.buffer.*;

import java.util.Collection;
import java.util.List;

public class Unzipper {

    public static <T> BoundedBuffer<T> unzip(BoundedBuffer<List<T>> input){
        BoundedBuffer<T> outputBuffer = new SimpleBuffer<>(1, "toBuffer - output");
        InputPort<List<T>> inputPort = input.createInputPort();
        OutputPort<T> outputPort = outputBuffer.createOutputPort();

        new TickRunner() {
            @Override
            protected void tick() {
                try {
                    Collection<T> input = inputPort.consume();
                    for(T element : input){
                        outputPort.produce(element);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        return outputBuffer;
    }

}
