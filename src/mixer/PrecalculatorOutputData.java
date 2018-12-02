package mixer;

public class PrecalculatorOutputData<I, K, V> {
    private final I index;
    private final K finalUnfinishedData;
    private final V finishedDataUntilNow;

    public PrecalculatorOutputData(I index, K finalUnfinishedData, V finishedDataUntilNow) {
        this.index = index;
        this.finalUnfinishedData = finalUnfinishedData;
        this.finishedDataUntilNow = finishedDataUntilNow;
    }

    public I getIndex() {
        return index;
    }

    public K getFinalUnfinishedData() {
        return finalUnfinishedData;
    }

    public V getFinishedDataUntilNow() {
        return finishedDataUntilNow;
    }
}
