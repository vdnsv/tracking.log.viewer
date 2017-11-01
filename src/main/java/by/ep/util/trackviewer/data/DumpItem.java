package by.ep.util.trackviewer.data;

import java.util.ArrayList;
import java.util.List;

public class DumpItem {

    public final String id;
    public final String thread;
    public final String state;
    public final boolean isNative;

    public final List<String> dump = new ArrayList<>();

    public DumpItem(String id, String thread, String state, boolean isNative) {

        this.id = id;
        this.thread = thread;
        this.state = state;
        this.isNative = isNative;
    }

}
