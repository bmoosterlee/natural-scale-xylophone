public class Harmonic implements Comparable<Harmonic>{

    Note tonic;
    Fraction harmonicAsFraction;

    public Harmonic(Note tonic){
        this.tonic = tonic;
        harmonicAsFraction = new Fraction(1, 1);
    }

    @Override
    public int compareTo(Harmonic otherHarmonic) {
        if(harmonicAsFraction.denominator<otherHarmonic.harmonicAsFraction.denominator) {
            return -1;
        }
        else if(harmonicAsFraction.denominator==otherHarmonic.harmonicAsFraction.denominator){
            if(harmonicAsFraction.numerator<otherHarmonic.harmonicAsFraction.numerator){
                return -1;
            }
            else if(harmonicAsFraction.numerator==otherHarmonic.harmonicAsFraction.numerator){
                return 0;
            }
            else{
                return 1;
            }
        }
        else {
            return 1;
        }
    }
}
