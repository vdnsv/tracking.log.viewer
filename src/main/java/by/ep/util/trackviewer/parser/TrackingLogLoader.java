package by.ep.util.trackviewer.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import by.ep.util.trackviewer.data.DumpItem;
import by.ep.util.trackviewer.data.ThreadStatItem;
import by.ep.util.trackviewer.data.TrackItem;

public class TrackingLogLoader {
    // private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

    private static final int START_ITEM_DEF_PROCESS_TIME = 999999;

    private List<ThreadStatItem> threadStatistics = new ArrayList<>();
    private List<TrackItem> trackItems = new ArrayList<>();
    private Map<String, TrackItem> idToItemMap = new HashMap<>();
    private List<TrackItem> rootItems = new ArrayList<>();
    private Map<String, List<String>> stackItems = new HashMap<>();
    private List<DumpItem> unboundSamples = new ArrayList<>();


    private String dirName;

    public TrackingLogLoader(final String dirName) {

        this.dirName = dirName;
    }

    public void scan() throws IOException {

        File dir = new File(dirName);
        File[] trackingLogFiles = dir
                .listFiles((dir1, name) -> name.startsWith("tracking-stacks.") || name.startsWith("tracking."));
        if (trackingLogFiles != null) {
            Arrays.sort(trackingLogFiles,
                    (File o1, File o2) -> {
                        if (o1.getName().startsWith("tracking-stacks.log") && !o2.getName()
                                .startsWith("tracking-stacks.log")) {
                            return -1;
                        }
                        if (!o1.getName().startsWith("tracking-stacks.log") && o2.getName()
                                .startsWith("tracking-stacks.log")) {
                            return 1;
                        }

                        if (o1.getName().contains(".log.") && !o2.getName().contains(".log.")) {
                            return -1;
                        }

                        if (!o1.getName().contains(".log.") && o2.getName().contains(".log.")) {
                            return -1;
                        }

                        return o1.getName().compareTo(o2.getName());
                    }
            );
            for (File trackingLogFile : trackingLogFiles) {
                if (trackingLogFile.getName().startsWith("tracking-stacks.")) {
                    loadTrackingStack(trackingLogFile);
                } else {
                    loadTrackingLog(trackingLogFile);
                    // break;
                }
            }
            buildTree();
        }
    }

    private void loadTrackingStack(File trackingStackFile) throws FileNotFoundException {

        try (Scanner scanner = new Scanner(trackingStackFile)) {
            List<String> latestStack = null;
            while (scanner.hasNext()) {
                final String line = scanner.nextLine();
                if (line.isEmpty()) {
                    latestStack = null;
                } else {
                    if (line.endsWith(" (SamplerRunner) StoreState; ")
                            || line.contains(" (SamplerRunner) StoreCleanInfo; ")
                            || line.contains(" (SamplerRunner) StoreStateDelta; ")) {
                        latestStack = null;
                    } else if (line.trim().length() == 40) {
                        latestStack = new ArrayList<>();
                        stackItems.put(line, latestStack);
                    } else {
                        if (latestStack == null) {
                            System.err.println("No header for stack found: " + line);
                        } else {
                            latestStack.add(line);
                        }
                    }
                }
            }
        }
    }

