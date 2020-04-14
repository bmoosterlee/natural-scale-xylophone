package pianola.notebuilder;

import component.buffer.BoundedBuffer;
import component.buffer.Packet;
import component.buffer.SimplePacket;
import frequency.Frequency;
import pianola.notebuilder.envelope.DeterministicEnvelope;
import pianola.notebuilder.state.EnvelopeBuilder;
import pianola.notebuilder.state.NoteTimestamper;
import pianola.notebuilder.state.TimestampedNewNotesWithEnvelope;
import sound.SampleRate;
import spectrum.SpectrumWindow;

import java.util.Collection;
import java.util.Map;

public class NoteBuilder {

    public static <B extends Packet<Frequency>> BoundedBuffer<Double[], SimplePacket<Double[]>> buildComponent(BoundedBuffer<Frequency, B> noteInputBuffer, SampleRate sampleRate, SpectrumWindow spectrumWindow, BoundedBuffer<Long, SimplePacket<Long>> stampedSamplesBuffer) {
        BoundedBuffer<NewNotesVolumeData, SimplePacket<NewNotesVolumeData>> newNoteData = stampedSamplesBuffer
                .connectTo(NoteTimestamper.buildPipe(noteInputBuffer))
                .performMethod(EnvelopeBuilder.buildEnvelope(sampleRate), sampleRate.sampleRate / 32, "pianola.notebuilder - build envelope")
                .performMethod(NoteBuilder::extractNewNotesData, sampleRate.sampleRate / 32, "pianola.notebuilder - extract new note data");

        return newNoteData
                .connectTo(VolumeCalculator.buildPipe())
                .performMethod(input -> {
                    Double[] volumes = new Double[spectrumWindow.width];
                    for(int i = 0; i<spectrumWindow.width; i++){
                        volumes[i] = 0.;
                    }
                    for(Map.Entry<Frequency, Double> entry : input.volumes.entrySet()){
                        volumes[spectrumWindow.getX(entry.getKey())] += entry.getValue();
                    }
                    return volumes;
                }, 100, "notebuilder - convert to array");
    }

    private static NewNotesVolumeData extractNewNotesData(TimestampedNewNotesWithEnvelope timestampedNewNotesWithEnvelope) {
        Long startingSampleCount = timestampedNewNotesWithEnvelope.getSampleCount();
        Collection<Frequency> newNotes = timestampedNewNotesWithEnvelope.getFrequencies();
        DeterministicEnvelope envelope = timestampedNewNotesWithEnvelope.getEnvelope();
        long endingSampleCount = envelope.getEndingSampleCount();

        return new NewNotesVolumeData(startingSampleCount, endingSampleCount, newNotes, envelope);
    }
}
