package notes.state;

import main.SampleRate;
import notes.Frequency;
import notes.Wave;

import java.util.*;

public class WaveState {
    SampleRate sampleRate;
    Set<Frequency> frequencies;
    Map<Frequency, Wave> frequencyWaveTable;

    public WaveState(SampleRate sampleRate){
        this.sampleRate = sampleRate;
        frequencies = new HashSet<>();
        frequencyWaveTable = new HashMap<>();
    }

    public WaveState(SampleRate sampleRate, Set<Frequency> frequencies, Map<Frequency, Wave> frequencyWaveTable) {
        this.sampleRate = sampleRate;
        this.frequencies = frequencies;
        this.frequencyWaveTable = frequencyWaveTable;
    }

    public WaveState remove(Frequency frequency) {
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

    public WaveState update(Set<Frequency> updatedFrequencies) {
        WaveState newWaveState = this;

        Set<Frequency> removedFrequencies = new HashSet<>(this.frequencies);
        removedFrequencies.removeAll(new HashSet<>(updatedFrequencies));

        Set<Frequency> addedFrequencies = new HashSet<>(updatedFrequencies);
        addedFrequencies.removeAll(new HashSet<>(this.frequencies));

        if(!removedFrequencies.isEmpty()) {
            Iterator<Frequency> iterator = removedFrequencies.iterator();
            while (iterator.hasNext()) {
                newWaveState = newWaveState.remove(iterator.next());
            }
        }

        if(!addedFrequencies.isEmpty()) {
            Iterator<Frequency> iterator = addedFrequencies.iterator();
            while (iterator.hasNext()) {
                newWaveState = newWaveState.add(iterator.next());
            }
        }

        return newWaveState;
    }

    public Wave getWave(Frequency frequency) {
        return frequencyWaveTable.get(frequency);
    }
}
