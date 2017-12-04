package by.ep.util.trackviewer.data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DumpItem {
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

    public final String time;
    public final String id;
    public final String thread;
    public final String state;
    public final boolean isNative;
    private long longTime = -1;

    public final List<String> stackTrace = new ArrayList<>();

    public DumpItem(String time, String id, String thread, String state, boolean isNative) {

        this.time = time;
        this.id = id;
        this.thread = thread;
        this.state = state;
        this.isNative = isNative;
    }

    public long getLongTime() {

        if (longTime == -1) {
            if (time == null || time.isEmpty()) {
                longTime = 0;
            } else {
                longTime = ZonedDateTime.of(LocalDateTime.parse(time, dateFormat), ZoneId.systemDefault()).toInstant()
                        .toEpochMilli();
            }

        }
        return longTime;
    }
}
