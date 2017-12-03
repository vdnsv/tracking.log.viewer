package by.ep.util.trackviewer.data;

import java.util.stream.Collectors;

import by.ep.util.trackviewer.filter.FieldsProvider;

public class TrackItemFieldsProvider extends FieldsProvider<TrackItem, Object> {

    @Override
    public Object apply(final TrackItem trackItem, final String variableName) {

        switch (variableName) {
            case "name":
                return trackItem.name;
            case "duration":
                return trackItem.processTime;
            case "time":
                return trackItem.time;
            case "thread":
                return trackItem.thread;
            case "typ":
                return trackItem.typ;
            case "id":
                return trackItem.id;
            case "parentId":
                return trackItem.parentId;
            case "startTime":
                return trackItem.startTime;
            case "otherInvocationsCount":
                return trackItem.otherInvocationsCount;
            case "otherInvocationsTime":
                return trackItem.otherInvocationsTime;
            case "othersStart":
                return trackItem.othersStart;
            case "othersFinish":
                return trackItem.othersFinish;
            case "sqlCount":
                return trackItem.sqlCount;
            case "sqlTime":
                return trackItem.sqlTime;
            case "params":
                return trackItem.other;
            case "dump":
                return (trackItem.samplingItems == null || trackItem.samplingItems.isEmpty()) ? "" :
                        trackItem.samplingItems.stream()
                                .filter((DumpItem dumpItem) -> !dumpItem.stackTrace
                                        .isEmpty())
                                .flatMap(dumpItem -> dumpItem.stackTrace.stream()).collect(Collectors.joining(", "));
            default:
                throw new IllegalArgumentException("Invalid variable name: " + variableName);
        }
    }
}
