package mixer;

import component.buffer.*;
import component.orderer.OrderStampedPacket;
import component.orderer.OrderStamper;
import component.orderer.Orderer;
import frequency.Frequency;
import mixer.state.AmplitudeState;
import mixer.state.Wave;
import sound.SampleRate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

class AmplitudeCalculator {

    static PipeCallable<BoundedBuffer<NewNotesAmplitudeData, OrderStampedPacket<NewNotesAmplitudeData>>, BoundedBuffer<AmplitudeState, OrderStampedPacket<AmplitudeState>>> buildPipe(SampleRate sampleRate) {
        return new PipeCallable<>() {
            final Map<Long, Map<Frequency, Wave>> unfinishedSampleFragments = new HashMap<>();

            @Override
            public BoundedBuffer<AmplitudeState, OrderStampedPacket<AmplitudeState>> call(BoundedBuffer<NewNotesAmplitudeData, OrderStampedPacket<NewNotesAmplitudeData>> inputBuffer) {
                return inputBuffer
                        .performMethod(((PipeCallable<NewNotesAmplitudeData, Long>) this::addNewWaves).toSequential(), "amplitude calculator - add new notes")
                        .<PrecalculatorOutputData<Long, Map<Frequency, Wave>, AmplitudeState>, OrderStampedPacket<PrecalculatorOutputData<Long, Map<Frequency, Wave>, AmplitudeState>>>connectTo(MapPrecalculator.buildPipe(
                                unfinishedSampleFragments,
                                input2 -> calculateAmplitudesPerFrequency(input2.getKey(), input2.getValue()),
                                AmplitudeState::add,
                                HashMap::new,
                                () -> new AmplitudeState(new HashMap<>()))).performMethod(input -> input.getFinishedDataUntilNow()
                                .add(calculateAmplitudesPerFrequency(
                                        input.getIndex(),
                                        input.getFinalUnfinishedData())));
            }

            private Long addNewWaves(NewNotesAmplitudeData newNotesAmplitudeData) {
                Long sampleCount = newNotesAmplitudeData.getSampleCount();

                Collection<Frequency> newNotes = newNotesAmplitudeData.getNewNotes();

                if(!newNotes.isEmpty()) {
                    Map<Frequency, Wave> newNoteWaves = reuseOrCreateNewWaves(newNotes, sampleRate);

                    for (Long i = sampleCount; i < newNotesAmplitudeData.getEndingSampleCount(); i++) {
                        synchronized (unfinishedSampleFragments) {
                            Map<Frequency, Wave> oldUnfinishedSliceWaves = unfinishedSampleFragments.remove(i);
                            if (oldUnfinishedSliceWaves != null) {
                                Map<Frequency, Wave> missingNoteWaves = new HashMap<>(newNoteWaves);
                                missingNoteWaves.keySet().removeAll(oldUnfinishedSliceWaves.keySet());
                                oldUnfinishedSliceWaves.putAll(missingNoteWaves);
                            } else {
                                oldUnfinishedSliceWaves = new HashMap<>(newNoteWaves);
                            }
                            unfinishedSampleFragments.put(i, oldUnfinishedSliceWaves);
                        }
                    }
                }
                return sampleCount;
            }

            private Map<Frequency, Wave> reuseOrCreateNewWaves(Collection<Frequency> newNotes, SampleRate sampleRate) {
                Map<Frequency, Wave> newNoteWaves = new HashMap<>();
                Set<Frequency> missingWaveFrequencies = new HashSet<>(newNotes);
                Set<Long> keys;
                synchronized (unfinishedSampleFragments) {
                    keys = new HashSet<>(unfinishedSampleFragments.keySet());
                }
                for (Long i : keys) {
                        synchronized (unfinishedSampleFragments) {
                            Map<Frequency, Wave> unfinishedSampleFragment = unfinishedSampleFragments.get(i);
                            if (unfinishedSampleFragment != null) {
                                Map<Frequency, Wave> foundWaves = new HashMap<>(unfinishedSampleFragment);
                                foundWaves.keySet().retainAll(missingWaveFrequencies);
                                newNoteWaves.putAll(foundWaves);

                                missingWaveFrequencies.removeAll(foundWaves.keySet());
                            }
                        }
                    }
                for (Frequency frequency : missingWaveFrequencies) {
                    Wave newWave = new Wave(frequency, sampleRate);
                    newNoteWaves.put(frequency, newWave);
                }
                return newNoteWaves;
            }
        };
    }

    private static AmplitudeState calculateAmplitudesPerFrequency(Long sampleCount, Map<Frequency, Wave> wavesPerFrequency) {
        Map<Frequency, Double> newAmplitudeCollections = new HashMap<>();
        for (Frequency frequency : wavesPerFrequency.keySet()) {
            Wave wave = wavesPerFrequency.get(frequency);
            double amplitude = wave.getAmplitude(sampleCount);
            newAmplitudeCollections.put(frequency, amplitude);
        }
        return new AmplitudeState(newAmplitudeCollections);
    }
}
