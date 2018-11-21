package main;

import component.Counter;
import component.Unzipper;
import component.buffer.BoundedBuffer;
import component.buffer.OutputPort;
import component.buffer.SimpleBuffer;
import frequency.Frequency;
import gui.GUI;
import mixer.Mixer;
import mixer.state.VolumeAmplitudeState;
import pianola.Pianola;
import pianola.patterns.PianolaPattern;
import pianola.patterns.SweepToTargetUpDown;
import sound.SampleRate;
import sound.SoundEnvironment;
import spectrum.SpectrumBuilder;
import spectrum.SpectrumWindow;
import spectrum.buckets.Buckets;
import time.PerformanceTracker;
import time.Pulser;
import time.TimeInSeconds;

import java.awt.*;
import java.util.AbstractMap;
import java.util.LinkedList;

//todo AbstractComponent's getInputBuffers and getOutputBuffers should be refactored to getInputBuffers and getOutputBuffers
//We might directly connect our input buffers to other buffers. In this case we don't have an input buffer. We also do
//not want to create an input port, as this messes up our chain. We could of course create one internally.
//Since every input port links to it's buffer, we can easily retrace our steps.
//todo implement sequential and parallel component chain links. This is one class with a constructor setting.
//Since each component has a value here, we try to add a new component to it. If the value differs,
//we break the chain, create a TickRunningStrategy, and add ourselves as the previous component.
//We then just keep going until this occurs again. When we break the chain buffer wise, we create a simple tick runner
//for the entire chain.
//todo implement a field for parallelisability. For now we'll just use a return true by default method.
//todo Since parallelisability is a property of components, we might want to use isParallelisable instead.
//todo a final field for components will enforce it's final nature.
//todo abstractComponent is now just a meaningless class. We need to get back to basics.
//todo the purpose of chains was to run multiple components in a row on one thread. This includes non parallelizable
//components. With parallelization, we would want extra threads, to prevent clogging but just for the chains which are
//parallelizable. This means we build sub chains within the chain for parallelizable parts. If we initialize these
//with a live runner minimum of 0, we ensure that we don't always create two threads.
//This means build the entire component chain, building the parallelizable subchain and non-parallelizeable subchain
//during construction, create their runners at construction.
//This means a component chain either becomes a chain of chains.
//If we create the tickRunningStrategy at the end of the chain, we'll have a centralized point, but also need to
//iterate over the internal chains to do behavior they could do themselves. Lets do it the breaking of the chain.
//todo implement a tickConcurrency strategy which determines whether a component can be parallelized or not.
//this will let us convert components such as pulser and pianola to AbstractComponents. In TickRunnerSpawner we can
//build constructors for these.
//The hierarchy is as such: tickRunningStrategy is built for a tickConcurrencyStrategy, calls TickExecutingStrategy.
//chains have a try executing strategy, autonomous components (non chain) will use a wait strategy.
//This means that we will need to wrap chains and autonomous components into wrappers of a superclass.
//This could be called pipelining strategy. This is currently caught within the 'abstract pipe component' vs
//'abstract component' distinction, but this means we can't use abstract components as input of tick running strategy
//nor for tick runner spawner.
//Perhaps we could catch the autonomous or chainable logic into an interface. We still need to wrap chainable into
//the component chain wrapper, but we could use an object which determines this, and returns the correct wrapper
//autonomous or non autonomous.
//We shouldn't use an interface because components are either or. Rather we extend an abstract superclass.
//This means the performMethod call can be extended towards broadcasts as well, but will return a different wrapper.
//When building the componentChainLinkWrapper we currently return the output buffers. We do this for broadcast as well,
//but return multiple buffers in a collection. PerformMethod will not be callable on this collection, and thus we
//don't break perform method logic. However, we can't return multiple output buffers in perform method as of right now.
//We could define a second performMethod call which returns collections, but then we run into trouble with more complex
//output buffer structures.
//todo perform Collections.singleton calls etc at construction time.
//todo Merge ChainedInput and ChainedPipe.
//todo Refactor each component to an AbstractComponent extension.
//todo Refactor input components and callableWithArgument's to buildPipe
//todo rename to adaptiveTickRunner
//todo component chains should try to consume instead of consume at each component. If we can't find input,
//we continue onto the next component. This way we can more easily tackle bottlenecks.
//This would mean we want control over the tick method in MethodPipeComponents as well. Our runner strategy focusses
//on executing this, while another strategy could focus on tick executing.
//todo refactor SimpleTickRunner to take a Tickable construction parameter.
//Still pass the entire component to RunningStrategy so we can find it's input buffer.
//todo Implement a attemptToProduce method that lets harmonic buckets be filled if there is space available.
//todo We could create an PipeInputSegment class which is one of the two parts of a pipe component.
//We then always know what buffer to monitor for clogging.
//We can start with the actual pipe components and spawn more threads there.
//this lets us write logic for spawning extra threads for components with one input buffer.
//todo Research parallel pipelines.
//todo change all these running pipe components to pipecomponents, and if we don't use their constructors just remove
//the extension.
//todo implement combine method which takes two buffers of the same type and outputs one buffer which both are relayed to
//this is not useful in the main class as we have component loops. In order to do this correctly for flushers,
//we might need to create a method which lets you create an output port with a buffer, and lets you get the buffer.
//Sort of like 'performMethod' but for output ports, as in "push this calls' results to a buffer you create".
//The complicated part is that we can't create an outputPort and later add a buffer to it. What we can do is create
//a buffer with the port, and get the buffer from the port. We can then use this buffer later on.
//todo hide construction of simplebuffers, implement synchronizedBuffer class for fields which are updated.
//a field is a type of buffer where we ensure no other thread can consume from it until we've set the new value of the
//buffer. This means the entire chain after the field is synchronized until we reach the field again.
//todo refactor mixer to become more legible and buffer chained
//todo use the full range of buckets everywhere, and connect them with pipes.
//todo the purpose of buffer chains is to run an entire pipeline on a thread because we know it's a safe chain.
//this is why we left out broadcasts and pairers. Later, we figured out we can also use multiple threads on a chain
//as long as we don't use loops within the chains. This would be fixed by getting rid of callableWithArguments,
//and directly chain the input buffer to the method internals. Loops would be found because the only way to build a loop
//is by creating a SimpleBuffer explicitly. This would mean we can also get rid of createInternalInputPort.
//We can also use multiple threads within broadcasts, pairers, unpairers, etc. This would alleviate clogging.
//todo Another way to speed up rendering is to calculate rendering parameters in a component, and only do the actual
//rendering in the rendering component.
//We might still be able to use pipe components, as we could use broadcasts between the spectrum building components.
//If I recall correctly, we use the note spectrum to build the harmonic spectrum. This is a natural pipeline.
//If we broadcast the note spectrum and send it to a noteRenderer component, which has the back buffer image stored,
//we can start clearing the image, and then render the notes.
//We then send a pulse to the harmonic renderer, which triggers it to consume the harmonic spectrum.
//We then render the harmonic spectrum. This component also has the back buffer image stored.
//We then send a pulse to the cursor renderer, which has the back buffer image stored. We render the cursor renderer,
//and send a pulse to the painting component, which has the back buffer stored. Within the paintComponent method.
//We send a pulse to the note renderer, which starts the process over again.
//We cannot use a component which stores the back buffer image, as the rendering methods are intricate.
//If we had data we could send which represents the drawn data, we would be fine.
//Perhaps we could create translucent frames, and render the necessary data on that. This would allow us to
//send the layer to a component which holds the back buffer image from each renderer, and layer them over eachother.
//We could also move the clearing of the image to this component, which clears on a pulse from the repaint method.
//todo chain the render calls together in pipe components which respond to a pulse and timed consumption.
//at the end of the chain we send a pulse to a repainting component which also responds to a pulse.
//todo move paint clearing to the end of the paintComponent method such that we can start consuming as soon as possible.
//todo solve volumes to ys issue by refactoring GUIPanel GUI relationship to the one with Mixer and SoundEnvironment.
//currently the harmonics input for GUIPanel calculation might outrun the note input calculation. This means we have
//harmonic output clogged, while we wait for note input. This might be solved by painting asynchronously, if possible.
//Another solution would be to pair the information, and incur clogging before the pairer instead of in the repaint call.
//Since we're sending three pieces of data (notes, harmonics, and cursor location), we could create a class which
//represents the GUI state.
//Our testing also supports this hypothesis by showing the only clogged element is note volumes to ys, not harmonic
//volumes to ys.
//todo refactor NoteClicker and CursorMover to Flushers
//todo create a callableWithArguments implementation which allows to be run on buffers.
//this way, we can link buffers to the internal buffers of a build method without having to use input ports
//todo move towards no longer letting components extend pipecomponents, just let them have their build methods.
//todo refactor flush method in bufferStrategy to ensure that we never stall for polling, for when multiple ports
//  poll from a buffer. Flush should be a quick method which flushes the buffer, so it doesn't wait for new input.
//todo we want to be able to turn modifiable fields into components to achieve a more functional program.
//  We should encapsulate this into an object. It might be a type of buffer which always links to itself,
//  and allows for outside access by additional input and output ports,
//  the output ports are communicated with through broadcast.
//todo PrecalculatedBucketHistory needs thorough refactoring. Ideally we would remove the class, and use it's component.
//todo should we be able to create a RunningPipeComponent or ChainedPipeComponent out of any PipeComponent?
//since it's only the threading aspect that is changed by these classes, perhaps we can extract a larger class.
//We could extract to a ThreadingStrategy interface, and name them IndependentStrategy, and ChainStrategy
//todo if createOutputPort is never used for a BufferChainLink, as we can only access it after performing methods on it,
//we should remove the method, or create the ouputPort on the first buffer in the chain. We don't need to break the
//chain for this however.
//todo We must use createInputPort everywhere to ensure that the chain is broken for each BufferChainLink.
//that there is only one component linked to the buffer after any action has been performed on it.
//What if we create two inputPorts for example?
//An issue is that when we store the chain link, create an inputPort, and then perform another method on the chain link,
//we will not be able to ensure that the product of the chain link will certainly end up in the next chain link that we
//create in a performMethod call parallel to the inputPort. This means infinite stalling.
//We might be able to do this with statefulness, where the buffer sets a flag as soon as the chain has been broken.
//This prevents other components from treating it like a chain link.
//todo we can currently create multiple links to a buffer Chain end, which means two threads will run it's code.
//todo brainstorm techniques for making bufferChainLink hidden to the programmer to ensure we are in control of the
//chain flow.
//NOTE: I don't believe it's an issue when two threads run the code of a bufferChainLink, since every components is
//  separated by buffers. What might be an issue is when an inputPort and a performMethod call are called on the same
//  buffer link, causing the performMethod call not to be able to continue execution because of stalling.
//  since we also break the chain at the inputport, we will have a second thread refilling the pipeline, but the behavior
//  of the buffer chain link's input priority might cause problems.
// we should also look into whether we break the chain when creating an inputPort. Probably yes, as we cannot ensure
//todo rename to ChainBuffer, and SimplePipeComponent
//todo Use ChainedOutputComponent for our tickers.
//todo move overwritable buffers between mixer and spectrum and others into their respective comonents
//todo I'm writing all these CallableWithArguments methods now. For those we need to flush all these input and output ports.
//However, if we're linked components with performMethod, we can cut out the middleman of in and output ports, and
//connect the buffers of each component directly.
//this could be done by writing PipeComponent methods instead, and wrapping them writing a createInputPort and
//createOutputPort for the PipeComponent to use the ports when needed. The further we move to component based computing,
//the less these ports will be needed.
//Since we can't create a component without first creating it's buffers, the method we'll use instead of CallableWithArguments,
//which takes in the type of the inputBuffer content, and outputs the type of the outputBuffer,
//we write a method which takes in an inputBuffer, and outputs an outputBuffer.
//These can then be chained together directly.
//todo wrap each field of Mixer into an immutable class
//todo the finishedSlices field will be the easiest to refactor
//todo refactor Mixer to use no more fields
//todo convert oldCursorX field to component which takes in two inputs, the oldCursorX and newCursorX.
//if we approach each field like this we can move towards completely functional programs by turning each field into
//an immutable packet of data which is updated whenever new input comes in.

