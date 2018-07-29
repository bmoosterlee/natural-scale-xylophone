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

        noteEnvironment.addNoteObservable.addObserver((Observer<Note>) event -> addNote(event));
        noteEnvironment.removeNoteObservable.addObserver((Observer<Note>) event -> removeNote(event));
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
        HashMap<Note, Double> newVolumeTable = getVolumes(currentSampleCount);
        synchronized(volumeTable){
            volumeTable = newVolumeTable;
        }

        synchronized (iteratorHierarchy) {
            ComparableIterator[] iterators = iteratorHierarchy.toArray(new ComparableIterator[iteratorHierarchy.size()]);
            iteratorHierarchy.clear();
            for (ComparableIterator comparableIterator : iterators) {
                Note note = lookupNotesFromIterators.get(comparableIterator);

                Double noteVolume = newVolumeTable.get(note);
                if(noteVolume==null) {
                    lookupNotesFromIterators.remove(comparableIterator);
                }
                else{
                    comparableIterator.noteVolume = noteVolume;
                    iteratorHierarchy.add(comparableIterator);
                }
            }
        }

        harmonicBuffer.rebuildPriorityQueue(volumeTable);
    }

    private HashMap<Note, Double> getVolumes(long currentSampleCount) {
        HashMap<Note, Double> newVolumeTable = new HashMap<>();
        synchronized (iteratorHierarchy) {
            for (ComparableIterator comparableIterator : iteratorHierarchy) {
                Note note = lookupNotesFromIterators.get(comparableIterator);
                newVolumeTable.put(note, noteEnvironment.getVolume(note, currentSampleCount));
            }
        }
        return newVolumeTable;
    }

    private void removeNote(Note note) {
        harmonicBuffer.removeNote(note);
        volumeTable.remove(note);
    }

    private void addNote(Note note) {
        synchronized(iteratorHierarchy) {
            ComparableIterator comparableIterator = new ComparableIterator(0);
            lookupNotesFromIterators.put(comparableIterator, note);
//          calculate note volumes and pair them with their note
            iteratorHierarchy.add(comparableIterator);
            volumeTable.put(note, 0.);
            harmonicBuffer.addNote(note);
        }
    }

}
