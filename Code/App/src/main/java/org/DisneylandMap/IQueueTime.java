package src.main.java.org.DisneylandMap;

import java.io.IOException;

// Interface for the QueueTimes API adaptor
public interface IQueueTime {

    public int getWaitTime(int landID, int attractionID) throws IOException, InterruptedException;
}
