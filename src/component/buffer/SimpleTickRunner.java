package component.buffer;

public abstract class SimpleTickRunner extends TickRunner {

    private boolean alive = true;

    @Override
    public void run() {
        while(alive){
            tick();
        }
    }

    public void kill() {
        alive = false;
    }
}
