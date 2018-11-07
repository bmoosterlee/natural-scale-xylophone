package notes.state;

import frequency.Frequency;
import main.*;
import notes.envelope.DeterministicEnvelope;
import notes.envelope.Envelope;
import sound.SampleRate;
import time.PerformanceTracker;
import time.TimeKeeper;
import wave.Wave;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;


public class VolumeAmplitudeCalculator implements Runnable {
    private final SampleRate sampleRate;

    private final Map<Long, Collection<EnvelopeForFrequency>> unfinishedSlices;
    private final Map<Long, Map<Frequency, Wave>> unfinishedSlicesWaves;
    private final Map<Long, VolumeAmplitudeState> finishedSlices;

    private final InputPort<TimestampedNewNotesWithEnvelope> input;
    private final OutputPort<VolumeAmplitudeState> output;

    private final OutputPort<SimpleImmutableEntry<DeterministicEnvelope, Collection<Frequency>>> addNewNotesOutputPort;
    private final InputPort<Collection<EnvelopeForFrequency>> addNewNotesInputPort;
    private final OutputPort<Collection<EnvelopeForFrequency>> groupEnvelopesByFrequencyOutputPort;
    private final OutputPort<Long> sampleCountOutputPort;
    private final OutputPort<Map<Frequency, Wave>> waveOutputPort;
    private final InputPort<Map<Frequency, VolumeAmplitude>> sumValuesPerFrequencyInputPort;

