package org.DisneylandMap;

import java.io.IOException;

// Adaptor for the QueueTimes API
public interface IQueueTime {

    public int getWaitTime(int landID, int attractionID) throws IOException, InterruptedException;
}
