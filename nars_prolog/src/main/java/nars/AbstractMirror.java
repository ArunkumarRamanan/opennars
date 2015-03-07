/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars;

import nars.core.NAR;
import nars.event.AbstractReaction;
import nars.logic.reason.ImmediateProcess;
import nars.logic.entity.BudgetValue;
import nars.logic.entity.Sentence;
import nars.logic.entity.Task;

/**
 * Interface with which to implement a "mirror" - a mental prosthetic which
 * reflects NAR activity into an enhanced or accelerated representation.
 * Usually these violate NARS theory and principles as the expense of
 * improved performance.  However these can be uesd for comparing results.
 * 
 */
abstract public class AbstractMirror extends AbstractReaction {

    private final NAR nar;

    public AbstractMirror(NAR n, boolean active, Class... events) {
        super(n, active, events);
        this.nar = n;
    }

    public static enum InputMode {
        /** normal input, ie. nar.addInput */
        Perceive,

        /** bypass input buffers, directly as a new memory Task, ie. memory.addTask */
        InputTask,

        /** instance an ImmediateProcess and run it immediately */
        ImmediateProcess,

        /** insert the sentence directly into a concept, attempt to create the concept if one does not exist */
        Concept
    }
    
    public boolean input(Sentence s, InputMode mode, Task parent) {
        if (mode == InputMode.Perceive) {
            nar.input(s);
            return true;
        }
        else if ((mode == InputMode.InputTask)|| (mode == InputMode.ImmediateProcess)) {

            Task t = new Task(s, BudgetValue.newDefault(s, nar.memory), parent  );

            //System.err.println("  " + t);

            if (mode == InputMode.InputTask)
                nar.memory.taskInput(t);
            else if (mode == InputMode.ImmediateProcess)
                new ImmediateProcess(nar.memory, t).run();

            return true;

        }
        else if (mode == InputMode.Concept) {
            throw new RuntimeException("unimpl yet");
        }
        return false;
    }    
}
