package by.ep.util.trackviewer.filter;

public class TrackingExpressionBuilder {
    private String expression;
    private int p = 0;

    public static Expression build(String expression) {

        TrackingExpressionBuilder builder = new TrackingExpressionBuilder(expression);
        builder.skip(" ");
        return builder.build(0);
    }

    private TrackingExpressionBuilder(String expression) {

        this.expression = expression;
    }

    private Expression build(int state) {

        if (lastState(state)) {
            Expression ex;
            boolean isMinus = startWith("-");
            if (isMinus) {
                skip("-");
            }

            if (startWith("(")) {
                skip("(");
                ex = build(0);
                skip(")");
            } else {
                ex = readSingle();
            }
            if (isMinus) {
                ex = new Expression.Unary(ex, "-");
            }
            return ex;
        }

        boolean unarNot = state == 2 && startWith("!");
        if (unarNot) {
            skip("!");
        }

        Expression a1 = build(state + 1);
        if (unarNot) {
            a1 = new Expression.Unary(a1, "!");
        }

        String op;
        while ((op = readStateOperator(state)) != null) {
            Expression a2 = build(state + 1);
            a1 = new Expression.Binary(a1, a2, op);

        }
        return a1;
    }

    private static String[][] states = new String[][]{
            {"||"},
            {"&&"},
            {"!"},
            {"<=", ">=", "==", "!=", "<", ">", "="},
            {"+", "-"},
            {"*", "/"},
            null
    };

    private boolean lastState(int s) {

        return s + 1 >= states.length;
    }

    private boolean startWith(String s) {

        return expression.startsWith(s, p);
    }

    private void skip(String s) {

        if (startWith(s)) {
            p += s.length();
        }
        while (p < expression.length() && expression.charAt(p) == ' ') {
            p++;
        }
    }


    private String readStateOperator(int state) {

        String[] ops = states[state];
        for (String s : ops) {
            if (startWith(s)) {
                skip(s);
                return s;
            }
        }
        return null;
    }

    private Expression readSingle() {

        int p0 = p;
        if (startWith("'") || startWith("\"")) {
            boolean q2 = startWith("\"");
            p = expression.indexOf(q2 ? '"' : '\'', p + 1);
            Expression ex = new Expression.Str(expression.substring(p0 + 1, p));
            skip(q2 ? "\"" : "'");
            return ex;
        }

        while (p < expression.length()) {
            if (!(Character.isLetterOrDigit(expression.charAt(p)))) {
                break;
            }
            p++;
        }

        if (p > p0) {
            String s = expression.substring(p0, p);
            skip(" ");
            try {
                long x = Long.parseLong(s);
                return new Expression.Num(x);
            } catch (Exception e) {
                // do nothing
            }

            if ("null".equals(s)) {
                return new Expression.Str(null);
            }

            return new Expression.Var(s);

        }
        return null;
    }
}