    private void loadTrackingLog(File trackingLogFile) throws IOException {

        Entry<String, TrackItem>[] oldIdToItemMapKeys = idToItemMap.entrySet().toArray(new Entry[0]);

        try (Scanner scanner = new Scanner(trackingLogFile)) {
            ThreadStatItem threadStatItem = null;
            DumpItem dumpItem = null;
            TrackItem lastTrackItem = null;
            while (scanner.hasNext()) {
                final String line = scanner.nextLine();
                final String trimLine = line.trim();
                if (!trimLine.isEmpty() && !trimLine.contains(" (SamplerRunner) Tick: ")) {

                    if (line.startsWith("id: ")) {
                        // start of dump
                        // example: id: 1026; name: ServerService Thread Pool -- 51; state: BLOCKED; native: false

                        dumpItem = parseDumpItem(line, threadStatItem == null ? null : threadStatItem.time);

                    } else if (line.startsWith("\t") && trimLine.length() == 40) {
                        // stack trace hash
                        // example: 	b453b47aefe48ff38d0883ba7549588b424bb332

                        if (dumpItem == null) {
                            System.err.println("Not found dump for stack trace hash: " + line);
                        } else {
                            dumpItem.stackTrace.add(trimLine);
                        }
                    } else if (trimLine.contains(" (SamplerRunner) Dump; states: ")) {
                        // Sample info
                        // example: 2019-10-16 23:15:11,396 (SamplerRunner) Dump; states: WAITING=947,BLOCKED=3,TIMED_WAITING=103,RUNNABLE=44; forced: 1; sampled: 37

                        threadStatItem = parseThreadStatItem(line);
                        threadStatistics.add(threadStatItem);
                    } else if (line.startsWith("Additional Info: ")) {
                        if (lastTrackItem != null) {
                            lastTrackItem.other =
                                    lastTrackItem.other == null ? line : lastTrackItem.other + "\n" + line;
                        }
                    } else if (line.startsWith("ID: ")) {
                        // example: ID: 1153; Start time: 2019-10-16 23:14:29,990; Process time: 256;

                        lastTrackItem = processTrackItem(parseEndTrackItem(line));
                        dumpItem = null;
                    } else if (line.length() > 25 && Character.isDigit(line.charAt(0)) && line.charAt(19) == ','
                            && line.charAt(24) == '(') {
                        // example:
                        // 2019-10-16 23:14:30,287 (MyEjb-test-service) Start; ID: 1163; TYPE: EJB; name: by.vdnsv.test.ejb.TestEjb.start;
                        // or
                        // 2019-10-16 23:14:30,300 (MyEjb-test-service) ID: 1163; Start time: 2019-10-16 23:14:30,287; Process time: 12; OtherInvocations count: 2; OtherInvocations time: 7; OthersStart: 2019-10-16 23:14:30,288; OthersFinish: 2019-10-16 23:14:30,299;

                        lastTrackItem = processTrackItem(parseStartTrackItem(line));
                        if (lastTrackItem.isStart && "1".equals(lastTrackItem.id)) {
                            // if server was restarted
                            idToItemMap.clear();
                            System.out.println(
                                    "Found server restart in file: " + trackingLogFile.getName() + " time: "
                                            + lastTrackItem.time + " line: " + line);

                        }
                        dumpItem = null;

                    } else {
                        System.err.println("Invalid line: " + line);
                    }

                }
            }
        }

        for (Entry<String, TrackItem> entry : oldIdToItemMapKeys) {
            // keep only last page
            idToItemMap.remove(entry.getKey(), entry.getValue()); // don't search parent items in previous files
        }
    }

    private static TrackItem parseTrackItem(final String line) {
        // RegExp isn't used to reach the maximum performance
        String id = null;
        String type = null;
        String name = null;
        String startTime = null;
        int processTime = 0;
        String parentId = null;
        int otherInvocationsCount = 0;
        int otherInvocationsTime = 0;
        String othersStart = null;
        String othersFinish = null;
        int sqlCount = 0;
        int sqlTime = 0;
        String other = null;

        for (String token : line.split("; ")) {
            if (token.startsWith("ID: ")) {
                id = token.substring(4);
            } else if (token.startsWith("TYPE: ")) {
                type = token.substring(6);
            } else if (token.startsWith("name: ")) {
                name = token.substring(6);
            } else if (token.startsWith("Start time: ")) {
                startTime = token.substring(12);
            } else if (token.startsWith("Process time: ")) {
                processTime = Integer.parseInt(token.substring(14));
            } else if (token.startsWith("ParentID: ")) {
                parentId = token.substring(10);
            } else if (token.startsWith("OtherInvocations count: ")) {
                otherInvocationsCount = Integer.parseInt(token.substring(24));
            } else if (token.startsWith("OtherInvocations time: ")) {
                otherInvocationsTime = Integer.parseInt(token.substring(23));
            } else if (token.startsWith("OthersStart: ")) {
                othersStart = token.substring(13);
            } else if (token.startsWith("OthersFinish: ")) {
                othersFinish = token.substring(14);
            } else if (token.startsWith("SQL count: ")) {
                sqlCount = Integer.parseInt(token.substring(11));
            } else if (token.startsWith("SQL time: ")) {
                sqlTime = Integer.parseInt(token.substring(10));
            } else {
                other = (other == null) ? token : "\n" + token;
            }
        }

        return new TrackItem(startTime, null, name, type, id, parentId, startTime, processTime, otherInvocationsCount,
                otherInvocationsTime, othersStart, othersFinish, sqlCount, sqlTime, other, false);
    }

    private TrackItem parseStartTrackItem(String line) {
        // example:
        // 2019-10-16 23:14:30,287 (MyEjb-test-service) Start; ID: 1163; TYPE: EJB; name: by.vdnsv.test.ejb.TestEjb.start;
        // or
        // 2019-10-16 23:14:30,300 (MyEjb-test-service) ID: 1163; Start time: 2019-10-16 23:14:30,287; Process time: 12; OtherInvocations count: 2; OtherInvocations time: 7; OthersStart: 2019-10-16 23:14:30,288; OthersFinish: 2019-10-16 23:14:30,299;
        // 0123456789012345678901234567890
        //           10        20

        final String time = line.substring(0, 23);
        final int threadNameEndIndex = line.indexOf(") ", 26);
        final String threadName = line.substring(25, threadNameEndIndex);
        int i = threadNameEndIndex + 2;
        // System.out.println(line);
        final boolean isStart = ((line.length() >= i + 6) && "Start;".equals(line.substring(i, i + 6)));
        if (isStart) {
            i += 7;
        }
        TrackItem trackItem = parseTrackItem(line.substring(i));
        if (trackItem.time == null) {
            trackItem.time = time;
        }
        if (isStart && trackItem.processTime == 0) {
            trackItem.processTime = START_ITEM_DEF_PROCESS_TIME;
        }
        trackItem.thread = threadName;
        trackItem.isStart = isStart;
        return trackItem;
    }

