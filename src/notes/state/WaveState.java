package notes.state;

import notes.Frequency;
import notes.Wave;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WaveState {

    Set<Wave> waves;
    Map<Frequency, Wave> frequencyWaveTable;

    public WaveState(){
        waves = new HashSet<>();
        frequencyWaveTable = new HashMap<>();
    }

    public WaveState(WaveState waveState){
        waves = new HashSet<>(waveState.waves);
        frequencyWaveTable = new HashMap<>(waveState.frequencyWaveTable);
    }

    public WaveState(Set<Wave> waves, Map<Frequency, Wave> frequencyWaveTable) {
        this.waves = waves;
        this.frequencyWaveTable = frequencyWaveTable;
    }

    public WaveState remove(Frequency frequency) {
        Set<Wave> newWaves = new HashSet<>(waves);
        Map<Frequency, Wave> newFrequencyWaveTable = new HashMap<>(frequencyWaveTable);

        newWaves.remove(frequencyWaveTable.get(frequency));
        newFrequencyWaveTable.remove(frequency);

        return new WaveState(newWaves, newFrequencyWaveTable);
    }

    public WaveState add(Frequency frequency) {
        Set<Wave> newWaves = new HashSet<>(waves);
        Map<Frequency, Wave> newFrequencyWaveTable = new HashMap<>(frequencyWaveTable);

        if(!frequencyWaveTable.containsKey(frequency)) {
            Wave wave = new Wave(frequency);
            newWaves.add(wave);
            newFrequencyWaveTable.put(frequency, wave);
        }

        return new WaveState(newWaves, newFrequencyWaveTable);
    }
}
