/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.predict;

import nars.NAR;
import nars.io.narsese.InvalidInputException;
import nars.nal.entity.Concept;
import nars.nal.entity.Task;
import nars.nal.entity.Term;
import nars.nal.nal1.Inheritance;
import nars.nal.nal2.Instance;
import nars.nal.nal7.Tense;
import nars.nal.rule.DirectProcess;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author me
 */
public class Discretize {
    private final NAR nar;
    private int discretization;
    
    /** levels >=2 */
    public Discretize(NAR n, int levels) {
        this.nar = n;
        this.discretization = levels;
        
    }
    
    public int i(double v) {
        if (v <= 0) {
            return 0;
        }
        if (v >= 1f) {
            return discretization-1;
        }
        int x = (int)Math.round(-0.5 + v * (discretization));
        return x;
    }

    public void setDiscretization(int discretization) {
        this.discretization = discretization;
    }
    
    public double d(double v) {
        if (v <= 0) {
            return 0;
        }
        if (v >= 1f) {
            return discretization-1;
        }
        double x = (-0.5 + v * (discretization));
        return x;
        
    }    
    
    /** inverse of discretize */
    public double continuous(double discretized) {
        return discretized / ((double)discretization-1);
    }
    public double continuous(int discretized) {
        return continuous((double)discretized);
    }

    /** calculate proportion that value 'v' is at level 'l', or somewhere in between levels */
    public double pDiscrete(double v, int i) {        
        int center = i(v);
        if (i == center) return 1.0;
        return 0.0;
    }

    
    /** calculate proportion that value 'v' is at level 'l', or somewhere in between levels */
    public double pSmooth(double v, int l) {
        double center = d(v);
        double levelsFromCenter = Math.abs(l - center);
        double sharpness = 10.0;
        return 1.0 / (1 + levelsFromCenter/(discretization/2)*sharpness);        }
    
    /** assign 1.0 to the closest discretized level regardless */
    public double pSmoothDiscrete(double v, int l) {
        double center = d(v);
        int centerDisc = i(v);
        if (centerDisc == l) return 1.0; 
        double levelsFromCenter = Math.abs(l - center);
        double sharpness = 10.0;
        return 1.0 / (1 + levelsFromCenter/(discretization/2)*sharpness);
    }
    
    public Inheritance getValueTerm(String prefix, int level) {
        return Instance.make( Term.get(prefix), Term.get("y" + level));
    }
    
    public Term[] getValueTerms(String prefix) {
        Term t[] = new Term[discretization];
        for (int i = 0; i < discretization; i++) {
            t[i] = getValueTerm(prefix, i);
        }
        return t;
    }
    public Concept[] getValueConcepts(String prefix) {
        Concept t[] = new Concept[discretization];
        for (int i = 0; i < discretization; i++) {
            t[i] = nar.memory.concept(getValueTerm(prefix, i));
        }
        return t;
    }    
    
    public Term getValueTerm(double y) {
        return Term.get("y" + i((float)y));
    }

    /**
     * 
     * @param variable
     * @param signal
     * @param dt zero = now, + = future cycles, - = past cycles
     */
    void believe(String variable, double signal, int dt) {        
        for (int i = 0; i < discretization; i++) {
            //double p = pDiscrete(signal, i);
            double p = pSmoothDiscrete(signal, i);
            believe(variable, i, dt, (float)p, 0.95f, BeliefInsertion.MemoryInput);
        }
            
    }

    
    
    public static enum BeliefInsertion {
        Input, MemoryInput, ImmediateProcess, BeliefInsertion
    }
    
    //TODO input method: normal input, memory input, immediate process, direct belief insertion    
    void believe(String variable, int level, int dt, float freq, float conf, BeliefInsertion mode) {
                //TODO handle 'dt'

        if (mode == BeliefInsertion.Input) {
            try {

                nar.believe(getValueTerm(variable, level).toString(), Tense.Present, freq, conf);
            } catch (InvalidInputException ex) {
                Logger.getLogger(Discretize.class.getName()).log(Level.SEVERE, null, ex);
            }
            
          
        }
        else if ((mode == BeliefInsertion.MemoryInput)|| (mode == BeliefInsertion.ImmediateProcess)) {
            
            Task t = nar.memory.newTask(getValueTerm(variable, level)).judgment().truth(freq, conf).budget(1.0f, 0.8f).get();

            System.out.println(t);
            
            if (mode == BeliefInsertion.MemoryInput)
                nar.memory.taskInput(t);
            else if (mode == BeliefInsertion.ImmediateProcess)
                new DirectProcess(nar.memory, t).run();
            
        }
    }            
}