    public VolumeAmplitudeCalculator(BoundedBuffer<TimestampedNewNotesWithEnvelope> inputBuffer, BoundedBuffer<VolumeAmplitudeState> outputBuffer, SampleRate sampleRate){
        this.sampleRate = sampleRate;

        unfinishedSlices = new HashMap<>();
        unfinishedSlicesWaves = new HashMap<>();
        finishedSlices = new HashMap<>();

        this.input = new InputPort<>(inputBuffer);
        this.output = new OutputPort<>(outputBuffer);

        SimpleImmutableEntry<OutputPort<SimpleImmutableEntry<DeterministicEnvelope, Collection<Frequency>>>, InputPort<Collection<EnvelopeForFrequency>>> addNewNotesPorts = PipeComponent.methodToComponentPorts(addNewNotesInput -> toEnvelopesForFrequencies(addNewNotesInput.getKey(), addNewNotesInput.getValue()), 10000, "addNewNotes");
        addNewNotesOutputPort = addNewNotesPorts.getKey();
        addNewNotesInputPort = addNewNotesPorts.getValue();

        BoundedBuffer<Collection<EnvelopeForFrequency>> groupEnvelopesByFrequencyInputBuffer = new BoundedBuffer<>(10000, "groupEnvelopesByFrequency - input");
        groupEnvelopesByFrequencyOutputPort = new OutputPort<>(groupEnvelopesByFrequencyInputBuffer);
        BoundedBuffer<Map<Frequency, Collection<Envelope>>> groupEnvelopesByFrequencyOutputBuffer = new BoundedBuffer<>(10000, "groupEnvelopesByFrequency - output");
        new PipeComponent<>(groupEnvelopesByFrequencyInputBuffer, groupEnvelopesByFrequencyOutputBuffer, VolumeAmplitudeCalculator::groupEnvelopesByFrequency);

        BoundedBuffer<Long> sampleCountBuffer = new BoundedBuffer<>(10000, "calculateValuesPerFrequency - sampleCount");
        sampleCountOutputPort = new OutputPort<>(sampleCountBuffer);
        BoundedBuffer<Map<Frequency, Wave>> waveBuffer = new BoundedBuffer<>(10000, "calculateValuesPerFrequency - waves");
        waveOutputPort = new OutputPort<>(waveBuffer);

        BoundedBuffer<SimpleImmutableEntry<Map<Frequency, Collection<Envelope>>, Map<Frequency, Wave>>> pair1Buffer = new BoundedBuffer<>(10000, "pair1");
        new Pairer<>(groupEnvelopesByFrequencyOutputBuffer, waveBuffer, pair1Buffer);
        BoundedBuffer<SimpleImmutableEntry<Long, SimpleImmutableEntry<Map<Frequency, Collection<Envelope>>, Map<Frequency, Wave>>>> pair2Buffer = new BoundedBuffer<>(10000, "pair2");
        new Pairer<>(sampleCountBuffer, pair1Buffer, pair2Buffer);

        BoundedBuffer<EnvelopeWaveSlice> pairToParamObjectOutputBuffer = new BoundedBuffer<>(10000, "pair to paramObject - output");
        new PipeComponent<>(pair2Buffer, pairToParamObjectOutputBuffer, input -> new EnvelopeWaveSlice(input.getKey(), input.getValue().getKey(), input.getValue().getValue()));

        BoundedBuffer<Map<Frequency, Collection<VolumeAmplitude>>> calculateValuesPerFrequencyOutputBuffer = new BoundedBuffer<>(10000, "calculateValuesPerFrequency - output");
        new PipeComponent<>(pairToParamObjectOutputBuffer, calculateValuesPerFrequencyOutputBuffer, VolumeAmplitudeCalculator::calculateValuesPerFrequency);

        BoundedBuffer<Map<Frequency, VolumeAmplitude>> sumValuesPerFrequencyOutputBuffer = new BoundedBuffer<>(10000, "sumValuesPerFrequency - output");
        new PipeComponent<>(calculateValuesPerFrequencyOutputBuffer, sumValuesPerFrequencyOutputBuffer, VolumeAmplitudeCalculator::sumValuesPerFrequency);
        sumValuesPerFrequencyInputPort = new InputPort<>(sumValuesPerFrequencyOutputBuffer);

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
            TimestampedNewNotesWithEnvelope timestampedNewNotesWithEnvelope = input.consume();

            long sampleCount = timestampedNewNotesWithEnvelope.getSampleCount();
            DeterministicEnvelope envelope = timestampedNewNotesWithEnvelope.getEnvelope();
            Collection<Frequency> newNotes = timestampedNewNotesWithEnvelope.getFrequencies();

            TimeKeeper timeKeeper = PerformanceTracker.startTracking("calculate volumes");
            addNewNotes(sampleCount, envelope, newNotes);
            VolumeAmplitudeState currentState = finish(sampleCount);

//            precalculateInBackground();
            PerformanceTracker.stopTracking(timeKeeper);

            output.produce(currentState);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void precalculateInBackground() {
        while(input.isEmpty()){
            try {
                Long futureSampleCount = unfinishedSlices.keySet().iterator().next();
                VolumeAmplitudeState finishedSlice = finish(futureSampleCount);
                finishedSlices.put(futureSampleCount, finishedSlice);
            }
            catch(NoSuchElementException e){
                break;
            }
        }
    }

    private VolumeAmplitudeState finish(long sampleCount) {
        try {
            Collection<EnvelopeForFrequency> currentUnfinishedSlices = unfinishedSlices.remove(sampleCount);

            try {
                groupEnvelopesByFrequencyOutputPort.produce(currentUnfinishedSlices);
            }
            catch(NullPointerException e){
                groupEnvelopesByFrequencyOutputPort.produce(new LinkedList<>());
            }

            sampleCountOutputPort.produce(sampleCount);
            Map<Frequency, Wave> currentUnfinishedSliceWaves = unfinishedSlicesWaves.remove(sampleCount);
            try {
                waveOutputPort.produce(currentUnfinishedSliceWaves);
            }
            catch(NullPointerException e){
                waveOutputPort.produce(new HashMap<>());
            }

            Map<Frequency, VolumeAmplitude> newVolumeAmplitudes = sumValuesPerFrequencyInputPort.consume();

            return totalFinishedSlices(sampleCount, newVolumeAmplitudes);

        } catch (InterruptedException e) {
            e.printStackTrace();

            return null;
        }
    }

    private VolumeAmplitudeState totalFinishedSlices(long sampleCount, Map<Frequency, VolumeAmplitude> newVolumeAmplitudes) {
        VolumeAmplitudeState oldVolumeState = finishedSlices.remove(sampleCount);
        VolumeAmplitudeState totalVolumeState;
        try {
            totalVolumeState = oldVolumeState.add(newVolumeAmplitudes);
        }
        catch(NullPointerException e){
            totalVolumeState = new VolumeAmplitudeState(sampleCount, newVolumeAmplitudes);
        }

        return totalVolumeState;
    }

    private static Map<Frequency, VolumeAmplitude> sumValuesPerFrequency(Map<Frequency, Collection<VolumeAmplitude>> newVolumeAmplitudeCollections) {
        Map<Frequency, VolumeAmplitude> newVolumeAmplitudes = new HashMap<>();
        for(Frequency frequency : newVolumeAmplitudeCollections.keySet()) {
            Collection<VolumeAmplitude> volumeAmplitudeCollection = newVolumeAmplitudeCollections.get(frequency);
            VolumeAmplitude totalVolumeAmplitude = VolumeAmplitude.sum(volumeAmplitudeCollection);
            newVolumeAmplitudes.put(frequency, totalVolumeAmplitude);
        }
        return newVolumeAmplitudes;
    }

    private static Map<Frequency, Collection<VolumeAmplitude>> calculateValuesPerFrequency(EnvelopeWaveSlice envelopeWaveSlice) {
        long sampleCount = envelopeWaveSlice.getSampleCount();
        Map<Frequency, Collection<Envelope>> envelopesPerFrequency = envelopeWaveSlice.getEnvelopesPerFrequency();
        Map<Frequency, Wave> wavesPerFrequency = envelopeWaveSlice.getWavesPerFrequency();

        Map<Frequency, Collection<VolumeAmplitude>> newVolumeAmplitudeCollections = new HashMap<>();
        for (Frequency frequency : envelopesPerFrequency.keySet()) {
            Collection<Envelope> envelopes = envelopesPerFrequency.get(frequency);
            Wave wave = wavesPerFrequency.get(frequency);
            double amplitude = wave.getAmplitude(sampleCount);

            Collection<VolumeAmplitude> volumeAmplitudes = new LinkedList<>();
            for(Envelope envelope : envelopes) {
                double volume = envelope.getVolume(sampleCount);
                VolumeAmplitude volumeAmplitude = new VolumeAmplitude(volume, amplitude);
                volumeAmplitudes.add(volumeAmplitude);
            }

            newVolumeAmplitudeCollections.put(frequency, volumeAmplitudes);
        }
        return newVolumeAmplitudeCollections;
    }

    private static Map<Frequency, Collection<Envelope>> groupEnvelopesByFrequency(Collection<EnvelopeForFrequency> envelopesForFrequencies) {
        Map<Frequency, Collection<Envelope>> currentUnfinishedSlicesPerFrequency = new HashMap<>();
        if(envelopesForFrequencies!=null) {
            for (EnvelopeForFrequency envelopeForFrequency : envelopesForFrequencies) {
                Frequency frequency = envelopeForFrequency.getFrequency();
                Envelope envelope = envelopeForFrequency.getEnvelope();
                Collection<Envelope> envelopesForFrequency = currentUnfinishedSlicesPerFrequency.get(frequency);
                try {
                    envelopesForFrequency.add(envelope);
                } catch (NullPointerException e) {
                    envelopesForFrequency = new LinkedList<>();
                    envelopesForFrequency.add(envelope);
                    currentUnfinishedSlicesPerFrequency.put(frequency, envelopesForFrequency);
                }
            }
        }
        return currentUnfinishedSlicesPerFrequency;
    }

    //todo there might be duplicate frequencies added at a timestamp. Group by frequency as well.
    //todo combine unfinishedSlice and unfinishedSliceWaves into one object
    private void addNewNotes(Long sampleCount, DeterministicEnvelope envelope, Collection<Frequency> newNotes) {
        Collection<EnvelopeForFrequency> newNotesWithEnvelopes;
        try {
            addNewNotesOutputPort.produce(new SimpleImmutableEntry<>(envelope, newNotes));
            newNotesWithEnvelopes = addNewNotesInputPort.consume();

            Map<Frequency, Wave> newNoteWaves = reuseOrCreateNewWaves(newNotes);

            long endingSampleCount = envelope.getEndingSampleCount();
            addNewEnvelopes(sampleCount, endingSampleCount, newNotesWithEnvelopes);
            addNewWaves(sampleCount, endingSampleCount, newNotes, newNoteWaves);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Collection<EnvelopeForFrequency> toEnvelopesForFrequencies(DeterministicEnvelope envelope, Collection<Frequency> frequencies) {
        Collection<EnvelopeForFrequency> newNotesWithEnvelopes = new LinkedList<>();
        for(Frequency frequency : frequencies){
            newNotesWithEnvelopes.add(new EnvelopeForFrequency(frequency, envelope));
        }
        return newNotesWithEnvelopes;
    }

    private void addNewWaves(Long sampleCount, Long endingSampleCount, Collection<Frequency> newNotes, Map<Frequency, Wave> newNoteWaves) {
        if(newNotes.isEmpty()){
            return;
        }
        for (Long i = sampleCount; i < endingSampleCount; i++) {
            Map<Frequency, Wave> oldUnfinishedSliceWaves = unfinishedSlicesWaves.get(i);
            for(Frequency frequency : newNotes){
                Wave wave = newNoteWaves.get(frequency);
                try{
                    oldUnfinishedSliceWaves.putIfAbsent(frequency, wave);
                }
                catch(NullPointerException e){
                    Map<Frequency, Wave> newUnfinishedSliceWaves = new HashMap<>();
                    newUnfinishedSliceWaves.put(frequency, wave);
                    unfinishedSlicesWaves.put(i, newUnfinishedSliceWaves);
                }
            }
        }
    }

    private void addNewEnvelopes(Long sampleCount, Long endingSampleCount, Collection<EnvelopeForFrequency> newNotesWithEnvelopes) {
        if(newNotesWithEnvelopes.isEmpty()){
            return;
        }
        for (Long i = sampleCount; i < endingSampleCount; i++) {
            Collection<EnvelopeForFrequency> oldUnfinishedSlice = unfinishedSlices.get(i);
            try {
                oldUnfinishedSlice.addAll(newNotesWithEnvelopes);
            } catch (NullPointerException e) {
                Collection<EnvelopeForFrequency> newUnfinishedSlice = new LinkedList<>(newNotesWithEnvelopes);
                unfinishedSlices.put(i, newUnfinishedSlice);
            }
        }
    }

    private Map<Frequency, Wave> reuseOrCreateNewWaves(Collection<Frequency> newNotes) {
        Map<Frequency, Wave> newNoteWaves = new HashMap<>();
        Set<Frequency> missingWaveFrequencies = new HashSet<>(newNotes);
        for (Long i : unfinishedSlicesWaves.keySet()) {
            Map<Frequency, Wave> oldUnfinishedSliceWaves = unfinishedSlicesWaves.get(i);

            Set<Frequency> foundWaveFrequencies = new HashSet<>(missingWaveFrequencies);
            foundWaveFrequencies.retainAll(oldUnfinishedSliceWaves.keySet());

            for (Frequency frequency : foundWaveFrequencies) {
                newNoteWaves.put(frequency, oldUnfinishedSliceWaves.get(frequency));
            }

            missingWaveFrequencies.removeAll(foundWaveFrequencies);
        }
        for(Frequency frequency : missingWaveFrequencies){
            Wave newWave = new Wave(frequency, sampleRate);
            newNoteWaves.put(frequency, newWave);
        }
        return newNoteWaves;
    }

}
