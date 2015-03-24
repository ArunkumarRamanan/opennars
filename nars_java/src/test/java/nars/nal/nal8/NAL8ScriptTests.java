package nars.nal.nal8;


import nars.prototype.Default;
import nars.ProtoNAR;
import nars.nal.ScriptNALTest;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static nars.io.ExampleFileInput.getParams;

public class NAL8ScriptTests extends ScriptNALTest {

    public NAL8ScriptTests(ProtoNAR b, String input) {
        super(b, input);
    }

    @Parameterized.Parameters(name= "{1} {0}")
    public static Collection configurations() {
        return getParams(new String[]{"test8"},
                new Default(),
                new Default().setInternalExperience(null));
    }

    public int getMaxCycles() { return 1500; }


}

