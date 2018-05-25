public class Harmonic implements Comparable<Harmonic>{

    Note tonic;
    Fraction harmonicAsFraction;
    long lastSampleCount;

    public Harmonic(Note tonic, Fraction harmonicAsFraction){
        this.tonic = tonic;
        this.harmonicAsFraction = harmonicAsFraction;
    }

    @Override
    public int compareTo(Harmonic o) {
        return Double.compare(getSonanceValue(lastSampleCount), o.getSonanceValue(o.lastSampleCount))*-1;
    }

    public double getSonanceValue(long sampleCount) {
        return tonic.getVolume(sampleCount)/Math.max(harmonicAsFraction.numerator, harmonicAsFraction.denominator);
    }
}
