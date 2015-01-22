package nars.io.condition;

import nars.core.Events;
import nars.core.NAR;
import nars.io.Output;

/**
 * Counts # of outputs
 */
public class OutputCount extends Output {

    int inputs = 0;
    int outputs = 0;
    int others = 0;

    public OutputCount(NAR n) {
        super(n);
    }

    @Override
    public void event(Class event, Object[] args) {
        if (event == Events.IN.class) inputs++;
        else if (event == Events.OUT.class) outputs++;
        else others++;
    }

    public int getInputs() {
        return inputs;
    }

    public int getOutputs() {
        return outputs;
    }

    public int getOthers() {
        return others;
    }
}
