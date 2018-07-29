import javafx.util.Pair;

import java.util.*;

public class HarmonicCalculator {
    
    private final HarmonicBuffer harmonicBuffer = new HarmonicBuffer();

    PriorityQueue<Pair<MemoableIterator, Double>> iteratorHierarchy;
    private HashMap<MemoableIterator, Note> lookupNotesFromIterators;
    private HashMap<Note, Double> volumeTable;

    public HarmonicCalculator(NoteEnvironment noteEnvironment){
        lookupNotesFromIterators = new HashMap<>();
        volumeTable = new HashMap<>();
        iteratorHierarchy = new PriorityQueue<>((o1, o2) -> -Double.compare(
                Harmonic.getHarmonicValue(o1.getValue(), o1.getKey().currentHarmonicAsFraction),
                Harmonic.getHarmonicValue(o2.getValue(), o2.getKey().currentHarmonicAsFraction))
        );

        noteEnvironment.addNoteObservable.addObserver((Observer<Note>) event -> addNote(event));
        noteEnvironment.removeNoteObservable.addObserver((Observer<Note>) event -> removeNote(event));
    }

    private void addNewHarmonicsToBuffer(HashMap<Note, Double> volumeTable) {
        double newHarmonicVolume;
        double highestPriorityHarmonicVolume;
        do {
            newHarmonicVolume = getNewHarmonicVolume(volumeTable);
            highestPriorityHarmonicVolume = harmonicBuffer.getHighestPriorityHarmonicVolume();
        } while(newHarmonicVolume > highestPriorityHarmonicVolume);
    }

    private double getNewHarmonicVolume(HashMap<Note, Double> volumeTable) {
        try {
            Pair<MemoableIterator, Double> highestValueIteratorPair = iteratorHierarchy.poll();
            MemoableIterator highestValueMemoableIterator = highestValueIteratorPair.getKey();
            Double noteVolume = highestValueIteratorPair.getValue();
            Fraction nextHarmonicAsFraction = highestValueMemoableIterator.next();
            iteratorHierarchy.add(new Pair(highestValueMemoableIterator, noteVolume));

            Note highestValueNote = lookupNotesFromIterators.get(highestValueMemoableIterator);
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

    public LinkedList<Pair<Harmonic, Double>> getCurrentHarmonicHierarchy(long currentSampleCount, HashSet<Note> liveNotes) {
        HashMap<Note, Double> volumeTable = getVolumes(currentSampleCount, liveNotes);

        rebuildIteratorHierarchy(volumeTable, liveNotes);

        harmonicBuffer.rebuildHarmonicHierarchy(volumeTable);

        addNewHarmonicsToBuffer(volumeTable);

        return harmonicBuffer.getHarmonicHierarchy();
    }

    private void rebuildIteratorHierarchy(HashMap<Note, Double> volumeTable, HashSet<Note> liveNotes) {
        synchronized (iteratorHierarchy) {
            Pair<MemoableIterator, Double>[] iterators = iteratorHierarchy.toArray(new Pair[iteratorHierarchy.size()]);
            iteratorHierarchy.clear();
            for (Pair<MemoableIterator, Double> comparableIteratorPair : iterators) {
                MemoableIterator MemoableIterator = comparableIteratorPair.getKey();
                Note note = lookupNotesFromIterators.get(MemoableIterator);

                Double noteVolume = volumeTable.get(note);
                if(noteVolume==null) {
                    lookupNotesFromIterators.remove(MemoableIterator);
                }
                else{
                    iteratorHierarchy.add(new Pair(MemoableIterator, noteVolume));
                }
            }
        }
    }

    private HashMap<Note, Double> getVolumes(long currentSampleCount, HashSet<Note> liveNotes) {
        HashMap<Note, Double> newVolumeTable = new HashMap<>();
        for(Note note : liveNotes) {
            newVolumeTable.put(note, note.getVolume(currentSampleCount));
        }
        return newVolumeTable;
    }

    private void removeNote(Note note) {
        harmonicBuffer.removeNote(note);
        volumeTable.remove(note);
    }

    private void addNote(Note note) {
        synchronized(iteratorHierarchy) {
            MemoableIterator MemoableIterator = new MemoableIterator();
            lookupNotesFromIterators.put(MemoableIterator, note);
//          calculate note volumes and pair them with their note
            iteratorHierarchy.add(new Pair(MemoableIterator, 0.));
            volumeTable.put(note, 0.);
            harmonicBuffer.addNote(note);
        }
    }

}
