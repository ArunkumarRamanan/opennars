/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */

package nars.operator.app.farg;

import nars.core.Events.CycleEnd;
import nars.core.NAR;
import nars.event.Reaction;
import nars.logic.entity.Concept;
import nars.util.bag.impl.LevelBag;

/**
 *
 * @author patrick.hammer
 */
public class Workspace {

    public double temperature=0.0;
    public NAR nar;
    public int n_concepts=0;
    
    public Workspace(FluidAnalogiesAgents farg, NAR nar) {
        this.nar=nar;
        Workspace ws=this;
        farg.coderack=new LevelBag(farg.codelet_level,farg.max_codelets);
        nar.on(CycleEnd.class, new Reaction() {

            @Override
            public void event(Class event, Object[] args) {
                for(int i=0;i<10;i++) { //process 10 codelets in each step
                    Codelet cod=farg.coderack.pop();
                    if(cod!=null) {
                        if(cod.run(ws)) {
                            farg.coderack.put(cod);
                        }
                    }
                    temperature=calc_temperature();
                }
                controller();
            }
        });
    }
    
    public void controller() { 
        //when to put in Codelets of different type, and when to remove them
        //different controller for different domains would inherit from FARG
    }
    
    public double calc_temperature() {
        double s=0.0f;
        n_concepts=0;
        for(Concept node : nar.memory.concepts) {
            if(!node.goals.isEmpty()) {
                s+=node.getPriority()*node.goals.get(0).truth.getExpectation();
            }
            n_concepts++;
        }
        return s/((double) n_concepts);
    }
}