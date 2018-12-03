package component.buffer;

public class MethodPipeComponent<K, V, A extends Packet<K>, B extends Packet<V>> extends AbstractPipeComponent<K, V, A, B> {
    protected final PipeCallable<K, V> method;

    public MethodPipeComponent(SimpleBuffer<K, A> inputBuffer, BoundedBuffer<V, B> outputBuffer, PipeCallable<K, V> method) {
        super(inputBuffer.createInputPort(), new OutputPort<>(outputBuffer));
        this.method = method;
    }

    public MethodPipeComponent(BufferChainLink<K, A> inputBuffer, BoundedBuffer<V, B> outputBuffer, PipeCallable<K, V> method) {
        super(inputBuffer.createMethodInternalInputPort(), new OutputPort<>(outputBuffer));
        this.method = method;
    }

    protected void tick() {
        A consumed = consume();
        procesAndProduce(consumed);
    }

    protected void tryTick() {
        A consumed = tryConsume();
        if(consumed!=null) {
            procesAndProduce(consumed);
        }
    }

    private A consume() {
        try{
            return input.consume();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private A tryConsume() {
        return input.tryConsume();
    }

    private void procesAndProduce(A consumed) {
        try {
            B result = consumed.transform(method);
            output.produce(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Boolean isParallelisable(){
        return method.isParallelisable();
    }

}
