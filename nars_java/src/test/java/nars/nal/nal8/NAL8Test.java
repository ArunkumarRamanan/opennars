package nars.nal.nal8;

import nars.prototype.Curve;
import nars.prototype.Default;
import nars.ProtoNAR;
import nars.Global;
import nars.io.narsese.InvalidInputException;
import nars.nal.JavaNALTest;
import nars.nal.nal7.Tense;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;


public class NAL8Test extends JavaNALTest {

    public NAL8Test(ProtoNAR b) { super(b); }

    @Parameterized.Parameters(name= "{0}")
    public static Collection configurations() {
        return Arrays.asList(new Object[][]{
                {new Default()},
                {new Default().setInternalExperience(null)},
                {new Curve().setInternalExperience(null)},
                {new Default.DefaultMicro() }
                //{new Discretinuous() },
        });
    }

    @Test public void testQuest() throws InvalidInputException {

        String goal = "<a --> b>";

        nar.goal(Global.DEFAULT_GOAL_PRIORITY, Global.DEFAULT_GOAL_DURABILITY, goal, 1.0f, 0.9f);

        nar.run(50);

        nar.mustDesire(60, goal, 1.0f, 0.9f);
        nar.quest(goal);

        nar.run(10);
    }

    protected void testGoalExecute(String condition, String action) {

        //TextOutput.out(nar);

        nar.believe(condition, Tense.Present, 1.0f, 0.9f);
        nar.goal("(&/,"+ condition + ',' + action + ")", 1.0f, 0.9f);

        nar.mustDesire(100, action, 1.0f, 0.43f);

        nar.mustOutput(1, 10, action, '.', 1f, 1f, 0.98f, 0.99f, 0); // :|: %1.00;0.99%"); //TODO use an ExecuteCondition instance


        nar.run(100);
    }

    @Test public void testGoalExecute1() {
        /* 8.1.14
        ********** [24 + 12 -> 25]
        IN: <(*,SELF,{t002}) --> reachable>. :|:
        IN: (&/,<(*,SELF,{t002}) --> reachable>,(^pick,{t002}))!
                45
        ''outputMustContain('(^pick,{t002},SELF)! %1.00;0.43%')
        */


        testGoalExecute("<(*,SELF,{t002}) --> reachable>", "(^pick,{t002},SELF)");
    }

    @Test public void testGoalExecute2() {
        /* 8.1.14
        ********** [24 + 12 -> 25]
        IN: <(*,SELF,{t002}) --> reachable>. :|:
        IN: (&/,<(*,SELF,{t002}) --> reachable>,(^pick,{t002}))!
                45
        ''outputMustContain('(^pick,{t002},SELF)! %1.00;0.43%')
        */


        testGoalExecute("<a --> b>", "(^pick,<c --> d>,SELF)");
    }


    @Test public void testOperationInheritance() {
        nar.input("pick(c). :|:");
        nar.run(6);
        nar.input("<a --> b>. :|:");
        nar.input("<a --> b>!");
        /*nar.on(Events.TaskDerive.class, new Reaction() {
            @Override
            public void event(Class event, Object[] args) {
                Task t = (Task) args[0];
                System.out.println("Derived: " + t);
            }
        });
        nar.on(Events.TaskRemove.class, new Reaction() {
            @Override
            public void event(Class event, Object[] args) {
                Task t = (Task) args[0];
                System.out.println("Remove: " + t + " " + t.getReason());
            }
        });*/
        nar.mustBelieve(100, "(^pick,c,SELF)", 1f, 1f, 0.47f, 0.47f);//this is checking for the eternalized result, but there are non-eternalized results that occur before that
        nar.run(100);

    }

}
