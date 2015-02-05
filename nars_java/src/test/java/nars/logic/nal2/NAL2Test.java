package nars.logic.nal2;


import nars.build.Curve;
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

public class NAL2Test extends AbstractNALTest {

    public NAL2Test(Build b) { super(b); }

    @Parameterized.Parameters(name= "{0}")
    public static Collection configurations() {
        return Arrays.asList(new Object[][]{
                {new Default()}, //NAL8 + NAL9 didnt solve it
                {new Default().level(3)}, //NAL2 didnt solve it as well as with 3
                {new Default().setInternalExperience(null)},
                {new Curve().setInternalExperience(null)}

                //{new Neuromorphic(4)},
        });
    }

    /** 2.10 */
    @Test
    public void structureTransformation() throws InvalidInputException {
        /*
            /home/me/share/opennars/nars_java/../nal/test/nal2.10.nal '********** structure transformation

                    'Birdie is similar to Tweety
            <Birdie <-> Tweety>. %0.90%

                    'Is Birdie similar to Tweety?
            <{Birdie} <-> {Tweety}>?

                    6

                    'Birdie is similar to Tweety.
                    ''outputMustContain('<{Birdie} <-> {Tweety}>. %0.90;0.73%')

            @1750        */

        long time = 160;
        //TextOutput.out(n);

        n.believe("<Birdie <-> Tweety>", Eternal, 0.9f, 0.9f)
                .en("Birdie is similar to Tweety.");
        n.step(1);
        n.ask("<{Birdie} <-> {Tweety}>")
                .en("Is Birdie similar to Tweety?");

        n.mustBelieve(time, "<{Birdie} <-> {Tweety}>", 0.8f, 0.95f, 0.70f, 0.76f)
                .en("Birdie is similar to Tweety.");

    }
}

