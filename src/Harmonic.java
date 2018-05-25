public class Harmonic implements Comparable<Harmonic>{

    Note tonic;
    Fraction harmonicAsFraction;
    double noteVolume;

    public Harmonic(Note tonic, Fraction harmonicAsFraction){
        this.tonic = tonic;
        this.harmonicAsFraction = harmonicAsFraction;
    }

    @Override
    public int compareTo(Harmonic o) {
        return Double.compare(getSonanceValue(), o.getSonanceValue())*-1;
    }

    public double getSonanceValue() {
        return noteVolume/Math.max(harmonicAsFraction.numerator, harmonicAsFraction.denominator);
    }

    public double getFrequency() {
        return tonic.getFrequency()*(double)harmonicAsFraction.numerator/(double)harmonicAsFraction.denominator;
    }
}
