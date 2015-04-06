package nars.analyze;

import nars.prototype.Default;
import nars.Events;
import nars.ProtoNAR;
import nars.Global;
import nars.event.Reaction;
import nars.io.test.TestNAR;
import nars.nal.Task;
import org.junit.Ignore;

import java.io.FileNotFoundException;

/**
 * report filtered by failures
 */
@Ignore
public class NALysisSome extends NALysis {



    public NALysisSome(ProtoNAR b) {
        super(b);
    }


    public static void main(String[] args) throws FileNotFoundException {

        Global.DEBUG = true;
        Global.DEBUG_DERIVATION_STACKTRACES = true;
        Global.TASK_HISTORY = true;
        showInput = true;
        showOutput = true;
        showTrace = true;

        //String test = "./nal/test8/nal8.1.16.nal";
        String test = "./nal/test6/nal6.15.nal";
        //String test = "./nal/test5/depr/nal5.19.nal";
        //String test = "./nal/test4/depr/nal4.recursion.small.nal";
        //String test = "./nal/test8/nal8.1.0.nal";
        //String test = "./nal/test8/nal8.1.21.nal";
        //String test = "./nal/test6/nal6.22.nal";
        //String test = "./nal/test5/nal5.18.1.nal";
        //String test = "./nal/test5/nal5.18.1.nal";
        //String test = "./nal/test7/nal7.2.nal";

        ProtoNAR build = new Default().setInternalExperience(null);


        //NewNAR build = new Solid(1, 256, 0, 9, 0, 3);
        //NewNAR build = new Default();

        TestNAR n = analyze(
                build,
                test,
                100,
                1
        );
        n.on(new Reaction() {
            @Override public void event(Class event, Object[] args) {
                Task t = (Task)args[0];
                System.out.println("Derived: " + t + " " + t.getHistory());
            }
        }, Events.TaskDerive.class);
        n.on(new Reaction() {
            @Override public void event(Class event, Object[] args) {
                Task t = (Task)args[0];
                System.out.println("Remove: " + t + " " + t.getHistory());
            }
        }, Events.TaskRemove.class);

        n.run();


        //results.printARFF(new PrintStream(dataOut));
        //results.printCSV(new PrintStream(System.out));

        /*n.concept("(&&,<robin --> swimmer>,<robin --> [flying]>)").print(System.out);
        n.concept("<robin --> swimmer>").print(System.out);
        n.concept("<robin --> [flying]>").print(System.out);*/
    }


}