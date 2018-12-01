package mixer;

import component.Counter;
import component.Pulse;
import component.buffer.*;
import frequency.Frequency;
import mixer.envelope.DeterministicEnvelope;
import mixer.state.*;
import sound.SampleRate;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.Callable;

public class Mixer {

    public static SimpleImmutableEntry<BoundedBuffer<VolumeState>, BoundedBuffer<AmplitudeState>> buildComponent(BoundedBuffer<Pulse> inputBuffer, BoundedBuffer<Frequency> noteInputBuffer, SampleRate sampleRate){
        Callable<SimpleImmutableEntry<BoundedBuffer<VolumeState>, BoundedBuffer<AmplitudeState>>> o = new Callable<>() {

            @Override
            public SimpleImmutableEntry<BoundedBuffer<VolumeState>, BoundedBuffer<AmplitudeState>> call(){
                //Mix
                LinkedList<SimpleBuffer<NewNotesVolumeData>> newNoteDataBroadcast = new LinkedList<>(
                        inputBuffer
                        //Precalculate future samples
                        .performMethod(((PipeCallable<Pulse, Pulse>) input1 -> {
//                            precalculateInBackground();
                            return input1;
                        }).toSequential(), "mixer - precalculate")
                        //Determine whether to add new notes or mix
                        .performMethod(Counter.build(), "count samples")
//                        .performMethod(OrderStamper.build(), "mixer - order stamp")
                        .connectTo(NoteTimestamper.buildPipe(noteInputBuffer))
                        .performMethod(EnvelopeBuilder.buildEnvelope(sampleRate), "build envelope")
                        .performMethod(((PipeCallable<TimestampedNewNotesWithEnvelope, NewNotesVolumeData>) this::extractNewNotesData).toSequential(), "mixer - add new notes")
                        .broadcast(2, "mixer - new note data"));

                return new SimpleImmutableEntry<>(
                        newNoteDataBroadcast.poll()
                        .connectTo(VolumeCalculator.buildPipe().toSequential()),
                        newNoteDataBroadcast.poll()
                        .performMethod(((PipeCallable<NewNotesVolumeData, NewNotesAmplitudeData>) input ->
                                new NewNotesAmplitudeData(input.getSampleCount(), input.getEndingSampleCount(), input.getNewNotes())).toSequential(), "mixer - extract amplitude data from new note data")
                        .connectTo(AmplitudeCalculator.buildPipe(sampleRate).toSequential()));
            }

//            private void precalculateInBackground() {
//                while (inputBuffer.isEmpty()) {
//                    try {
//                        Long futureSampleCount = volumeCalculator.unfinishedEnvelopeSlices.keySet().iterator().next();
//
//                        VolumeState finishedVolumeSlice = calculateVolume.call(futureSampleCount);
//                        AmplitudeState finishedAmplitudeSlice = calculateAmplitude.call(futureSampleCount);
//
//                        volumeCalculator.finishedVolumeSlices.put(futureSampleCount, finishedVolumeSlice);
//                        amplitudeCalculator.finishedAmplitudeSlices.put(futureSampleCount, finishedAmplitudeSlice);
//                    } catch (NoSuchElementException e) {
//                        break;
//                    } catch (NullPointerException ignored) {
//                        //Before the volume calculator and amplitude calculator have been initialized,
//                        //we throw a NullPointerException.
//                    }
//                }
//            }

            private NewNotesVolumeData extractNewNotesData(TimestampedNewNotesWithEnvelope timestampedNewNotesWithEnvelope) {
                Long startingSampleCount = timestampedNewNotesWithEnvelope.getSampleCount();
                Collection<Frequency> newNotes = timestampedNewNotesWithEnvelope.getFrequencies();
                DeterministicEnvelope envelope = timestampedNewNotesWithEnvelope.getEnvelope();
                long endingSampleCount = envelope.getEndingSampleCount();

                return new NewNotesVolumeData(startingSampleCount, endingSampleCount, newNotes, envelope);
            }
        };

        try {
            return o.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
