package nars.nal.meta.pre;

import nars.$;
import nars.Premise;
import nars.nal.RuleMatch;
import nars.task.Temporal;
import nars.term.Term;

/**
 * Created by me on 8/15/15.
 */
public class MeasureTime extends AbstractMeasureTime {

    public MeasureTime(Term arg1) {
        super(arg1);
    }

    /**
     * HACK this ignores the parameters 'a' and 'b'
     * because this rule only appears once we hardcode
     * what it means.  The third parameter specifies the
     * pattern variable where interval
     * term representing the time difference will be
     * substituted.
     */
    @Override protected final boolean testEvents(RuleMatch m, Term target) {
        Premise p = m.premise;

        int time = Temporal.between(
            (Temporal)p.getTask(),
            (Temporal)p.getBelief()
        );
        if (time < 0) { //should this be <= 0  ??
            return false;
        }

        return m.putXY(target, $.cycles(time));
    }
}