    private TrackItem parseEndTrackItem(String line) {
        // example:
        // ID: 1153; Start time: 2019-10-16 23:14:29,990; Process time: 256;

        return parseTrackItem(line);
    }

    private ThreadStatItem parseThreadStatItem(String line) {
        // example:
        // 2019-10-16 23:15:11,396 (SamplerRunner) Dump; states: WAITING=947,BLOCKED=3,TIMED_WAITING=103,RUNNABLE=44; forced: 1; sampled: 37

        return new ThreadStatItem(line.substring(0, 23),
                getIntStatValue(line, "RUNNABLE="),
                getIntStatValue(line, "TIMED_WAITING="),
                getIntStatValue(line, "WAITING="),
                getIntStatValue(line, "BLOCKED="),
                getIntStatValue(line, "forced: "),
                getIntStatValue(line, "sampled: "));
    }

    private static int getIntStatValue(final String line, final String prop) {

        int i = line.indexOf(prop);
        if (i < 0) {
            return 0;
        } else {
            i += prop.length();
            int k = i + 1;
            while (k < line.length() && Character.isDigit(line.charAt(k))) {
                k++;
            }
            return Integer.parseInt(line.substring(i, k));
        }
    }

    private DumpItem parseDumpItem(String line, String time) {
        // example:
        // id: 1026; name: ServerService Thread Pool -- 51; state: BLOCKED; native: false

        String id = null;
        String threadName = null;
        String state = null;
        String isNative = null;
        for (String token : line.split("; ")) {
            if (token.startsWith("id: ")) {
                id = token.substring(4);
            } else if (token.startsWith("name: ")) {
                threadName = token.substring(6);
            } else if (token.startsWith("state: ")) {
                state = token.substring(7);
            } else if (token.startsWith("native: ")) {
                isNative = token.substring(8);
            } else {
                System.err.println("Unknown token: " + token + " in the line " + line);
            }
        }

        DumpItem dumpItem = new DumpItem(time, id, threadName, state, Boolean.parseBoolean(isNative));
        boolean isUnbound = true;
        if (threadName != null) {
            for (int i = trackItems.size() - 1; i >= 0 && i > trackItems.size() - 1000; i--) {
                TrackItem item = trackItems.get(i);
                if (threadName.equals(item.thread) && item.parentId == null) {
                    if (item.isStart) { // don't wire with "end" item
                        if (item.samplingItems == null) {
                            item.samplingItems = new ArrayList<>();
                        }
                        item.samplingItems.add(dumpItem);
                        isUnbound = false;
                    }
                    break;
                }
            }
        }
        if (isUnbound) {
            unboundSamples.add(dumpItem);
        }

        return dumpItem;
    }


    private TrackItem findStartTrackItem(TrackItem endItem) {

        TrackItem startTrackItem = idToItemMap.get(endItem.id);
        /*
        String startItemTime = startTrackItem.startTime;
        if (startItemTime == null) {
            startItemTime = startTrackItem.time;
        }

        String endItemTime = endItem.startTime;
        if (endItemTime == null) {
            endItemTime = endItem.time;
        }
        if (endItemTime != null) {
            ZonedDateTime.of(LocalDateTime.parse(time, dateFormat), ZoneId.systemDefault()).toInstant()
                    .toEpochMilli();
        }
        */

        return startTrackItem;
    }

