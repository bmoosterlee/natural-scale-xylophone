package pianola;/*todo write a history tracker of when notes were played. Take average of notes played to form smoothed peaks
 * todo find maxima of these smoothed values. Perform fourier analysis on this signal, also obtaining the phases of
 * todo a frequency. find harmonic image of each frequency, multiply image by the phase of the tonic.
 * todo add all these phase dependent harmonic images together, and display the rhythmic value in real time.
 * todo let the pianola take the rhythmic harmonic value in real time and play that.
 * todo use a lookahead of the length of the shortest frame for the pianola, and play the maximum within that frame*/

import frequency.Frequency;
import gui.buckets.*;
import main.BoundedBuffer;
import main.InputPort;
import main.OutputPort;
import main.Pulse;
import pianola.patterns.PianolaPattern;

public class Pianola implements Runnable{
    private final PianolaPattern pianolaPattern;

    private final InputPort<Buckets> notesInput;
    private BucketHistory noteHistory;
    private final int repetitionDampener;
    private final OutputPort<Buckets> averagerInput;

    private final InputPort<Pulse> pulseInput;
    private final InputPort<Buckets> preparedNotesInput;
    private final InputPort<Buckets> preparedHarmonicsInput;
    private final OutputPort<Frequency> playedNotes;

    public Pianola(PianolaPattern pianolaPattern, BoundedBuffer<Pulse> inputBuffer, BoundedBuffer<Frequency> outputBuffer, BoundedBuffer<Buckets> pianolaNotesBucketsBuffer, BoundedBuffer<Buckets> pianolaHarmonicsBucketsBuffer, int inaudibleFrequencyMargin) {
        this.pianolaPattern = pianolaPattern;
        
        int count = 0;
        int capacity = 1000;

        repetitionDampener = 3;
        noteHistory = new PrecalculatedBucketHistory(50);

        BoundedBuffer<Buckets> notesAveragerInputBuffer = new BoundedBuffer<>(capacity, "Pianola" + String.valueOf(count)); count++;
        BoundedBuffer<Buckets> notesAveragerOutputBuffer = new BoundedBuffer<>(capacity, "Pianola" + String.valueOf(count)); count++;
        new BucketsAverager(2* inaudibleFrequencyMargin, notesAveragerInputBuffer, notesAveragerOutputBuffer);
        averagerInput = new OutputPort<>(notesAveragerInputBuffer);

        BoundedBuffer<Buckets> harmonicsAveragerBuffer = new BoundedBuffer<>(capacity, "Pianola" + String.valueOf(count)); count++;
        new BucketsAverager(inaudibleFrequencyMargin, pianolaHarmonicsBucketsBuffer, harmonicsAveragerBuffer);

        pulseInput = new InputPort<>(inputBuffer);
        notesInput = new InputPort<>(pianolaNotesBucketsBuffer);
        preparedNotesInput = new InputPort<>(notesAveragerOutputBuffer);
        preparedHarmonicsInput = new InputPort<>(harmonicsAveragerBuffer);
        playedNotes = new OutputPort<>(outputBuffer);

        start();
    }

    private void start() {
        new Thread(this).start();
    }


    @Override
    public void run() {
        while(true){
            tick();
        }
    }

    private void tick() {
        try {
            pulseInput.consume();

            Buckets origNoteBuckets;
            try {
                origNoteBuckets = notesInput.consume();
            }
            catch(NullPointerException e){
                origNoteBuckets = new Buckets();
            }
            noteHistory = noteHistory.addNewBuckets(origNoteBuckets);

            Buckets notesBeforeAveraging = noteHistory.getTimeAveragedBuckets().multiply(repetitionDampener);
            averagerInput.produce(notesBeforeAveraging);

            Buckets noteBuckets = preparedNotesInput.consume();
            Buckets harmonicsBuckets = preparedHarmonicsInput.consume();

            for(Frequency frequency : pianolaPattern.playPattern(noteBuckets, harmonicsBuckets)){
                try {
                    playedNotes.produce(frequency);
                }
                catch(NullPointerException ignored) {
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
