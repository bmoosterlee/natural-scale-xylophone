package notes;

import frequency.Frequency;
import notes.envelope.Envelope;

public class Note {
    private final Frequency frequency;
    private final long startingSampleCount;

    public Note(Frequency frequency, long startingSampleCount){
        if(frequency==null) throw new NullPointerException();

        this.frequency = frequency;
        this.startingSampleCount = startingSampleCount;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public long getStartingCount() {
        return startingSampleCount;
    }
}
