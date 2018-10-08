package wave.state;

import frequency.Frequency;
import sound.SampleRate;
import wave.Wave;

import java.util.*;

public class SimpleWaveState implements WaveState {
    private SampleRate sampleRate;
    private Set<Frequency> frequencies;
    private Map<Frequency, Wave> frequencyWaveTable;

    private double angleComponent = 2.0 * Math.PI;

    SimpleWaveState(SampleRate sampleRate){
        this.sampleRate = sampleRate;
        frequencies = new HashSet<>();
        frequencyWaveTable = new HashMap<>();
    }

    private SimpleWaveState(SampleRate sampleRate, Set<Frequency> frequencies, Map<Frequency, Wave> frequencyWaveTable) {
        this.sampleRate = sampleRate;
        this.frequencies = frequencies;
        this.frequencyWaveTable = frequencyWaveTable;
    }

    private SimpleWaveState remove(Frequency frequency) {
        Set<Frequency> newFrequencies = new HashSet<>(frequencies);
        Map<Frequency, Wave> newFrequencyWaveTable = new HashMap<>(frequencyWaveTable);

        newFrequencies.remove(frequency);
        newFrequencyWaveTable.remove(frequency);

        return new SimpleWaveState(sampleRate, newFrequencies, newFrequencyWaveTable);
    }

    public SimpleWaveState add(Frequency frequency) {
        Set<Frequency> newFrequencies = new HashSet<>(frequencies);
        Map<Frequency, Wave> newFrequencyWaveTable = new HashMap<>(frequencyWaveTable);

        if(!frequencyWaveTable.containsKey(frequency)) {
            newFrequencies.add(frequency);
            Wave wave = new Wave(frequency, sampleRate);
            newFrequencyWaveTable.put(frequency, wave);
        }

        return new SimpleWaveState(sampleRate, newFrequencies, newFrequencyWaveTable);
    }

    @Override
    public WaveState update(Set<Frequency> frequencies) {
        if(frequencies.equals(this.frequencies)){
            return this;
        }

        SimpleWaveState newWaveState = this;

        Set<Frequency> removedFrequencies = new HashSet<>(this.frequencies);
        removedFrequencies.removeAll(new HashSet<>(frequencies));

        Set<Frequency> addedFrequencies = new HashSet<>(frequencies);
        addedFrequencies.removeAll(new HashSet<>(this.frequencies));

        if(!removedFrequencies.isEmpty()) {
            for (Frequency removedFrequency : removedFrequencies) {
                newWaveState = newWaveState.remove(removedFrequency);
            }
        }

        if(!addedFrequencies.isEmpty()) {
            for (Frequency addedFrequency : addedFrequencies) {
                try {
                    newWaveState = newWaveState.add(addedFrequency);
                } catch (NullPointerException ignored) {

                }
            }
        }

        return newWaveState;
    }

    private Wave getWave(Frequency frequency) {
        return frequencyWaveTable.get(frequency);
    }

    @Override
    public WaveState update(long sampleCount) {
        return this;
    }

    @Override
    public double getAmplitude(Frequency frequency, long sampleCount) {
        //todo create infrastructure (just correctly named methods) to move 2*Pi here as well
        double timeAndAngleComponent = sampleRate.asTime(sampleCount).getValue() * angleComponent;
        return getWave(frequency).getAmplitudePrecalculated(timeAndAngleComponent);
    }
}
