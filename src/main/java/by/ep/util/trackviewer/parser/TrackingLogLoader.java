package by.ep.util.trackviewer.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import by.ep.util.trackviewer.data.DumpItem;
import by.ep.util.trackviewer.data.ThreadStatItem;
import by.ep.util.trackviewer.data.TrackItem;

public class TrackingLogLoader {
    // private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

    private static final int START_ITEM_DEF_PROCESS_TIME = 999999;
    private static final String TIME_PATTERNS = "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3})";
    private static final String THREAD_INTERNAL_PATTERN = "([^()]+(\\([^()]+\\))*)";
    private static final String THREAD_PATTERN = "(\\(" + THREAD_INTERNAL_PATTERN + "\\))";

    private static final Pattern START_PATTERN = Pattern.compile(
            TIME_PATTERNS
                    + " " + THREAD_PATTERN
                    + "( Start;)?"
                    + "( ID: (\\d+);)?"
                    + "( TYPE: ([^;]+);)?"
                    + "( name: ([^;]+);)?"
                    + "( Start time: " + TIME_PATTERNS + ";)?"
                    + "( Process time: (\\d+);)?"
                    + "( ParentID: (\\d+);)?"
                    + "( OtherInvocations count: (\\d+);)?"
                    + "( OtherInvocations time: (\\d+);)?"
                    + "( OthersStart: " + TIME_PATTERNS + ";)?"
                    + "( OthersFinish: " + TIME_PATTERNS + ";)?"
                    + "( SQL count: (\\d+);)?"
                    + "( SQL time: (\\d+);)?"
                    + "( .+)?"
    );

    private static final Pattern END_PATTERN = Pattern.compile(
            "ID: (\\d+);"
                    + "( TYPE: ([^;]+);)?"
                    + "( name: ([^;]+);)?"
                    + "( Start time: " + TIME_PATTERNS + ";)?"
                    + "( Process time: (\\d+);)?"
                    + "( ParentID: (\\d+);)?"
                    + "( OtherInvocations count: (\\d+);)?"
                    + "( OtherInvocations time: (\\d+);)?"
                    + "( OthersStart: " + TIME_PATTERNS + ";)?"
                    + "( OthersFinish: " + TIME_PATTERNS + ";)?"
                    + "( SQL count: (\\d+);)?"
                    + "( SQL time: (\\d+);)?"
                    + "( .+)?"
    );

    private static final Pattern DUMP_START_PATTERN = Pattern.compile(
            TIME_PATTERNS + " \\(SamplerRunner\\) Dump; states: ([_A-Z]+=(\\d+),?)+; forced: (\\d+); sampled: (\\d+)"
    );

    private static final Pattern DUMP_THREAD_PATTERN = Pattern.compile(
            "id: (\\d+); name: " + THREAD_INTERNAL_PATTERN + "; state: ([^;]+); native: ([^)]+)"
    );

    private static final Pattern DUMP_PATTERN = Pattern.compile(
            "\t([a-f0-9]{40})"
    );

    private static final Pattern STACK_START_PATTERM = Pattern.compile(
            "^" + TIME_PATTERNS + " \\(SamplerRunner\\) StoreStateDelta; Size: (\\d+); Memory KB: (\\d+)"
    );

    private static final Pattern STACK_BLOCK_START = Pattern.compile(
            "^([a-f0-9]{40})$"
    );

    private static final String ADDITIONAL_INFO_PATTERN_STR = "^Additional Info: (.+)$";


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
                    } else if (line.length() == 40 && tryPattern(STACK_BLOCK_START, line) != null) {
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
                if (!line.trim().isEmpty() && !line.contains(" (SamplerRunner) Tick: ")) {
                    Matcher matcher;
                    if (dumpItem != null && line.startsWith("\t")
                            && (matcher = tryPattern(DUMP_PATTERN, line)) != null) {
                        dumpItem.stackTrace.add(matcher.group(1));
                    } else if (line.contains(" (SamplerRunner) Dump; states: ")
                            && (matcher = tryPattern(DUMP_START_PATTERN, line)) != null) {
                        threadStatItem = new ThreadStatItem(matcher.group(1),
                                0, 0, 0, // FIXME
                                Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)));
                        threadStatistics.add(threadStatItem);
                    } else if (line.startsWith("id: ") && (matcher = tryPattern(DUMP_THREAD_PATTERN, line)) != null) {
                        dumpItem = new DumpItem(threadStatItem == null ? null : threadStatItem.time, matcher.group(1),
                                matcher.group(2), matcher.group(4),
                                Boolean.parseBoolean(matcher.group(5)));
                        final String threadName = dumpItem.thread;
                        boolean isUnbound = true;
                        if (threadName != null && !dumpItem.isNative) {
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
                    } else if ((matcher = tryPattern(START_PATTERN, line)) != null) {
                        boolean isStartItem = " Start;".equals(matcher.group(5));
                        TrackItem trackItem = new TrackItem(
                                matcher.group(1),
                                matcher.group(3),
                                matcher.group(11),
                                matcher.group(9),
                                matcher.group(7),
                                matcher.group(17),
                                matcher.group(13),
                                toInt(matcher.group(15), isStartItem ? START_ITEM_DEF_PROCESS_TIME : 0),
                                toInt(matcher.group(19), 0),
                                toInt(matcher.group(21), 0),
                                matcher.group(23),
                                matcher.group(25),
                                toInt(matcher.group(27), 0),
                                toInt(matcher.group(29), 0),
                                matcher.group(30),
                                isStartItem
                        );

                        if (trackItem.id != null) {
                            int newId = Integer.parseInt(trackItem.id);
                            if (newId == 1) {
                                // if server was restarted
                                idToItemMap.clear();
                                System.out.println(
                                        "Found server restart in file: " + trackingLogFile.getName() + " time: "
                                                + trackItem.time + " line: " + line);
                            }
                        }

                        lastTrackItem = processTrackItem(trackItem, isStartItem);
                        dumpItem = null;
                    } else if ((matcher = tryPattern(END_PATTERN, line)) != null) {

                        final String curItemId = matcher.group(3);
                        TrackItem trackItem = new TrackItem(
                                matcher.group(7),
                                null,
                                matcher.group(5),
                                curItemId,
                                matcher.group(1),
                                matcher.group(11),
                                matcher.group(7),
                                toInt(matcher.group(9), 0),
                                toInt(matcher.group(13), 0),
                                toInt(matcher.group(15), 0),
                                matcher.group(17),
                                matcher.group(19),
                                toInt(matcher.group(21), 0),
                                toInt(matcher.group(23), 0),
                                matcher.group(24),
                                idToItemMap.get(curItemId) == null
                        );
                        lastTrackItem = processTrackItem(trackItem, false);
                        dumpItem = null;
                    } else if (line.startsWith("Additional Info: ")) {
                        if (lastTrackItem != null) {
                            lastTrackItem.other =
                                    lastTrackItem.other == null ? line : lastTrackItem.other + "; " + line;
                        }
                    } else {
                        System.err.println("Error line: " + line);
                    }
                }
            }
        }

        for (Entry<String, TrackItem> entry : oldIdToItemMapKeys) {
            // keep only last page
            idToItemMap.remove(entry.getKey(), entry.getValue()); // don't search parent items in previous files
        }
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

    private TrackItem processTrackItem(final TrackItem trackItem, boolean isStartItem) {

        TrackItem startTrackItem = null;
        if (!isStartItem && trackItem.id != null) {
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
                    startTrackItem.other += "; " + trackItem.other;
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

    private static Matcher tryPattern(final Pattern pattern, final String str) {

        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher;
        }
        return null;
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