//todo move towards using one amplitude for multiple volumes.
//todo move to composite volume. Do this by having a volume object which either has atomic volume (value) or composite volume (set)
//todo since we can only add notes at the current sampleCount, and we can't add notes in the future or past,
//todo our futureEnvelopeWaveStates is never longer than the length of an envelope. We can this use an array for it's length.
//todo extract delegate in boundedBuffer and overwritableBuffer which encapsulate the buffer behavior. Then we can
//create classes such as PairBuffer which can be either bounded or overwritable, which has all methods from the superclass
//"buffer".
//todo create a complimentary pianola pattern which, at a certain rate, checks what notes are being played,
//todo and plays harmonically complimentary notes near the notes being played. Use a higher frame rate preferably

/*
The main components are:
GUI             - Takes in user input and note and harmonic spectra, sends out notes, renders
Mixer           - Takes in notes, sends out volumes
SoundEnvironment- Takes in volumes, plays sound
SpectrumBuilder - Takes in volumes, sends out note and harmonic spectra
Pianola         - Takes in note and harmonic spectra, and sends out notes

The component edges are:
GUI -> Mixer                        - sending notes
Mixer -> SoundEnvironment           - sending volumes
SoundEnvironment -> SpectrumBuilder - sending volumes
SpectrumBuilder -> GUI              - sending note and harmonic spectra
SpectrumBuilder -> Pianola          - sending note and harmonic spectra
Pianola -> Mixer                    - sending notes

The IO side effects are:
GUI                 - Takes in user input, renders
SoundEnvironment    - plays sound

The tickers for the components are:
Frame ticker    - notifies the GUI to render a new frame
Sample ticker   - notifies the Mixer to sample volumes
Pianola ticker  - notifies the Pianola to play new notes
*/
class Main {

