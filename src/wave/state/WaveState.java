package wave.state;

import frequency.Frequency;
import notes.Note;
import sound.SampleRate;
import wave.Wave;

import java.util.*;

public class WaveState {
//todo the problem right now is that we don't know how to ensure that all contents of a FrequencyStateBucket are deterministic
//todo we can however write a method for each frequency state bucket to create a wave. This wave can then be added together
//todo how do we ensure we don't precalculate twice? We could use memoization.
//todo if we turn waves into envelopes of a special kind, we can use our memoizer to precalculate.
//todo if envelopes are immutable we can reuse a lot of data.
//todo 
    private SampleRate sampleRate;
    private Set<Frequency> frequencies;
    private Map<Frequency, Wave> frequencyWaveTable;

    private double angleComponent = 2.0 * Math.PI;

    WaveState(SampleRate sampleRate){
        this.sampleRate = sampleRate;
        frequencies = new HashSet<>();
        frequencyWaveTable = new HashMap<>();
    }

    private WaveState(SampleRate sampleRate, Set<Frequency> frequencies, Map<Frequency, Wave> frequencyWaveTable) {
        this.sampleRate = sampleRate;
        this.frequencies = frequencies;
        this.frequencyWaveTable = frequencyWaveTable;
    }

    private WaveState remove(Frequency frequency) {
        Set<Frequency> newFrequencies = new HashSet<>(frequencies);
        Map<Frequency, Wave> newFrequencyWaveTable = new HashMap<>(frequencyWaveTable);

        newFrequencies.remove(frequency);
        newFrequencyWaveTable.remove(frequency);

        return new WaveState(sampleRate, newFrequencies, newFrequencyWaveTable);
    }

    public WaveState add(Frequency frequency) {
        Set<Frequency> newFrequencies = new HashSet<>(frequencies);
        Map<Frequency, Wave> newFrequencyWaveTable = new HashMap<>(frequencyWaveTable);

        if(!frequencyWaveTable.containsKey(frequency)) {
            newFrequencies.add(frequency);
            Wave wave = new Wave(frequency, sampleRate);
            newFrequencyWaveTable.put(frequency, wave);
        }

        return new WaveState(sampleRate, newFrequencies, newFrequencyWaveTable);
    }

    public WaveState update(Set<Frequency> frequencies, Map<Frequency, Set<Note>> map) {
        Set<Frequency> newFrequencies = new HashSet<>();
        Map<Frequency, Wave> newFrequencyWaveTable = new HashMap<>();

        for(Frequency frequency : frequencies){
            newFrequencies.add(frequency);

            Set<Note> notes = map.get(frequency);
            Wave oldWave = frequencyWaveTable.get(frequency);
            Wave newWave;

            if(oldWave==null){
                newWave = new Wave(frequency, notes, sampleRate);
            }
            else{
                newWave = frequencyWaveTable.get(frequency).update(notes);
            }
            newFrequencyWaveTable.put(frequency, newWave);
        }

        return new WaveState(sampleRate, frequencies, newFrequencyWaveTable);
    }

    private Wave getWave(Frequency frequency) {
        return frequencyWaveTable.get(frequency);
    }

    public WaveState update(long sampleCount) {
        return this;
    }

    public double getAmplitude(Frequency frequency, long sampleCount) {
        //todo create infrastructure (just correctly named methods) to move 2*Pi here as well
        double timeAndAngleComponent = sampleRate.asTime(sampleCount).getValue() * angleComponent;
        return getWave(frequency).getAmplitudePrecalculated(timeAndAngleComponent);
    }
}
