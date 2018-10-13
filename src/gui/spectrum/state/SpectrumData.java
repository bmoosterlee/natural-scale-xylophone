package gui.spectrum.state;

import time.TimeInNanoSeconds;

public class SpectrumData {
    private final TimeInNanoSeconds frameEndTime;

    public SpectrumData(TimeInNanoSeconds frameEndTime) {
        this.frameEndTime = frameEndTime;
    }

    public TimeInNanoSeconds getFrameEndTime() {
        return frameEndTime;
    }
}
