package component.buffer;

public class SimpleTickRunner extends TickRunner {

    private final AbstractComponent component;
    private boolean alive;

    public SimpleTickRunner(AbstractComponent component){
        this.component = component;
        alive = true;
    }

    @Override
    public void run() {
        while(alive){
            tick();
        }
    }

    void kill() {
        alive = false;
    }

    @Override
    protected void tick() {
        component.tick();
    }
}
