package org.DisneylandMap;

import java.io.IOException;

public interface IQueueTime {

    public int getWaitTime(int landID, int attractionID) throws IOException, InterruptedException;
}
