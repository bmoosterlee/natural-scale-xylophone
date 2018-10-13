package gui.spectrum.state;

import gui.spectrum.SpectrumWindow;
import time.TimeInNanoSeconds;

public class SpectrumInput {
    private final long expectedTickCount;
    private final TimeInNanoSeconds frameEndTime;

    public SpectrumInput(long expectedTickCount, TimeInNanoSeconds frameEndTime) {
        this.expectedTickCount = expectedTickCount;
        this.frameEndTime = frameEndTime;
    }

    public long getExpectedTickCount() {
        return expectedTickCount;
    }

    public TimeInNanoSeconds getFrameEndTime() {
        return frameEndTime;
    }
}
