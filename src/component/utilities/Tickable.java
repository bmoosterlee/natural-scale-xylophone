package component.utilities;

public abstract class Tickable implements Runnable {

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
