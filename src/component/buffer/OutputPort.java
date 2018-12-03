package component.buffer;

public class OutputPort<K, A extends Packet<K>> {

    private final BoundedBuffer<K, A> buffer;

    public OutputPort(BoundedBuffer<K, A> buffer){
        this.buffer = buffer;
    }

    public OutputPort(){
        this("output port created buffer");
    }

    public OutputPort(String name){
        this(new SimpleBuffer<>(1, name));
    }

    public <Z extends A> void produce(Z packet) throws InterruptedException {
        buffer.offer(packet);
    }

    public BoundedBuffer<K, A> getBuffer() {
        return buffer;
    }

}

//todo each port can only connect to one other port. A component can have multiple ports.

//todo a component is an object and has a thread.

//todo implement these as they come up.
//to connect an output port to multiple input ports, we use a multiplexer, which fixes the waiting part.

//the other way round, multiple output ports connected to one input port is done by a combinator (find the word from telecom)
