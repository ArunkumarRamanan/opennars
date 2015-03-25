package nars.operate.io;


import nars.Memory;
import nars.nal.entity.Task;
import nars.nal.entity.Term;
import nars.nal.nal8.Operation;
import nars.nal.nal8.Operator;

import java.util.List;

/** sets the memory's current SELF term; warning: can cause mental disturbance */
public class Schizo extends Operator {

    public Schizo() {
        super("^schizo");
    }

    @Override
    protected List<Task> execute(Operation operation, Term[] args, Memory memory) {
        memory.setSelf(args[0]);
        return null;
    }

    @Override
    public boolean isImmediate() {
        return true;
    }
}
