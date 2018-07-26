public class Harmonic implements Comparable<Harmonic>{

    final int sharedPeriod;
    final double frequency;
    final double noteVolume;

    public Harmonic(Note tonic, Fraction harmonicAsFraction, double noteVolume){
        sharedPeriod = calculateSharedPeriod(harmonicAsFraction);
        frequency = calculateFrequency(tonic, harmonicAsFraction);
        this.noteVolume = noteVolume;
    }

    @Override
    public int compareTo(Harmonic o) {
        return Double.compare(getHarmonicValue(), o.getHarmonicValue())*-1;
    }

    public double getHarmonicValue() {
        return getVolume(noteVolume);
    }

    public double getVolume(double noteVolume) {
        return noteVolume/sharedPeriod;
    }

    public static double getHarmonicValue(double noteVolume, Fraction harmonic) {
        return noteVolume/calculateSharedPeriod(harmonic);
    }

    private static int calculateSharedPeriod(Fraction harmonicAsFraction) {
        return Math.max(harmonicAsFraction.numerator, harmonicAsFraction.denominator);
    }

    private double calculateFrequency(Note tonic, Fraction harmonicAsFraction) {
        return tonic.getFrequency()*(double)harmonicAsFraction.numerator/(double)harmonicAsFraction.denominator;
    }

    public double getFrequency(){
        return frequency;
    }
}
