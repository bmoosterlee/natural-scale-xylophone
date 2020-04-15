package sound;

public class CalculateFFT {

    public static double[] calculateFFT(double[] signal, int width)
    {
        final int mNumberOfFFTPoints = width;
//        double mMaxFFTSample, mPeakPos;

        double temp;
        Complex[] y;
        Complex[] complexSignal = new Complex[mNumberOfFFTPoints];
        double[] absSignal = new double[mNumberOfFFTPoints];

        for(int i = 0; i < mNumberOfFFTPoints; i++){
            temp = signal[i];
            complexSignal[i] = new Complex(temp,0.0);
        }

        y = FFT.fft(complexSignal); // --> Here I use sound.FFT class

//        mMaxFFTSample = 0.0;
//        mPeakPos = 0;
        for(int i = 0; i < (mNumberOfFFTPoints); i++)
        {
            absSignal[i] = Math.sqrt(Math.pow(y[i].re(), 2) + Math.pow(y[i].im(), 2));
//            if(absSignal[i] > mMaxFFTSample)
//            {
//                mMaxFFTSample = absSignal[i];
//                mPeakPos = i;
//            }
        }

        return absSignal;

    }

}
