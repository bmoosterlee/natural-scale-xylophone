public class Harmonic implements Comparable<Harmonic>{

    Note tonic;
    Fraction harmonicAsFraction;

    public Harmonic(Note tonic, Fraction harmonicAsFraction){
        this.tonic = tonic;
        this.harmonicAsFraction = harmonicAsFraction;
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
