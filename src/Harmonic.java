public class Harmonic implements Comparable<Harmonic>{

    final int sharedPeriod;
    final double frequency;
    double noteVolume;

    public Harmonic(Note tonic, Fraction harmonicAsFraction){
        sharedPeriod = calculateSharedPeriod(harmonicAsFraction);
        frequency = calculateFrequency(tonic, harmonicAsFraction);
    }

    @Override
    public int compareTo(Harmonic o) {
        return Double.compare(getSonanceValue(), o.getSonanceValue())*-1;
    }

    public double getSonanceValue() {
        return noteVolume/sharedPeriod;
    }

    private int calculateSharedPeriod(Fraction harmonicAsFraction) {
        return Math.max(harmonicAsFraction.numerator, harmonicAsFraction.denominator);
    }

    private double calculateFrequency(Note tonic, Fraction harmonicAsFraction) {
        return tonic.getFrequency()*(double)harmonicAsFraction.numerator/(double)harmonicAsFraction.denominator;
    }

    public double getFrequency(){
        return frequency;
    }
}
