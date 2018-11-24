package mixer;

import component.buffer.*;
import frequency.Frequency;
import mixer.envelope.DeterministicEnvelope;
import mixer.envelope.Envelope;
import mixer.state.*;
import sound.SampleRate;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.stream.Collectors;

public class Mixer extends MethodPipeComponent<Long, VolumeAmplitudeState> {

    public Mixer(SimpleBuffer<Long> sampleCountBuffer, BoundedBuffer<Frequency> noteInputBuffer, SimpleBuffer<VolumeAmplitudeState> outputBuffer, SampleRate sampleRate){
        super(sampleCountBuffer, outputBuffer, toMethod(buildPipe(noteInputBuffer, sampleRate)));
    }

    public static PipeCallable<BoundedBuffer<Long>, BoundedBuffer<VolumeAmplitudeState>> buildPipe(BoundedBuffer<Frequency> noteInputBuffer, SampleRate sampleRate){
        return new PipeCallable<>() {
            private Map<Long, Collection<EnvelopeForFrequency>> unfinishedSlices;
            private Map<Long, Map<Frequency, Wave>> unfinishedSlicesWaves;
            private Map<Long, VolumeAmplitudeState> finishedSlices;

            private InputPort<TimestampedNewNotesWithEnvelope> timestampedNewNotesWithEnvelopeInputPort;

            private OutputPort<SimpleImmutableEntry<DeterministicEnvelope, Collection<Frequency>>> addNewNotesOutputPort;
            private InputPort<Collection<EnvelopeForFrequency>> addNewNotesInputPort;

            private OutputPort<Long> sampleCountOutputPort;
            private OutputPort<Collection<EnvelopeForFrequency>> groupEnvelopesByFrequencyOutputPort;
            private OutputPort<Map<Frequency, Wave>> waveOutputPort;
            private OutputPort<VolumeAmplitudeState> oldStateOutputPort;
            private InputPort<VolumeAmplitudeState> newStateInputPort;

            @Override
            public BoundedBuffer<VolumeAmplitudeState> call(BoundedBuffer<Long> inputBuffer) {
                int capacity = 10;

                unfinishedSlices = new HashMap<>();
                unfinishedSlicesWaves = new HashMap<>();
                finishedSlices = new HashMap<>();

                LinkedList<SimpleBuffer<Long>> inputBroadcast = new LinkedList<>(inputBuffer.broadcast(2, "mixer - sample count"));

                timestampedNewNotesWithEnvelopeInputPort =
                        inputBroadcast.poll()
                                .connectTo(NoteTimestamper.buildPipe(noteInputBuffer))
                                .performMethod(EnvelopeWaveBuilder.buildEnvelopeWave(sampleRate), "build envelope wave").createInputPort();

                addNewNotesOutputPort = new OutputPort<>();
                addNewNotesInputPort =
                    addNewNotesOutputPort.getBuffer()
                    .performMethod(
                        input ->
                            toEnvelopesForFrequencies(
                                input.getKey(),
                                input.getValue()), "addNewNotes")
                    .createInputPort();

                BoundedBuffer<VolumeAmplitudeState> outputBuffer =
                        inputBroadcast.poll().performMethod(this::mix, "mixer");

                sampleCountOutputPort = new OutputPort<>("mixer - sample count");
                groupEnvelopesByFrequencyOutputPort = new OutputPort<>("mixer - group envelopes by frequency");
                waveOutputPort = new OutputPort<>("mixer - wave output");
                oldStateOutputPort = new OutputPort<>("mixer - old state");

                newStateInputPort =
                    oldStateOutputPort.getBuffer()
                    .pairWith(
                        sampleCountOutputPort.getBuffer()
                        .pairWith(
                            groupEnvelopesByFrequencyOutputPort.getBuffer()
                            .performMethod(Mixer::groupEnvelopesByFrequency, "group envelopes by frequency precalc")
                            .pairWith(
                                waveOutputPort.getBuffer()))
                        .performMethod(
                            input11 -> new EnvelopeWaveSlice(
                                input11.getKey(),
                                input11.getValue().getKey(),
                                input11.getValue().getValue()), "build envelope wave slice precalc")
                        .performMethod(Mixer::calculateValuesPerFrequency, "calculate values per frequency precalc")
                        .performMethod(Mixer::sumValuesPerFrequency, "sum values per frequency precalc"))
                    .performMethod(
                        input1 ->
                            input1.getKey()
                            .add(input1.getValue()), "add new and old state precalc")
                .createInputPort();

                return outputBuffer;
            }

            private void precalculateInBackground() {
                while (timestampedNewNotesWithEnvelopeInputPort.isEmpty()) {
                    try {
                        Long futureSampleCount = unfinishedSlices.keySet().iterator().next();
                        Collection<EnvelopeForFrequency> currentUnfinishedSlice = unfinishedSlices.remove(futureSampleCount);
                        Map<Frequency, Wave> currentUnfinishedSliceWaves = unfinishedSlicesWaves.remove(futureSampleCount);
                        VolumeAmplitudeState oldFinishedSlice = finishedSlices.remove(futureSampleCount);
                        VolumeAmplitudeState finishedSlice = calculateVolume(futureSampleCount, currentUnfinishedSlice, currentUnfinishedSliceWaves, oldFinishedSlice);
                        finishedSlices.put(futureSampleCount, finishedSlice);
                    } catch (NoSuchElementException e) {
                        break;
                    }
                }
            }

            private VolumeAmplitudeState calculateVolume(long sampleCount, Collection<EnvelopeForFrequency> currentUnfinishedSlice, Map<Frequency, Wave> currentUnfinishedSliceWaves, VolumeAmplitudeState oldFinishedSlice) {
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

                    VolumeAmplitudeState result = newStateInputPort.consume();

                    return result;

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
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
                synchronized (unfinishedSlicesWaves) {
                    for (Long i : unfinishedSlicesWaves.keySet()) {
                        Map<Frequency, Wave> oldUnfinishedSliceWaves = unfinishedSlicesWaves.get(i);

                        Map<Frequency, Wave> foundWaves = new HashMap<>(oldUnfinishedSliceWaves);
                        foundWaves.keySet().retainAll(missingWaveFrequencies);
                        newNoteWaves.putAll(foundWaves);

                        missingWaveFrequencies = new HashSet<>(missingWaveFrequencies);
                        missingWaveFrequencies.removeAll(oldUnfinishedSliceWaves.keySet());
                    }
                }
                for (Frequency frequency : missingWaveFrequencies) {
                    Wave newWave = new Wave(frequency, sampleRate);
                    newNoteWaves.put(frequency, newWave);
                }
                return newNoteWaves;
            }

            private VolumeAmplitudeState mix(Long sampleCount) {
                try {
                    TimestampedNewNotesWithEnvelope timestampedNewNotesWithEnvelope = timestampedNewNotesWithEnvelopeInputPort.consume();
                    DeterministicEnvelope envelope = timestampedNewNotesWithEnvelope.getEnvelope();
                    Collection<Frequency> newNotes = timestampedNewNotesWithEnvelope.getFrequencies();

                    addNewNotes(sampleCount, envelope, newNotes);

                    Collection<EnvelopeForFrequency> currentFinishedSlice = unfinishedSlices.remove(sampleCount);
                    Map<Frequency, Wave> currentUnfinishedSliceWaves = unfinishedSlicesWaves.remove(sampleCount);
                    VolumeAmplitudeState oldFinishedSlice = finishedSlices.remove(sampleCount);
                    VolumeAmplitudeState result = calculateVolume(sampleCount, currentFinishedSlice, currentUnfinishedSliceWaves, oldFinishedSlice);

    //            precalculateInBackground();

                    return result;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
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
