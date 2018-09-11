package notes.state;

import main.SampleRate;
import notes.Frequency;
import notes.Wave;

import java.util.*;

public class WaveState {
    SampleRate sampleRate;
    Set<Frequency> frequencies;
    Set<Wave> waves;
    Map<Frequency, Wave> frequencyWaveTable;

    public WaveState(SampleRate sampleRate){
        this.sampleRate = sampleRate;
        frequencies = new HashSet<>();
        waves = new HashSet<>();
        frequencyWaveTable = new HashMap<>();
    }

    public WaveState(SampleRate sampleRate, Set<Frequency> frequencies, Set<Wave> waves, Map<Frequency, Wave> frequencyWaveTable) {
        this.sampleRate = sampleRate;
        this.frequencies = frequencies;
        this.waves = waves;
        this.frequencyWaveTable = frequencyWaveTable;
    }

    public WaveState remove(Frequency frequency) {
        Set<Frequency> newFrequencies = new HashSet<>(frequencies);
        Set<Wave> newWaves = new HashSet<>(waves);
        Map<Frequency, Wave> newFrequencyWaveTable = new HashMap<>(frequencyWaveTable);

        newFrequencies.remove(frequency);
        newWaves.remove(frequencyWaveTable.get(frequency));
        newFrequencyWaveTable.remove(frequency);

        return new WaveState(sampleRate, newFrequencies, newWaves, newFrequencyWaveTable);
    }

    public WaveState add(Frequency frequency) {
        Set<Frequency> newFrequencies = new HashSet<>(frequencies);
        Set<Wave> newWaves = new HashSet<>(waves);
        Map<Frequency, Wave> newFrequencyWaveTable = new HashMap<>(frequencyWaveTable);

        if(!frequencyWaveTable.containsKey(frequency)) {
            newFrequencies.add(frequency);
            Wave wave = new Wave(frequency, sampleRate);
            newWaves.add(wave);
            newFrequencyWaveTable.put(frequency, wave);
        }

        return new WaveState(sampleRate, newFrequencies, newWaves, newFrequencyWaveTable);
    }

    public WaveState update(Set<Frequency> updatedFrequencies) {
        WaveState newWaveState = this;

        Set<Frequency> removedFrequencies = this.frequencies;
        removedFrequencies.removeAll(new HashSet<>(updatedFrequencies));

        Set<Frequency> addedFrequencies = new HashSet<>(updatedFrequencies);
        addedFrequencies.removeAll(this.frequencies);

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
}
