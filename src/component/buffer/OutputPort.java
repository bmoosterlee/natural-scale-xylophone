package component.buffer;

public class OutputPort<T> {

    private final BoundedBuffer<T> buffer;

    public OutputPort(BoundedBuffer<T> buffer){
        this.buffer = buffer;
    }

    public void produce(T packet) throws InterruptedException {
        buffer.offer(packet);
    }

}

//todo each port can only connect to one other port. A component can have multiple ports.

//todo a component is an object and has a thread.

//todo implement these as they come up.
//to connect an output port to multiple input ports, we use a multiplexer, which fixes the waiting part.

//the other way round, multiple output ports connected to one input port is done by a combinator (find the word from telecom)
