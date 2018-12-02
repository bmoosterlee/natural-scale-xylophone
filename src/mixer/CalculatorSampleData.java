package mixer;

public class CalculatorSampleData<K, V> {
    private final Long sampleCount;
    private final K finalUnfinishedSampleFragments;
    private final V finishedSampleFragmentsUntilNow;

    public CalculatorSampleData(Long sampleCount, K finalUnfinishedSampleFragments, V finishedSampleFragmentsUntilNow) {
        this.sampleCount = sampleCount;
        this.finalUnfinishedSampleFragments = finalUnfinishedSampleFragments;
        this.finishedSampleFragmentsUntilNow = finishedSampleFragmentsUntilNow;
    }

    public Long getSampleCount() {
        return sampleCount;
    }

    public K getFinalUnfinishedSampleFragments() {
        return finalUnfinishedSampleFragments;
    }

    public V getFinishedSampleFragmentsUntilNow() {
        return finishedSampleFragmentsUntilNow;
    }
}
