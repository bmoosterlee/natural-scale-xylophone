package component;

public abstract class TickingComponent implements Runnable, Component {

    protected void start(){
        new Thread(this).start();
    }

    @Override
    public void run() {
        while(true){
            tick();
        }
    }

    protected abstract void tick();

}
