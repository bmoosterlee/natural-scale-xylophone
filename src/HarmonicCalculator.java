import javafx.util.Pair;

import java.util.*;

public class HarmonicCalculator {

    private final NoteEnvironment noteEnvironment;
    private final HarmonicBuffer harmonicBuffer = new HarmonicBuffer();

    PriorityQueue<ComparableIterator> iteratorHierarchy;
    private HashMap<ComparableIterator, Note> lookupNotesFromIterators;
    private HashMap<Note, Double> volumeTable;

    public HarmonicCalculator(NoteEnvironment noteEnvironment){
        this.noteEnvironment = noteEnvironment;
        lookupNotesFromIterators = new HashMap<>();
        volumeTable = new HashMap<>();
        iteratorHierarchy = new PriorityQueue<>();
    }

    public Pair<Harmonic, Double> getNextHarmonicVolumePair() {
        addNewHarmonicsToBuffer();

        try {
            return harmonicBuffer.getHighestValueHarmonic();
        }
        catch(NullPointerException e) {
            return null;
        }
    }

    private void addNewHarmonicsToBuffer() {
        double newHarmonicVolume;
        double highestPriorityHarmonicVolume;
        do {
            newHarmonicVolume = getNewHarmonicVolume();
            highestPriorityHarmonicVolume = harmonicBuffer.getHighestPriorityHarmonicVolume();
        } while(newHarmonicVolume > highestPriorityHarmonicVolume);
    }

    private double getNewHarmonicVolume() {
        try {
            ComparableIterator highestValueComparableIterator = iteratorHierarchy.poll();
            Fraction nextHarmonicAsFraction = highestValueComparableIterator.next();
            iteratorHierarchy.add(highestValueComparableIterator);

            Note highestValueNote = lookupNotesFromIterators.get(highestValueComparableIterator);
            Harmonic highestValueHarmonic = new Harmonic(highestValueNote, nextHarmonicAsFraction, volumeTable.get(highestValueNote));

//            compare this harmonic to the highest value harmonic from the previousHighHarmonicsVolume priorityqueue
            double newHarmonicVolume = highestValueHarmonic.getVolume(volumeTable.get(highestValueNote));

            harmonicBuffer.addHarmonicToHarmonicBuffer(newHarmonicVolume, highestValueNote, highestValueHarmonic);

            return newHarmonicVolume;
        }
        catch(NullPointerException e){
            return 0.;
        }
    }

    public void reset(long currentSampleCount) {
        lookupNotesFromIterators.clear();
        volumeTable.clear();
        iteratorHierarchy.clear();
        harmonicBuffer.previousHighHarmonicsVolume.clear();

        HashSet<Note> encounteredNotes = new HashSet<>(harmonicBuffer.notesForPreviousHighHarmonics.keySet());
        HashSet<Note> liveNotes = noteEnvironment.getLiveNotes();

        HashSet<Note> newNotes = (HashSet<Note>)(liveNotes.clone());
        newNotes.removeAll(encounteredNotes);

        HashSet<Note> deadNotes = (HashSet<Note>)(encounteredNotes.clone());
        deadNotes.removeAll(liveNotes);


        for(Note note : newNotes) {
            double volume = noteEnvironment.getVolume(note, currentSampleCount);
            ComparableIterator comparableIterator = new ComparableIterator(volume);
            lookupNotesFromIterators.put(comparableIterator, note);
//          calculate note volumes and pair them with their note
            iteratorHierarchy.add(comparableIterator);
        }

        for(Note note : deadNotes){
//            remove dead notes here based on volume
            for (Harmonic harmonic : harmonicBuffer.notesForPreviousHighHarmonics.get(note)) {
                harmonicBuffer.previousHighHarmonics.remove(harmonic);
                harmonicBuffer.previousHighHarmonicNotes.remove(harmonic);
                harmonicBuffer.previousHighHarmonicsVolume.remove(harmonic);
            }

            harmonicBuffer.notesForPreviousHighHarmonics.remove(note);
        }

        for(Note note : liveNotes) {
//          calculate note volumes and pair them with their note
            double volume = noteEnvironment.getVolume(note, currentSampleCount);
            volumeTable.put(note, volume);

            ComparableIterator[] iterators = iteratorHierarchy.toArray(new ComparableIterator[iteratorHierarchy.size()]);
            iteratorHierarchy.clear();
            for(ComparableIterator comparableIterator : iterators) {
                comparableIterator.noteVolume = volume;
                iteratorHierarchy.add(comparableIterator);
            }
        }

//            calculate harmonic volumes using note volumes
//            store volumes in a pair with the harmonic in a priorityqueue
        for(Harmonic harmonic : harmonicBuffer.previousHighHarmonics) {
            Note note = harmonicBuffer.previousHighHarmonicNotes.get(harmonic);
            harmonicBuffer.previousHighHarmonicsVolume.add(
                    new Pair(harmonic,
                            harmonic.getVolume(volumeTable.get(note))));
        }
    }

}
