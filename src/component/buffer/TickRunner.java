package component.buffer;

public abstract class TickRunner implements Runnable {
    public void start(){
        new Thread(this).start();
    }

    protected abstract void tick();
}
