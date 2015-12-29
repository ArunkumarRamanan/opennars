package nars.nal.meta.pre;

import nars.Premise;
import nars.nal.RuleMatch;
import nars.term.Term;

/**
 * Created by me on 8/15/15.
 */
public abstract class AbstractMeasureTime extends PreCondition1Output {


    protected AbstractMeasureTime(Term target) {
        super(target);
    }

    @Override
    public boolean test(RuleMatch m, Term target) {
        Premise premise = m.premise;

        if (!premise.isEvent())
            return false;

        return testEvents(m, target);
    }

    protected abstract boolean testEvents(RuleMatch m, Term target);
}
