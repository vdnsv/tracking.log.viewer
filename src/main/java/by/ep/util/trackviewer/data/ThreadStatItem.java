package by.ep.util.trackviewer.data;

public class ThreadStatItem {

    public final String time;
    public final int runnableCount;
    public final int timedWaitingCount;
    public final int waitingCount;
    public final int blocked;
    public final int forced;
    public final int sampled;

    public ThreadStatItem(String time, int runnableCount, int timedWaitingCount, int waitingCount, int blocked,
            int forced, int sampled) {

        this.time = time;
        this.runnableCount = runnableCount;
        this.timedWaitingCount = timedWaitingCount;
        this.waitingCount = waitingCount;
        this.blocked = blocked;
        this.forced = forced;
        this.sampled = sampled;
    }
}
