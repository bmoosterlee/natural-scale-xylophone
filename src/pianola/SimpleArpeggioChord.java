package pianola;

import frequency.Frequency;
import gui.buckets.Buckets;
import gui.spectrum.SpectrumWindow;
import main.BoundedBuffer;

import java.util.HashSet;
import java.util.Set;

public class SimpleArpeggioChord extends SimpleArpeggio {
    private ChordPlayer chordPlayer;

    public SimpleArpeggioChord(BoundedBuffer<Buckets> harmonicsBuffer, BoundedBuffer<Buckets> notesBuffer, int chordSize, SpectrumWindow spectrumWindow) {
        super(notesBuffer, harmonicsBuffer, chordSize, spectrumWindow);
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
        catch(NullPointerException ignored){

        }

        return frequencies;
    }

    @Override
    protected void generateNewChord() {
        super.generateNewChord();
        chordPlayer = new ChordPlayer(simpleChordGenerator.getFrequencies());
    }

}