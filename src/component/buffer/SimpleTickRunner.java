package component.buffer;

public abstract class SimpleTickRunner implements Runnable {

    private boolean alive;

    public void start(){
        alive = true;
        new Thread(this).start();
    }

    @Override
    public void run() {
        while(alive){
            tick();
        }
    }

    protected abstract void tick();

    public void kill() {
        alive = false;
    }
}
