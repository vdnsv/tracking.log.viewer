package by.ep.util.trackviewer.ui;

import by.ep.util.trackviewer.filter.Expression;
import by.ep.util.trackviewer.filter.TrackingExpressionBuilder;

public class FilterExpressionParser {

    private String expression;

    public FilterExpressionParser(String expression) {

        this.expression = expression;
    }

    public Expression parse() {

        return TrackingExpressionBuilder.build(expression);
    }
}
