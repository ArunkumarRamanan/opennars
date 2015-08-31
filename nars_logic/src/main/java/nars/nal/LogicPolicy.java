package nars.nal;

import com.google.common.collect.Lists;
import nars.Global;
import nars.premise.Premise;
import nars.task.Sentence;
import nars.task.Task;
import nars.task.TaskSeed;
import nars.task.filter.DerivationFilter;

import java.util.List;

/**
 * General class which specifies NAL rules, their ordering, and other parameters applied during inference processes
 */
public class LogicPolicy {

    public final RuleList<Premise> conceptProcessing;
    public final List<DerivationFilter> derivationFilters;

    public LogicPolicy() {
        this(null,null);
    }
    public LogicPolicy(LogicStage<Premise>[] conceptProcessRules, DerivationFilter[] derivationFilters) {

        conceptProcessing = new RuleList<Premise>(conceptProcessRules);

        if (derivationFilters!=null)
            this.derivationFilters = Lists.newArrayList(derivationFilters);
        else
            this.derivationFilters = Global.newArrayList();

    }


    public void fire(final Premise fireConcept) {
        final List<LogicStage<Premise>> rules = conceptProcessing.rules;
        final int n = rules.size();
        for (int l = 0; l < n; l++) {
            if (!rules.get(l).test(fireConcept))
                break;
        }
    }

    public List<DerivationFilter> getDerivationFilters() {
        return derivationFilters;
    }

    /** tests validity of a derived task; if valid returns null, else returns a String rule explaining why it is invalid */
    public String getDerivationRejection(final Premise nal, final Task task, final boolean solution, final boolean revised, final boolean single, final Sentence currentBelief, final Task currentTask) {

        List<DerivationFilter> derivationFilters = getDerivationFilters();
        final int dfs = derivationFilters.size();

        for (int i = 0; i < dfs; i++) {
            DerivationFilter d = derivationFilters.get(i);
            String rejectionReason = d.reject(nal, task, solution, revised);
            if (rejectionReason != null) {
                return rejectionReason;
            }
        }
        return null;
    }
}
