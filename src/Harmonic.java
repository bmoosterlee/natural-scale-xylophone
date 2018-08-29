public class Harmonic {

    final int sharedPeriod;
    final double frequency;

    public Harmonic(Double tonic, Fraction harmonicAsFraction){
        sharedPeriod = calculateSharedPeriod(harmonicAsFraction);
        frequency = calculateFrequency(tonic, harmonicAsFraction);
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

    private double calculateFrequency(Double tonic, Fraction harmonicAsFraction) {
        return tonic*(double)harmonicAsFraction.numerator/(double)harmonicAsFraction.denominator;
    }

    public double getFrequency(){
        return frequency;
    }
}
