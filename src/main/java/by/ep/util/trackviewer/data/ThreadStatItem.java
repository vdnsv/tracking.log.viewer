package by.ep.util.trackviewer.data;

public class ThreadStatItem {

    public String time;
    public int runnableCount;
    public int timedWaitingCount;
    public int waitingCount;
    public int forced;
    public int sampled;

    public ThreadStatItem(String time, int runnableCount, int timedWaitingCount, int waitingCount, int forced,
                          int sampled) {

        this.time = time;
        this.runnableCount = runnableCount;
        this.timedWaitingCount = timedWaitingCount;
        this.waitingCount = waitingCount;
        this.forced = forced;
        this.sampled = sampled;
    }

}