    private TrackItem processTrackItem(final TrackItem trackItem) {

        TrackItem startTrackItem = null;
        if (!trackItem.isStart && trackItem.id != null) {
            startTrackItem = findStartTrackItem(trackItem);
        }

        if (startTrackItem == null) {
            trackItems.add(trackItem);
            if (trackItem.id != null) {
                idToItemMap.put(trackItem.id, trackItem);
            }
            addToParent(trackItem);
            return trackItem;
        } else {
            if (trackItem.thread != null) {
                startTrackItem.thread = trackItem.thread;
            }
            if (trackItem.name != null) {
                startTrackItem.name = trackItem.name;
            }
            if (trackItem.typ != null) {
                startTrackItem.typ = trackItem.typ;
            }
            if (trackItem.parentId != null) {
                if (startTrackItem.parentId == null) {
                    startTrackItem.parentId = trackItem.parentId;
                    rootItems.remove(startTrackItem);
                    addToParent(startTrackItem);
                }
            }
            if (trackItem.startTime != null) {
                startTrackItem.startTime = trackItem.startTime;
                startTrackItem.time = trackItem.startTime;
            }
            if (trackItem.processTime > 0 || startTrackItem.processTime == START_ITEM_DEF_PROCESS_TIME) {
                startTrackItem.processTime = trackItem.processTime;
            }
            if (trackItem.otherInvocationsCount > 0) {
                startTrackItem.otherInvocationsCount = trackItem.otherInvocationsCount;
            }
            if (trackItem.otherInvocationsTime > 0) {
                startTrackItem.otherInvocationsTime = trackItem.otherInvocationsTime;
            }
            if (trackItem.othersStart != null) {
                startTrackItem.othersStart = trackItem.othersStart;
            }
            if (trackItem.othersFinish != null) {
                startTrackItem.othersFinish = trackItem.othersFinish;
            }
            if (trackItem.sqlCount > 0) {
                startTrackItem.sqlCount = trackItem.sqlCount;
            }
            if (trackItem.sqlTime > 0) {
                startTrackItem.sqlTime = trackItem.sqlTime;
            }
            if (trackItem.other != null) {
                if (startTrackItem.other == null) {
                    startTrackItem.other = trackItem.other;
                } else {
                    if (!startTrackItem.other.contains(trackItem.other)) {
                        startTrackItem.other += "\n" + trackItem.other;
                    }
                }
            }
            startTrackItem.isStart = false;
            return startTrackItem;
        }
    }

    private static int toInt(final String str, int defaultValue) {

        if (str != null) {
            return Integer.parseInt(str);
        } else {
            return defaultValue;
        }
    }


    private void addToParent(TrackItem trackItem) {

        if (trackItem.parentId == null) {
            rootItems.add(trackItem);
        } else {
            TrackItem parentItem = idToItemMap.get(trackItem.parentId);
            if (parentItem == null) {
                parentItem = new TrackItem(trackItem.time, trackItem.thread, "?", "?", trackItem.parentId, null,
                        trackItem.startTime, 0, 0,
                        0,
                        null, null, 0, 0,
                        null, true);
                idToItemMap.put(trackItem.parentId, parentItem);
                rootItems.add(parentItem);
                trackItems.add(parentItem);
            }
            if (parentItem.children == null) {
                parentItem.children = new ArrayList<>();
            }
            parentItem.children.add(trackItem);
        }
    }

    private void buildTree() {

        for (TrackItem trackItem : trackItems) {
            if (trackItem.samplingItems != null && !trackItem.samplingItems.isEmpty()) {
                for (DumpItem dumpItem : trackItem.samplingItems) {
                    expandDump(dumpItem);
                }
            }

            calculateSqlTime(trackItem);
            calculateSqlCount(trackItem);
        }

        for (DumpItem dumpItem : unboundSamples) {
            expandDump(dumpItem);
        }
    }

    private void expandDump(DumpItem dumpItem) {
        // expand samplingItems
        int i = 0;
        while (i < dumpItem.stackTrace.size()) {
            final String hash = dumpItem.stackTrace.get(i);
            List<String> cachedStack;
            if (hash.length() == 40 && ((cachedStack = stackItems.get(hash)) != null)) {
                dumpItem.stackTrace.remove(i);
                dumpItem.stackTrace.addAll(i, cachedStack);
                i += cachedStack.size();
            } else {
                i++;
            }
        }
        Collections.reverse(dumpItem.stackTrace);
    }


    private int calculateSqlTime(TrackItem trackItem) {

        if (trackItem.sqlTime == 0) {
            if (trackItem.children != null && !trackItem.children.isEmpty()) {
                for (TrackItem childTrackItem : trackItem.children) {
                    trackItem.sqlTime += calculateSqlTime(childTrackItem);
                }
            } else if ("SQL".equals(trackItem.typ) || trackItem.sqlCount > 0) {
                trackItem.sqlTime = trackItem.processTime;
            }
        }
        return trackItem.sqlTime;
    }

    private int calculateSqlCount(TrackItem trackItem) {

        if (trackItem.sqlCount == 0) {
            if (trackItem.children != null && !trackItem.children.isEmpty()) {
                for (TrackItem childTrackItem : trackItem.children) {
                    trackItem.sqlCount += calculateSqlCount(childTrackItem);
                }
            } else if ("SQL".equals(trackItem.typ)) {
                trackItem.sqlCount = 1;
            }
        }
        return trackItem.sqlCount;
    }

    public List<TrackItem> getRootItems() {

        return rootItems;
    }

    public List<DumpItem> getUnboundSamples() {

        return unboundSamples;
    }
}
