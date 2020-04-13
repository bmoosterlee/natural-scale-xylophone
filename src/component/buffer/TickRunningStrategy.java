package component.buffer;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class TickRunningStrategy {

    public TickRunningStrategy(final AbstractComponent component){
        new SimpleTickRunner(component).start();
        addComponent(component);
    }

    private void addComponent(AbstractComponent component) {
        List<AbstractMap.SimpleImmutableEntry<String, BoundedBuffer>> inputBuffers = ((Collection<BoundedBuffer>) component.getInputBuffers()).stream().map(input -> new AbstractMap.SimpleImmutableEntry<>(input.getName(), input)).collect(Collectors.toList());
        List<AbstractMap.SimpleImmutableEntry<String, BoundedBuffer>> outputBuffers = ((Collection<BoundedBuffer>) component.getOutputBuffers()).stream().map(input -> new AbstractMap.SimpleImmutableEntry<>(input.getName(), input)).collect(Collectors.toList());
        Map<String, BoundedBuffer> bufferMap = new HashMap<>();
        bufferMap.putAll(inputBuffers.stream().collect(Collectors.toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue)));
        bufferMap.putAll(outputBuffers.stream().collect(Collectors.toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue)));
        TrafficAnalyzer.trafficAnalyzer.addComponent(
                inputBuffers.stream().map(AbstractMap.SimpleImmutableEntry::getKey).collect(Collectors.toList()),
                outputBuffers.stream().map(AbstractMap.SimpleImmutableEntry::getKey).collect(Collectors.toList()),
                bufferMap);
    }

}
