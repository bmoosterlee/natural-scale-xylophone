package sound;

public class CalculateFFT {

    public static Complex[] calculateFFT(double[] signal, int width)
    {
        final int mNumberOfFFTPoints = width;

        double temp;
        Complex[] complexSignal = new Complex[mNumberOfFFTPoints];
        for(int i = 0; i < mNumberOfFFTPoints; i++){
            temp = signal[i];
            complexSignal[i] = new Complex(temp,0.0);
        }

        Complex[] y;
        y = FFT.fft(complexSignal); // --> Here I use sound.FFT class

        return y;
    }

    public static double[] getMagnitudes(Complex[] y, int mNumberOfFFTPoints) {
        double[] absSignal = new double[mNumberOfFFTPoints];
        for(int i = 0; i < (mNumberOfFFTPoints); i++) {
            absSignal[i] = getMagnitude(y[i]);
        }
        return absSignal;
    }

    public static double getMagnitude(Complex y) {
        return Math.sqrt(Math.pow(y.re(), 2) + Math.pow(y.im(), 2));
    }

    public static double[] getPhases(Complex[] y, int mNumberOfFFTPoints) {
        double[] absSignal = new double[mNumberOfFFTPoints];
        for(int i = 0; i < (mNumberOfFFTPoints); i++) {
            absSignal[i] = Math.atan2(y[i].im(), y[i].re());
        }
        return absSignal;
    }

    public static double[] calculateIFFT(Complex[] spectrum, int width)
    {
        final int mNumberOfFFTPoints = width;

        Complex temp;
        Complex[] y;

        y = FFT.ifft(spectrum); // --> Here I use sound.FFT class

        double[] absSignal = new double[mNumberOfFFTPoints];
        for(int i = 0; i < mNumberOfFFTPoints; i++){
            temp = y[i];
            absSignal[i] = temp.re();
        }

        return absSignal;

    }

}
