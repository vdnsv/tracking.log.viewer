package by.ep.util.trackviewer.filter;

public abstract class Expression {

    public abstract Object execute(Object object, FieldsProvider fieldsProvider);

    static class Num extends Expression {

        private final long value;

        Num(long x) {

            value = x;
        }

        @Override
        public Object execute(Object object, FieldsProvider fieldsProvider) {

            return value;
        }
    }

    static class Str extends Expression {

        private final String value;

        Str(String x) {

            value = x;
        }

        @Override
        public Object execute(Object object, FieldsProvider fieldsProvider) {

            return value;
        }
    }

    static class Var extends Expression {

        private final String name;

        Var(String name) {

            this.name = name;
        }

        @Override
        public Object execute(Object object, FieldsProvider fieldsProvider) {

            return fieldsProvider.apply(object, name);
        }
    }

    static class Unary extends Expression {

        private final Expression expr;
        private final boolean not;

        Unary(Expression e, String op) {

            expr = e;
            not = "!".equals(op);
        }

        @Override
        public Object execute(Object object, FieldsProvider fieldsProvider) {

            Object o = expr.execute(object, fieldsProvider);
            if (not) {
                return !(Boolean) o;
            } else {
                return -(Long) o;
            }
        }
    }

    static class Binary extends Expression {

        private final Expression x1;
        private final Expression x2;
        private final String op;

        Binary(Expression x1, Expression x2, String op) {

            this.x1 = x1;
            this.x2 = x2;
            this.op = op;
        }

        @Override
        public Object execute(Object object, FieldsProvider fieldsProvider) {

            Object o1 = x1.execute(object, fieldsProvider);
            Object o2 = x2.execute(object, fieldsProvider);

            Class type = commonType(o1, o2);

            if (type == String.class) {
                return execStr(o1 != null ? o1.toString() : null, o2 != null ? o2.toString() : null);
            } else if (type == Long.class) {
                return execNum(toLong(o1), toLong(o2));
            } else {
                return execBool((Boolean) o1, (Boolean) o2);
            }
        }

        private Long toLong(Object o) {

            if (o instanceof Long) {
                return (Long) o;
            }
            return Long.valueOf((Integer) o);
        }

        private Class commonType(Object o1, Object o2) {

            if (o1 == null || o2 == null || o1 instanceof String || o2 instanceof String) {
                return String.class;
            }
            if ((o1 instanceof Integer || o1 instanceof Long) && (o2 instanceof Integer || o2 instanceof Long)) {
                return Long.class;
            }
            return Boolean.class;
        }

        private Object execStr(String s1, String s2) {

            if ("==".equals(op) || "=".equals(op)) {
                return (Boolean) (s1 == null ? s2 == null : /*s1.equals(s2)*/ s1.contains(s2));
            }
            if ("!=".equals(op)) {
                return (Boolean) (s1 == null ? s2 != null : /*!s1.equals(s2)*/ !s1.contains(s2));
            }
            if ("+".equals(op)) {
                return (String) (s1 == null ? s2 : s1 + (s2 == null ? "" : s2));
            }
            if ("<".equals(op)) {
                return s1 == null ? s2 != null : s1.compareTo(s2) < 0;
            }
            if (">".equals(op)) {
                return s1 != null && s1.compareTo(s2) > 0;
            }
            throw new IllegalStateException("Illegal String operator: " + op);
        }

        private Object execBool(boolean q1, boolean q2) {

            if ("&&".equals(op)) {
                return q1 && q2;
            }
            if ("||".equals(op)) {
                return q1 || q2;
            }
            if ("==".equals(op) || "=".equals(op)) {
                return q1 == q2;
            }
            if ("!=".equals(op)) {
                return q1 != q2;
            }
            throw new IllegalStateException("Illegal Boolean operator: " + op);
        }

        private Object execNum(long n1, long n2) {

            if ("==".equals(op) || "=".equals(op)) {
                return (Boolean) (n1 == n2);
            }
            if ("!=".equals(op)) {
                return (Boolean) (n1 != n2);
            }
            if ("<".equals(op)) {
                return (Boolean) (n1 < n2);
            }
            if ("<=".equals(op)) {
                return (Boolean) (n1 <= n2);
            }
            if (">".equals(op)) {
                return (Boolean) (n1 > n2);
            }
            if (">=".equals(op)) {
                return (Boolean) (n1 >= n2);
            }
            if ("+".equals(op)) {
                return (Long) (n1 + n2);
            }
            if ("-".equals(op)) {
                return (Long) (n1 - n2);
            }
            if ("*".equals(op)) {
                return (Long) (n1 * n2);
            }
            if ("/".equals(op)) {
                return (Long) (n1 / n2);
            }

            throw new IllegalStateException("Illegal Long operator: " + op);
        }
    }
}
