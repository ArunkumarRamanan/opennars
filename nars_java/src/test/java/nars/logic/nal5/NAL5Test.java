package nars.logic.nal5;

import nars.build.Default;
import nars.build.Neuromorphic;
import nars.core.Build;
import nars.io.TextOutput;
import nars.io.narsese.InvalidInputException;
import nars.logic.AbstractNALTest;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static nars.logic.nal7.Tense.Eternal;


public class NAL5Test extends AbstractNALTest {

    public NAL5Test(Build b) { super(b); }

    @Parameterized.Parameters(name= "{0}")
    public static Collection configurations() {
        return Arrays.asList(new Object[][]{
                { new Default() },
                { new Default().setInternalExperience(null) },
                { new Default().level(5) },
                { new Neuromorphic(4) },
        });
    }

    /** 5.15 */
    @Test public void compoundDecompositionTwoPremises() throws InvalidInputException {
        /*
        'If robin is a type of bird then robin is not a type of flying animal.
        <<robin --> bird> ==> (&&,<robin --> animal>,<robin --> [flying]>)>. %0%

        'If robin is a type of bird then robin can fly.
        <<robin --> bird> ==> <robin --> [flying]>>.

        8

        'It is unlikely that if a robin is a type of bird then robin is a type of animal.
        ''outputMustContain('<<robin --> bird> ==> <robin --> animal>>. %0.00;0.81%')
        */

        long time = 525;
        //TextOutput.out(n);

        n.believe("<<robin --> bird> ==> (&&,<robin --> animal>,<robin --> [flying]>)>", Eternal, 0.0f, 0.9f)
                .en("If robin is a type of bird then robin is not a type of flying animal.");
        n.believe("<<robin --> bird> ==> <robin --> [flying]>>", Eternal, 1f, 0.9f )
                .en("If robin is a type of bird then robin can fly.");

        n.mustBelieve(time, "<<robin --> bird> ==> <robin --> animal>>", 0f, 0f, 0.81f, 0.81f)
                .en("It is unlikely that if a robin is a type of bird then robin is a type of animal.");

    }

    /** 5.19 */
    @Test public void compoundDecompositionOnePremise() throws InvalidInputException {
        /*

        'Robin can fly and swim.
        $0.90;0.90$ (&&,<robin --> swimmer>,<robin --> [flying]>). %0.9%
        1
        'Robin can swim.
        ''outputMustContain('<robin --> swimmer>. %0.90;0.73%')
        5
        ''//+2 from original
        'Robin can fly.
        ''outputMustContain('<robin --> [flying]>. %0.90;0.73%')

        */

        long time = 525;


        n.believe("(&&,<robin --> swimmer>,<robin --> [flying]>)", Eternal, 0.9f, 0.9f)
                .en("robin can fly and swim.")
                .en("robin is one of the flying and is a swimmer.");

        n.mustBelieve(time, "<robin --> swimmer>", 0.90f, 0.90f, 0.73f, 0.73f)
                .en("robin can swim.");
        n.mustBelieve(time, "<robin --> [flying]>", 0.90f, 0.90f, 0.73f, 0.73f)
                .en("robin can fly.")
                .en("robin is one of the flying.");

    }
    
}
