package notes;

import main.SampleRate;

import java.util.HashMap;

public class EnvelopeMemoizer {
    HashMap<SampleRate, HashMap<Double, HashMap<Double, EnvelopeFunction>>> calculatedEnvelopes;

    public EnvelopeMemoizer(){
        calculatedEnvelopes = new HashMap<>();
    }

    public EnvelopeFunction get(SampleRate sampleRate, double amplitude, double lengthInSeconds) {
        HashMap<Double, HashMap<Double, EnvelopeFunction>> sampleRateMap = calculatedEnvelopes.get(sampleRate);

        HashMap<Double, EnvelopeFunction> amplitudeMap;
        try {
            amplitudeMap = sampleRateMap.get(amplitude);
        }
        catch(NullPointerException e){
            sampleRateMap = new HashMap<>();
            amplitudeMap = new HashMap<>();
            sampleRateMap.put(amplitude, amplitudeMap);
            calculatedEnvelopes.put(sampleRate, sampleRateMap);
        }

        EnvelopeFunction envelopeFunction = amplitudeMap.get(lengthInSeconds);
        if(envelopeFunction==null){
            envelopeFunction = new PrecalculatedLinearEnvelopeFunction(sampleRate, amplitude, lengthInSeconds);
            amplitudeMap.put(lengthInSeconds, envelopeFunction);
        }
        return envelopeFunction;
    }

}
