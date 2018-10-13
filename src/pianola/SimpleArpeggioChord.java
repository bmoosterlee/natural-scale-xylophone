package pianola;

import frequency.Frequency;
import gui.GUI;
import gui.spectrum.SpectrumWindow;
import gui.spectrum.state.SpectrumManager;
import gui.spectrum.state.SpectrumState;
import main.BoundedBuffer;

import java.util.HashSet;
import java.util.Set;

public class SimpleArpeggioChord extends SimpleArpeggio {
    private ChordPlayer chordPlayer;

    public SimpleArpeggioChord(BoundedBuffer<SpectrumState> buffer, int chordSize, SpectrumWindow spectrumWindow) {
        super(buffer, chordSize, spectrumWindow);
    }

    @Override
    public Set<Frequency> playPattern() {
        Set<Frequency> frequencies  = new HashSet<>();

        try {
            frequencies.addAll(super.playPattern());

            if (arpeggiateUp.sequencer.i == 0) {
                Set<Frequency> chord = chordPlayer.playPattern();
                for (Frequency frequency : chord) {
                    frequencies.add(frequency.divideBy(4.));
                }
            }
        }
        catch(NullPointerException e){

        }

        return frequencies;
    }

    @Override
    protected void generateNewChord() {
        super.generateNewChord();
        chordPlayer = new ChordPlayer(simpleChordGenerator.getFrequencies());
    }

}