    public static void main(String[] args){
        new PerformanceTracker();
        PerformanceTracker.start();

        int SAMPLE_RATE = 44100/256;
        int sampleLookahead = SAMPLE_RATE / 4;
        int SAMPLE_SIZE_IN_BITS = 8;

        int frameRate = 60/6;
        int frameLookahead = frameRate / 4;
        int width = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth()/2;

        double octaveRange = 3.;
        int inaudibleFrequencyMargin = (int) (width/octaveRange/12/5);

        int pianolaRate = 4;
        int pianolaLookahead = pianolaRate / 4;

        SampleRate sampleRate = new SampleRate(SAMPLE_RATE);
        SpectrumWindow spectrumWindow = new SpectrumWindow(width, octaveRange);

        SimpleBuffer<Frequency> newNoteBuffer = new SimpleBuffer<>(64, "new notes");
        LinkedList<SimpleBuffer<VolumeAmplitudeState>> volumeBroadcast =
            new LinkedList<>(
                    Pulser.buildOutputBuffer(new TimeInSeconds(1).toNanoSeconds().divide(sampleRate.sampleRate),
                        sampleLookahead,
                        "sample ticker - output")
                .toOverwritable()
                .performMethod(Counter.build(), "count samples")
                .connectTo(Mixer.buildPipe(newNoteBuffer, sampleRate))
                .broadcast(2, "main volume - broadcast"));

        volumeBroadcast.poll()
        .connectTo(SoundEnvironment.buildPipe(SAMPLE_SIZE_IN_BITS, sampleRate));

        AbstractMap.SimpleImmutableEntry<BoundedBuffer<Buckets>, BoundedBuffer<Buckets>> spectrumPair =
            SpectrumBuilder.buildComponent(
                Pulser.buildOutputBuffer(new TimeInSeconds(1).toNanoSeconds().divide(frameRate), frameLookahead, "GUI ticker")
                .toOverwritable(),
            volumeBroadcast.poll()
                .toOverwritable(),
            spectrumWindow,
            width);

        LinkedList<SimpleBuffer<Buckets>> noteSpectrumBroadcast = new LinkedList<>(spectrumPair.getKey().broadcast(2, "main note spectrum - broadcast"));
        LinkedList<SimpleBuffer<Buckets>> harmonicSpectrumBroadcast = new LinkedList<>(spectrumPair.getValue().broadcast(2, "main harmonic spectrum - broadcast"));

        SimpleBuffer<java.util.List<Frequency>> guiOutputBuffer = new SimpleBuffer<>(1, "gui output");
        Unzipper.unzip(guiOutputBuffer).relayTo(newNoteBuffer);
        new GUI(
            noteSpectrumBroadcast.poll(),
            harmonicSpectrumBroadcast.poll(),
            guiOutputBuffer,
            spectrumWindow,
            width,
            inaudibleFrequencyMargin);

//        PianolaPattern pianolaPattern = new Sweep(this, 8, spectrumWindow.getCenterFrequency());
//        PianolaPattern pianolaPattern = new PatternPauser(8, new SweepToTarget(pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer, 5, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow), 5);
        PianolaPattern pianolaPattern = new SweepToTargetUpDown(8, spectrumWindow.getCenterFrequency(), 2.0, spectrumWindow, inaudibleFrequencyMargin);
//        PianolaPattern pianolaPattern = new SimpleArpeggio(pianolaNotesBucketsBuffer, pianolaHarmonicsBucketsBuffer,3, spectrumWindow);


        SimpleBuffer<java.util.List<Frequency>> pianolaOutputBuffer = new SimpleBuffer<>(1, "gui output");
        Unzipper.unzip(pianolaOutputBuffer).relayTo(newNoteBuffer);

        new Pianola(
                Pulser.buildOutputBuffer(new TimeInSeconds(1).toNanoSeconds().divide(pianolaRate),
                    pianolaLookahead,
                    "Pianola ticker")
                .toOverwritable(),
            noteSpectrumBroadcast.poll()
                .toOverwritable(),
            harmonicSpectrumBroadcast.poll()
                .toOverwritable(),
            pianolaOutputBuffer,
            pianolaPattern,
            inaudibleFrequencyMargin);

        playTestTone(newNoteBuffer, spectrumWindow);
    }

    private static void playTestTone(SimpleBuffer<Frequency> newNoteBuffer, SpectrumWindow spectrumWindow) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            new OutputPort<>(newNoteBuffer).produce(spectrumWindow.getCenterFrequency());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
