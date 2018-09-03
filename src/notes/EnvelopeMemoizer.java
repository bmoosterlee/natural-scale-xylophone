package notes;

import main.SampleRate;

import java.util.HashMap;

public class EnvelopeMemoizer {
    HashMap<SampleRate, HashMap<Double, HashMap<Double, Envelope>>> calculatedEnvelopes;

    public EnvelopeMemoizer(){
        calculatedEnvelopes = new HashMap<>();
    }

    public Envelope get(SampleRate sampleRate, double amplitude, double lengthInSeconds) {
        HashMap<Double, HashMap<Double, Envelope>> sampleRateMap = calculatedEnvelopes.get(sampleRate);

        HashMap<Double, Envelope> amplitudeMap;
        try {
            amplitudeMap = sampleRateMap.get(amplitude);
        }
        catch(NullPointerException e){
            sampleRateMap = new HashMap<>();
            amplitudeMap = new HashMap<>();
            sampleRateMap.put(amplitude, amplitudeMap);
            calculatedEnvelopes.put(sampleRate, sampleRateMap);
        }

        Envelope envelope = amplitudeMap.get(lengthInSeconds);
        if(envelope==null){
            envelope = new PrecalculatedLinearEnvelope(0, sampleRate, amplitude, lengthInSeconds);
            amplitudeMap.put(lengthInSeconds, envelope);
        }
        return envelope;
    }

}
