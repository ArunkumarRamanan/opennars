package nars.core;

import nars.Global;
import nars.prototype.Solid;
import nars.io.Answered;
import nars.io.test.TestNAR;
import nars.nal.Sentence;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;


public class SolidTest {

    @Test
    public void testDetective() throws FileNotFoundException {

        int time = 16; //should solve the example in few cycles

        Global.DEBUG = true;

        Solid s = new Solid(3, 256, 1, 10, 1, 4);
        s.param.duration.set(3);
        s.param.noveltyHorizon.set(2);

        TestNAR n = new TestNAR(s);

        //TextOutput.out(n).setOutputPriorityMin(1.0f);

        AtomicInteger solutions = new AtomicInteger(0);

        n.input(new File("../nal/other/detective.nal"));

        new Answered(n) {

            @Override
            public void onSolution(Sentence belief) {
                solutions.incrementAndGet();
            }

        };

        n.step(time);
        assertEquals(2, solutions.get());
    }


        /*
        n.input("<a --> b>. %1.00;0.90%\n" +
                "<b --> c>. %1.00;0.90%\n"+
                "<c --> d>. %1.00;0.90%\n" +
                "<a --> d>?");*/
        //''outputMustContain('<a --> d>. %1.00;0.27%')


}
