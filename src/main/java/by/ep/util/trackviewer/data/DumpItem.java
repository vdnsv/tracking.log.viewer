package by.ep.util.trackviewer.data;

import java.util.ArrayList;
import java.util.List;

public class DumpItem extends AbstractTrackItem {

    public String state;
    public boolean isNative;

    public List<String> dump = new ArrayList<>();

    public DumpItem(String id, String thread, String state, boolean isNative) {

        this.id = id;
        this.thread = thread;
        this.state = state;
        this.isNative = isNative;
    }

}
