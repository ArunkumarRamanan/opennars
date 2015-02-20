package nars.core;


import nars.logic.BudgetFunctions;
import nars.logic.entity.BudgetValue;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestBudget {

    @Test
    public void testForgetPeriodic() {

        //TODO write some comparison tests for budgets of different durability after time T

        int forgetCycles = 5;
        int maxTime = 100;

        float initPriority = 1f;
        float durability = 0.99f;
        float quality = 0.5f;
        float budgetThreshold = 0.1f;

        BudgetValue b = new BudgetValue(initPriority, durability, quality);

        for (int t = 0; t < maxTime; t++) {
            BudgetFunctions.forgetPeriodic(b, forgetCycles, budgetThreshold, t);
            System.out.println(t + "," + b.getPriority() + "," + b.getDurability() + "," + b.getQuality());
        }

    }
}
