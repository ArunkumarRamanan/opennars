package nars.logic.nal9;

import nars.build.Default;
import nars.core.NewNAR;
import nars.io.narsese.InvalidInputException;
import nars.logic.JavaNALTest;
import nars.logic.nal7.Tense;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by me on 1/20/15.
 */
public class AnticipationTest extends JavaNALTest {

    public AnticipationTest(NewNAR b) { super(b); }

    @Parameterized.Parameters(name= "{0}")
    public static Collection configurations() {
        return Arrays.asList(new Object[][]{
                {new Default()},


                //TODO compare result with ^anticipate enabled vs. disabled
                {new Default().setInternalExperience(null)},
        });
    }

    @Test //test the occurrence time feature of TaskCondition
    public void testOcurrenceTimeTesting()  throws InvalidInputException {
        nar.believe("<a --> b>", Tense.Present, 1.0f, 0.9f);
        nar.mustOutput(0, 50, "<a --> b>", '.', 0.00f, 1.00f, 0.0f, 1.0f, -55, 0);
        nar.run();
    }


    @Test
    public void testAnticipation1() throws InvalidInputException {

        /*
         <(&/,<a --> b>,+3) =/> <b --> c>>.
         <a --> b>. :|:
         25

        ''outputMustContain('<(&/,<a --> b>,+3) =/> <b --> c>>. :\: %0.00;0.45%')
        ''outputMustContain('<(&/,<a --> b>,+3) =/> <b --> c>>. :\: %0.92;0.91%')
         */
        String rule = "<(&/,<a --> b>,+3) =/> <b --> c>>";
        long time = 65;

        //TextOutput.out(n);

        nar.believe(rule, Tense.Eternal, 1.0f, 0.9f);
        nar.believe("<a --> b>", Tense.Present, 1.0f, 0.9f);

        //The actual output from Anticipate: <b --> c>. :\: %0.00;0.90%
        nar.mustOutput(0, time, "<b --> c>", '.', 0f, 0f, 0.9f, 0.9f, (int) (-1*time), 0);

        //The induced result
        nar.mustOutput(0, time, rule, '.', 0.00f, 0.00f, 0.40f, 0.50f, -25, 0);
        nar.mustOutput(0, time, rule, '.', 0.91f, 0.93f, 0.90f, 0.91f, -25, 0);
        nar.mustOutput(0, time, rule, '.', 0.00f, 1.00f, 0.0f, 1.0f, (int) (-1*time), 0); //match any

        nar.run();


    }

}
