package component.buffer;

public interface ComponentCallable {

    default Boolean isParallelisable(){
        return true;
    }

}
