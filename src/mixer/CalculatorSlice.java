package mixer;

public class CalculatorSlice<K, V> {
    private final Long sampleCount;
    private final K finalUnfinishedSlice;
    private final V finishedSliceUntilNow;

    public CalculatorSlice(Long sampleCount, K finalUnfinishedSlice, V finishedSliceUntilNow) {
        this.sampleCount = sampleCount;
        this.finalUnfinishedSlice = finalUnfinishedSlice;
        this.finishedSliceUntilNow = finishedSliceUntilNow;
    }

    public Long getSampleCount() {
        return sampleCount;
    }

    public K getFinalUnfinishedSlice() {
        return finalUnfinishedSlice;
    }

    public V getFinishedSliceUntilNow() {
        return finishedSliceUntilNow;
    }
}
