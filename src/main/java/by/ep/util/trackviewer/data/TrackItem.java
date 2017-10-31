package by.ep.util.trackviewer.data;

import java.util.List;

public class TrackItem extends AbstractTrackItem {

    public String time;
    public String name;
    public String typ;
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
    public DumpItem dump;
    public List<TrackItem> children;

    public TrackItem(String time, String thread, String name, String typ, String id, String parentId,
                     String startTime, int processTime, int otherInvocationsCount, int otherInvocationsTime,
                     String othersStart, String othersFinish, int sqlCount, int sqlTime, String other) {

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

        if (startTime != null) {
            this.time = startTime;
        }
    }
}
