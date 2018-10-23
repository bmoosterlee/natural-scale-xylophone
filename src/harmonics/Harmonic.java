package harmonics;

import frequency.Frequency;

public class Harmonic {

    private final int sharedPeriod;
    private final Frequency harmonicFrequency;

    public Harmonic(Frequency tonic, Fraction harmonicAsFraction){
        sharedPeriod = calculateSharedPeriod(harmonicAsFraction);
        harmonicFrequency = calculateFrequency(tonic, harmonicAsFraction);
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

    private Frequency calculateFrequency(Frequency tonic, Fraction harmonicAsFraction) {
        return tonic.multiplyBy((double)harmonicAsFraction.numerator).divideBy((double)harmonicAsFraction.denominator);
    }

    public Frequency getHarmonicFrequency(){
        return harmonicFrequency;
    }
}
