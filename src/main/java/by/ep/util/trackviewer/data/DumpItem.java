package by.ep.util.trackviewer.data;

import java.util.ArrayList;
import java.util.List;

public class DumpItem {

    public final String time;
    public final String id;
    public final String thread;
    public final String state;
    public final boolean isNative;

    public final List<String> stackTrace = new ArrayList<>();

    public DumpItem(String time, String id, String thread, String state, boolean isNative) {

        this.time = time;
        this.id = id;
        this.thread = thread;
        this.state = state;
        this.isNative = isNative;
    }

}
