package nars.core;


import nars.build.Default;
import nars.io.TextOutput;
import nars.io.condition.OutputCount;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NALLevelTest {



    @Test
    public void testLevel1vs8() {
        Parameters.DEBUG = true;

        NAR nDefault = new NAR(new Default());
        assertEquals(Parameters.DEFAULT_NAL, nDefault.nal());

        NAR n1 = new NAR(new Default().level(1));
        OutputCount n1Count = new OutputCount(n1);
        assertEquals(1, n1.nal());

        NAR n8 = new NAR(new Default().level(8));
        OutputCount n8Count = new OutputCount(n8);

        TextOutput.out(n8);

        String productSentence = "<(*,a,b) --> c>.";

        n1.addInput(productSentence);
        n1.run(5);

        n8.addInput(productSentence);
        n8.run(5);

        assertTrue("NAL8 will accept sentence containing a Product", n8Count.getOutputs() > 1);
        assertEquals("NAL1 will NOT accept sentence containing a Product", 0, n1Count.getOutputs() + n1Count.getOthers());



    }
}
