package mixer;

import component.buffer.*;
import component.utilities.RunningPipeComponent;
import frequency.Frequency;
import mixer.envelope.DeterministicEnvelope;
import mixer.envelope.Envelope;
import mixer.state.*;
import sound.SampleRate;
import mixer.state.Wave;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.stream.Collectors;

public class Mixer extends RunningPipeComponent {

    public Mixer(BoundedBuffer<Long> sampleCountBuffer, BoundedBuffer<Frequency> noteInputBuffer, SimpleBuffer<VolumeAmplitudeState> outputBuffer, SampleRate sampleRate){
        super(sampleCountBuffer, outputBuffer, build(noteInputBuffer, sampleRate));
    }

    public static CallableWithArguments<Long, VolumeAmplitudeState> build(BoundedBuffer<Frequency> noteInputBuffer, SampleRate sampleRate){
        return new CallableWithArguments<>() {
            private Map<Long, Collection<EnvelopeForFrequency>> unfinishedSlices;
            private Map<Long, Map<Frequency, Wave>> unfinishedSlicesWaves;
            private Map<Long, VolumeAmplitudeState> finishedSlices;

            private InputPort<TimestampedNewNotesWithEnvelope> input;

            private OutputPort<SimpleImmutableEntry<DeterministicEnvelope, Collection<Frequency>>> addNewNotesOutputPort;
            private InputPort<Collection<EnvelopeForFrequency>> addNewNotesInputPort;

            private OutputPort<Collection<EnvelopeForFrequency>> groupEnvelopesByFrequencyOutputPort;
            private OutputPort<Long> sampleCountOutputPort;
            private OutputPort<Map<Frequency, Wave>> waveOutputPort;
            private OutputPort<VolumeAmplitudeState> oldStateOutputPort;

            private OutputPort<Collection<EnvelopeForFrequency>> groupEnvelopesByFrequencyOutputPortPrecalc;
            private OutputPort<Long> sampleCountOutputPortPrecalc;
            private OutputPort<Map<Frequency, Wave>> waveOutputPortPrecalc;
            private OutputPort<VolumeAmplitudeState> oldStateOutputPortPrecalc;
            private InputPort<VolumeAmplitudeState> newStateInputPortPrecalc;
            private OutputPort<Long> methodInputPort;
            private InputPort<VolumeAmplitudeState> methodOutputPort;

            private void precalculateInBackground() {
                while (input.isEmpty()) {
                    try {
                        Long futureSampleCount = unfinishedSlices.keySet().iterator().next();
                        Collection<EnvelopeForFrequency> currentUnfinishedSlice = unfinishedSlices.remove(futureSampleCount);
                        Map<Frequency, Wave> currentUnfinishedSliceWaves = unfinishedSlicesWaves.remove(futureSampleCount);
                        VolumeAmplitudeState oldFinishedSlice = finishedSlices.remove(futureSampleCount);
                        VolumeAmplitudeState finishedSlice = precalculate(futureSampleCount, currentUnfinishedSlice, currentUnfinishedSliceWaves, oldFinishedSlice);
                        finishedSlices.put(futureSampleCount, finishedSlice);
                    } catch (NoSuchElementException e) {
                        break;
                    }
                }
            }

            private void finish(long sampleCount, Collection<EnvelopeForFrequency> currentUnfinishedSlice, Map<Frequency, Wave> currentUnfinishedSliceWaves, VolumeAmplitudeState oldFinishedSlice) {
                try {
                    sampleCountOutputPort.produce(sampleCount);

                    try {
                        groupEnvelopesByFrequencyOutputPort.produce(currentUnfinishedSlice);
                    } catch (NullPointerException e) {
                        groupEnvelopesByFrequencyOutputPort.produce(new LinkedList<>());
                    }

                    try {
                        waveOutputPort.produce(currentUnfinishedSliceWaves);
                    } catch (NullPointerException e) {
                        waveOutputPort.produce(new HashMap<>());
                    }

                    try {
                        oldStateOutputPort.produce(oldFinishedSlice);
                    } catch (NullPointerException e) {
                        oldStateOutputPort.produce(new VolumeAmplitudeState(sampleCount, new HashMap<>()));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            private VolumeAmplitudeState precalculate(long sampleCount, Collection<EnvelopeForFrequency> currentUnfinishedSlice, Map<Frequency, Wave> currentUnfinishedSliceWaves, VolumeAmplitudeState oldFinishedSlice) {
                try {
                    sampleCountOutputPortPrecalc.produce(sampleCount);

                    try {
                        groupEnvelopesByFrequencyOutputPortPrecalc.produce(currentUnfinishedSlice);
                    } catch (NullPointerException e) {
                        groupEnvelopesByFrequencyOutputPortPrecalc.produce(new LinkedList<>());
                    }

                    try {
                        waveOutputPortPrecalc.produce(currentUnfinishedSliceWaves);
                    } catch (NullPointerException e) {
                        waveOutputPortPrecalc.produce(new HashMap<>());
                    }

                    try {
                        oldStateOutputPortPrecalc.produce(oldFinishedSlice);
                    } catch (NullPointerException e) {
                        oldStateOutputPortPrecalc.produce(new VolumeAmplitudeState(sampleCount, new HashMap<>()));
                    }

                    VolumeAmplitudeState result = newStateInputPortPrecalc.consume();

                    return result;

                } catch (InterruptedException e) {
                    e.printStackTrace();

                    return null;
                }
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

            private void addNewWaves(Long sampleCount, Long endingSampleCount, Collection<Frequency> newNotes, Map<Frequency, Wave> newNoteWaves) {
                if (newNotes.isEmpty()) {
                    return;
                }
                for (Long i = sampleCount; i < endingSampleCount; i++) {
                    Map<Frequency, Wave> oldUnfinishedSliceWaves = unfinishedSlicesWaves.get(i);
                    try {
                        Map<Frequency, Wave> missingNoteWaves = new HashMap<>(newNoteWaves);
                        missingNoteWaves.keySet().removeAll(oldUnfinishedSliceWaves.keySet());
                        oldUnfinishedSliceWaves.putAll(missingNoteWaves);
                    } catch (NullPointerException e) {
                        Map<Frequency, Wave> newUnfinishedSliceWaves = new HashMap<>(newNoteWaves);
                        unfinishedSlicesWaves.put(i, newUnfinishedSliceWaves);
                    }
                }
            }

            private void addNewEnvelopes(Long sampleCount, Long endingSampleCount, Collection<EnvelopeForFrequency> newNotesWithEnvelopes) {
                if (newNotesWithEnvelopes.isEmpty()) {
                    return;
                }
                for (Long i = sampleCount; i < endingSampleCount; i++) {
                    Collection<EnvelopeForFrequency> newUnfinishedSlice = unfinishedSlices.remove(i);
                    try {
                        newUnfinishedSlice.addAll(newNotesWithEnvelopes);
                    } catch (NullPointerException e) {
                        newUnfinishedSlice = new LinkedList<>(newNotesWithEnvelopes);
                    }
                    unfinishedSlices.put(i, newUnfinishedSlice);
                }
            }

            private Map<Frequency, Wave> reuseOrCreateNewWaves(Collection<Frequency> newNotes) {
                Map<Frequency, Wave> newNoteWaves = new HashMap<>();
                Set<Frequency> missingWaveFrequencies = new HashSet<>(newNotes);
                for (Long i : unfinishedSlicesWaves.keySet()) {
                    Map<Frequency, Wave> oldUnfinishedSliceWaves = unfinishedSlicesWaves.get(i);

                    Map<Frequency, Wave> foundWaves = new HashMap<>(oldUnfinishedSliceWaves);
                    foundWaves.keySet().retainAll(missingWaveFrequencies);
                    newNoteWaves.putAll(foundWaves);

                    missingWaveFrequencies = new HashSet<>(missingWaveFrequencies);
                    missingWaveFrequencies.removeAll(oldUnfinishedSliceWaves.keySet());
                }
                for (Frequency frequency : missingWaveFrequencies) {
                    Wave newWave = new Wave(frequency, sampleRate);
                    newNoteWaves.put(frequency, newWave);
                }
                return newNoteWaves;
            }

            {
                int capacity = 10;

                unfinishedSlices = new HashMap<>();
                unfinishedSlicesWaves = new HashMap<>();
                finishedSlices = new HashMap<>();

                SimpleBuffer<Long> methodInputBuffer = new SimpleBuffer<>(1, "mixer - input");
                methodInputPort = methodInputBuffer.createOutputPort();

                input =
                    methodInputBuffer
                    .performMethod(NoteTimestamper.build(noteInputBuffer))
                    .performMethod(EnvelopeWaveBuilder.buildEnvelopeWave(sampleRate)).createInputPort();

                {
                    SimpleImmutableEntry<OutputPort<SimpleImmutableEntry<DeterministicEnvelope, Collection<Frequency>>>, InputPort<Collection<EnvelopeForFrequency>>> addNewNotesPorts = RunningPipeComponent.methodToComponentPorts(addNewNotesInput -> toEnvelopesForFrequencies(addNewNotesInput.getKey(), addNewNotesInput.getValue()), capacity, "addNewNotes");
                    addNewNotesOutputPort = addNewNotesPorts.getKey();
                    addNewNotesInputPort = addNewNotesPorts.getValue();
                }

                {
                    BoundedBuffer<Long> sampleCountBuffer2 = new SimpleBuffer<>(capacity, "calculateValuesPerFrequency - sampleCount");
                    sampleCountOutputPort = sampleCountBuffer2.createOutputPort();
                    SimpleBuffer<Collection<EnvelopeForFrequency>> groupEnvelopesByFrequencyInputBuffer = new SimpleBuffer<>(capacity, "groupEnvelopesByFrequency - input");
                    groupEnvelopesByFrequencyOutputPort = groupEnvelopesByFrequencyInputBuffer.createOutputPort();
                    BoundedBuffer<Map<Frequency, Wave>> waveBuffer = new SimpleBuffer<>(capacity, "calculateValuesPerFrequency - waves");
                    waveOutputPort = waveBuffer.createOutputPort();

                    BoundedBuffer<VolumeAmplitudeState> oldStateBuffer = new SimpleBuffer<>(capacity, "oldState");
                    oldStateOutputPort = oldStateBuffer.createOutputPort();

                    methodOutputPort =
                            oldStateBuffer
                                    .pairWith(
                                            sampleCountBuffer2
                                                    .pairWith(
                                                            groupEnvelopesByFrequencyInputBuffer
                                                                    .performMethod(Mixer::groupEnvelopesByFrequency)
                                                                    .pairWith(waveBuffer))
                                                    .performMethod(
                                                            input1 -> new EnvelopeWaveSlice(
                                                                    input1.getKey(),
                                                                    input1.getValue().getKey(),
                                                                    input1.getValue().getValue()))
                                                    .performMethod(Mixer::calculateValuesPerFrequency)
                                                    .performMethod(Mixer::sumValuesPerFrequency))
                                    .performMethod(input ->
                                            input.getKey()
                                                    .add(input.getValue()))
                                    .createInputPort();
                }

                {
                    BoundedBuffer<Long> sampleCountBuffer3 = new SimpleBuffer<>(capacity, "calculateValuesPerFrequencyPrecalc - sampleCount");
                    sampleCountOutputPortPrecalc = sampleCountBuffer3.createOutputPort();
                    SimpleBuffer<Collection<EnvelopeForFrequency>> groupEnvelopesByFrequencyInputBuffer = new SimpleBuffer<>(capacity, "groupEnvelopesByFrequencyPrecalc - input");
                    groupEnvelopesByFrequencyOutputPortPrecalc = groupEnvelopesByFrequencyInputBuffer.createOutputPort();
                    BoundedBuffer<Map<Frequency, Wave>> waveBuffer = new SimpleBuffer<>(capacity, "calculateValuesPerFrequencyPrecalc - waves");
                    waveOutputPortPrecalc = waveBuffer.createOutputPort();

                    BoundedBuffer<VolumeAmplitudeState> oldStateBuffer = new SimpleBuffer<>(capacity, "oldStatePrecalc");
                    oldStateOutputPortPrecalc = oldStateBuffer.createOutputPort();

                    newStateInputPortPrecalc =
                            oldStateBuffer
                                    .pairWith(
                                            sampleCountBuffer3
                                                    .pairWith(
                                                            groupEnvelopesByFrequencyInputBuffer
                                                                    .performMethod(Mixer::groupEnvelopesByFrequency)
                                                                    .pairWith(waveBuffer))
                                                    .performMethod(
                                                            input11 -> new EnvelopeWaveSlice(
                                                                    input11.getKey(),
                                                                    input11.getValue().getKey(),
                                                                    input11.getValue().getValue()))
                                                    .performMethod(Mixer::calculateValuesPerFrequency)
                                                    .performMethod(Mixer::sumValuesPerFrequency))
                                    .performMethod(
                                            input1 ->
                                                    input1.getKey()
                                                            .add(input1.getValue()))
                                    .createInputPort();
                }
            }

            private VolumeAmplitudeState mix(Long sampleCount) {
                try {
                    methodInputPort.produce(sampleCount);

                    TimestampedNewNotesWithEnvelope timestampedNewNotesWithEnvelope = input.consume();
                    DeterministicEnvelope envelope = timestampedNewNotesWithEnvelope.getEnvelope();
                    Collection<Frequency> newNotes = timestampedNewNotesWithEnvelope.getFrequencies();

                    addNewNotes(sampleCount, envelope, newNotes);

                    Collection<EnvelopeForFrequency> currentFinishedSlice = unfinishedSlices.remove(sampleCount);
                    Map<Frequency, Wave> currentUnfinishedSliceWaves = unfinishedSlicesWaves.remove(sampleCount);
                    VolumeAmplitudeState oldFinishedSlice = finishedSlices.remove(sampleCount);
                    finish(sampleCount, currentFinishedSlice, currentUnfinishedSliceWaves, oldFinishedSlice);

    //            precalculateInBackground();

                    return methodOutputPort.consume();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public VolumeAmplitudeState call(Long input) {
                return mix(input);
            }
        };
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
            try {
                double amplitude = wave.getAmplitude(sampleCount);

                Collection<VolumeAmplitude> volumeAmplitudes = new LinkedList<>();
                for (Envelope envelope : envelopes) {
                    double volume = envelope.getVolume(sampleCount);
                    VolumeAmplitude volumeAmplitude = new VolumeAmplitude(volume, amplitude);
                    volumeAmplitudes.add(volumeAmplitude);
                }

                newVolumeAmplitudeCollections.put(frequency, volumeAmplitudes);
            }
            catch(NullPointerException ignored){
            }
        }
        return newVolumeAmplitudeCollections;
    }

    private static Map<Frequency, Collection<Envelope>> groupEnvelopesByFrequency(Collection<EnvelopeForFrequency> envelopesForFrequencies) {
        Map<Frequency, List<EnvelopeForFrequency>> groupedEnvelopeFroFrequencies = envelopesForFrequencies.stream().collect(Collectors.groupingBy(w -> w.getFrequency()));
        Map<Frequency, Collection<Envelope>> result = groupedEnvelopeFroFrequencies.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().stream().map(EnvelopeForFrequency::getEnvelope).collect(Collectors.toList())));
        return result;
    }

    private static Collection<EnvelopeForFrequency> toEnvelopesForFrequencies(DeterministicEnvelope envelope, Collection<Frequency> frequencies) {
        Collection<EnvelopeForFrequency> newNotesWithEnvelopes = new LinkedList<>();
        for(Frequency frequency : frequencies){
            newNotesWithEnvelopes.add(new EnvelopeForFrequency(frequency, envelope));
        }
        return newNotesWithEnvelopes;
    }

}
