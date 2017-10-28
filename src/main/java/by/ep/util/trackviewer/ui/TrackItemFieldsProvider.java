package by.ep.util.trackviewer.ui;

import by.ep.util.trackviewer.filter.FieldsProvider;
import by.ep.util.trackviewer.data.TrackItem;

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
            case "typ":
                return trackItem.typ;
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


        }
        return null;
    }
}
