package notebuilder.envelope.functions;

import sound.SampleRate;
import time.TimeInSeconds;

import java.util.HashMap;

public class LinearFunctionMemoizer {
    public final static LinearFunctionMemoizer ENVELOPE_MEMOIZER = new LinearFunctionMemoizer();

    private final HashMap<SampleRate, HashMap<Double, HashMap<TimeInSeconds, DeterministicFunction>>> calculatedEnvelopes;

    private LinearFunctionMemoizer(){
        calculatedEnvelopes = new HashMap<>();
    }

    public DeterministicFunction get(SampleRate sampleRate, double amplitude, TimeInSeconds lengthInSeconds) {
        HashMap<Double, HashMap<TimeInSeconds, DeterministicFunction>> sampleRateMap = calculatedEnvelopes.get(sampleRate);

        HashMap<TimeInSeconds, DeterministicFunction> amplitudeMap;
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
