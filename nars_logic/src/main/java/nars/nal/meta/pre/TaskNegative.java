package nars.nal.meta.pre;

import nars.nal.RuleMatch;
import nars.nal.meta.PostCondition;
import nars.nal.meta.PreCondition;
import nars.task.Task;

/**
 * Created by me on 8/15/15.
 */
public final class TaskNegative extends PreCondition {

    @Override
    public boolean eval(RuleMatch m) {
        Task task = m.premise.getTask();
        return (task.isJudgmentOrGoal() && task.getFrequency() < PostCondition.HALF);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }


}
