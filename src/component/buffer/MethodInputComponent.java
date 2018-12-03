package component.buffer;

public class MethodInputComponent<K, A extends Packet<K>> extends AbstractInputComponent<K, A> {
    protected final InputCallable<K> method;

    public MethodInputComponent(SimpleBuffer<K, A> inputBuffer, InputCallable<K> method) {
        super(inputBuffer.createInputPort());
        this.method = method;
    }

    public MethodInputComponent(BufferChainLink<K, A> inputBuffer, InputCallable<K> method) {
        super(inputBuffer.createMethodInternalInputPort());
        this.method = method;
    }

    protected void tick() {
        A consumed = consume();
        process(consumed);
    }

    protected void tryTick() {
        A consumed = tryConsume();
        if(consumed!=null) {
            process(consumed);
        }
    }

    private A consume() {
        try {
            return input.consume();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private A tryConsume() {
        return input.tryConsume();
    }

    private void process(A consumed) {
        method.call(consumed.unwrap());
    }

    @Override
    public Boolean isParallelisable(){
        return method.isParallelisable();
    }

}
