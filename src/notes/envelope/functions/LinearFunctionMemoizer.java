package notes.envelope.functions;

import sound.SampleRate;

import java.util.HashMap;

public class LinearFunctionMemoizer {
    public final static LinearFunctionMemoizer ENVELOPE_MEMOIZER = new LinearFunctionMemoizer();

    HashMap<SampleRate, HashMap<Double, HashMap<Double, DeterministicFunction>>> calculatedEnvelopes;

    public LinearFunctionMemoizer(){
        calculatedEnvelopes = new HashMap<>();
    }

    public DeterministicFunction get(SampleRate sampleRate, double amplitude, double lengthInSeconds) {
        HashMap<Double, HashMap<Double, DeterministicFunction>> sampleRateMap = calculatedEnvelopes.get(sampleRate);

        HashMap<Double, DeterministicFunction> amplitudeMap;
        try {
            amplitudeMap = sampleRateMap.get(amplitude);
        }
        catch(NullPointerException e){
            sampleRateMap = new HashMap<>();
            amplitudeMap = new HashMap<>();
            sampleRateMap.put(amplitude, amplitudeMap);
            calculatedEnvelopes.put(sampleRate, sampleRateMap);
        }

        DeterministicFunction envelopeFunction = amplitudeMap.get(lengthInSeconds);
        if(envelopeFunction==null){
            envelopeFunction = new PrecalculatedLinearFunction(sampleRate, amplitude, lengthInSeconds);
            amplitudeMap.put(lengthInSeconds, envelopeFunction);
        }
        return envelopeFunction;
    }

}
