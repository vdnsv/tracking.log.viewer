package by.ep.util.trackviewer.data;

import java.util.List;

public class TrackItem {

    public String time;
    public String name;
    public String typ;
    public String thread;
    public String id;
    public String parentId;
    public String startTime;
    public int processTime;
    public int otherInvocationsCount;
    public int otherInvocationsTime;
    public String othersStart;
    public String othersFinish;
    public int sqlCount;
    public int sqlTime;
    public String other;
    public boolean isStart;
    public List<DumpItem> samplingItems;
    public List<TrackItem> children;

    public TrackItem(String time, String thread, String name, String typ, String id, String parentId,
            String startTime, int processTime, int otherInvocationsCount, int otherInvocationsTime,
            String othersStart, String othersFinish, int sqlCount, int sqlTime, String other, boolean isStart) {

        this.time = time;
        this.thread = thread;
        this.name = name;
        this.typ = typ;
        this.id = id;
        this.parentId = parentId;
        this.startTime = startTime;
        this.processTime = processTime;
        this.otherInvocationsCount = otherInvocationsCount;
        this.otherInvocationsTime = otherInvocationsTime;
        this.othersStart = othersStart;
        this.othersFinish = othersFinish;
        this.sqlCount = sqlCount;
        this.sqlTime = sqlTime;
        this.other = other;
        this.isStart = isStart;

        if (startTime != null) {
            this.time = startTime;
        }
    }

    @Override
    public String toString() {

        return "" +
                "time='" + time + '\'' +
                ", name='" + name + '\'' +
                ", typ='" + typ + '\'' +
                ", thread='" + thread + '\'' +
                ", id='" + id + '\'' +
                ", parentId='" + parentId + '\'' +
                ", startTime='" + startTime + '\'' +
                ", processTime=" + processTime +
                ", otherInvocationsCount=" + otherInvocationsCount +
                ", otherInvocationsTime=" + otherInvocationsTime +
                ", othersStart='" + othersStart + '\'' +
                ", othersFinish='" + othersFinish + '\'' +
                ", sqlCount=" + sqlCount +
                ", sqlTime=" + sqlTime;
    }
}
