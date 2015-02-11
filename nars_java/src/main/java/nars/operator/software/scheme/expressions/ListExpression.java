package nars.operator.software.scheme.expressions;


import nars.operator.software.scheme.cons.Cons;

import java.util.stream.Collectors;

import static nars.operator.software.scheme.cons.Cons.copyOf;


public class ListExpression implements Expression {
    public static class Nil extends ListExpression {
        private static final Nil NIL = new Nil();

        private Nil() {
            super(Cons.<Expression>empty());
        }

        public static Nil nil() {
            return NIL;
        }
    }

    public final Cons<Expression> value;

    public ListExpression(Cons<Expression> value) {
        this.value = value;
    }

    public static ListExpression list(Cons<Expression> list) {
        if (list == Cons.<Expression>empty()) {
            return Nil.nil();
        }

        return new ListExpression(list);
    }

    public static ListExpression list(Expression... exps) {
        return ListExpression.list(copyOf(exps));
    }

    @Override
    public boolean equals(Object o) {
        return getClass() == o.getClass() && value.equals(((ListExpression) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public String toString() {
        return String.format("list(%s)", value.stream()
                .map(Object::toString)
                .reduce((a, b) -> a + ", " + b)
                .orElse(""));
    }

    @Override
    public String print() {
        return String.format("(%s)", value.stream()
                .map(Expression::print)
                .collect(Collectors.joining(" ")));
    }
}
