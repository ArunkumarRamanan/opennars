package nars.io;


import nars.build.Default;
import nars.core.Events;
import nars.core.NAR;
import nars.core.Parameters;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class BufferedOutputTest {


    @Test
    public void testBufferedOutput1() {

        Parameters.DEBUG = true;
        Parameters.EXIT_ON_EXCEPTION = true;

        final int maxBufferCost = 10;
        final int minOutputInterval = 5;
        NAR n = new NAR(new Default.DefaultMicro());
        new BufferedOutput(n, 1, minOutputInterval, maxBufferCost) {

            SummaryStatistics in = new SummaryStatistics();
            SummaryStatistics ex = new SummaryStatistics();

            @Override
            public float cost(Class event, Object o) {
                if (event == Events.ERR.class) {
                    String errMsg = o instanceof Object [] ? Arrays.toString((Object[]) o).toString() : o.toString();
                    System.err.println(errMsg);
                    System.exit(1);
                    assertTrue(errMsg, false);
                }

                return super.cost(event, o);
            }

            @Override
            protected void output(List<OutputItem> buffer) {
                /*System.out.println(n.time() + ": " + buffer);
                System.out.println("  in:" + in.getMean() + "," + in.getSum()
                        + " ex:" + ex.getMean() + "," + ex.getSum());
                */

                if (in.getN() > 0 && ex.getN() > 0) {
                    assertTrue(in.getSum() + " exceeds limit of " + maxBufferCost + ": " + buffer.toString(),
                            in.getSum() - maxBufferCost < 0.1);
                    assertTrue(!toString(buffer).isEmpty());
                }
                in.clear();
                ex.clear();

            }

            @Override
            protected void included(OutputItem o) {
                in.addValue(o.cost);
            }

            @Override
            protected void excluded(OutputItem o) {
                ex.addValue(o.cost);
            }
        };
        n.input("<a --> [b]>.\n <b --> c>. \n <{c} --> a>.\n");
        n.run(64);


    }
}
