/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.io;

import jdk.nashorn.internal.ir.annotations.Ignore;
import nars.core.NAR;
import nars.core.Parameters;
import nars.io.condition.OutputCondition;

import java.io.PrintStream;
import java.util.List;

/**
 * part of the original script test system, being replaced
 * @author me
 */
@Ignore
@Deprecated public class NALPerformance  {
    
    static public int similarsToSave = 6;

    double score;
    private boolean success;
    private boolean error;
    public final List<OutputCondition> expects;
    private final NAR nar;
    private final String input;

    public NALPerformance(NAR n, String input) {
        this.nar = n;
        this.input = input;
        this.expects = OutputCondition.getConditions(nar, input, similarsToSave);
    }

    public void run(int maxCycles) {
        run(-1, maxCycles);
    }

    public void run(int minCycles, int maxCycles) {
        try {
            

            
            nar.addInput( input );

            if (minCycles == -1)
                nar.run(maxCycles);
            else
                nar.run(minCycles, maxCycles);
            
        } catch (Throwable e) {
            System.err.println(e);
            if (Parameters.DEBUG) {
                e.printStackTrace();
            }
            error = true;
        }
        success = expects.size() > 0 && (!error);
        if (success) {
            for (OutputCondition e : expects) {
                if (!e.succeeded) {
                    success = false;
                    break;
                }
            }
        }
        score = OutputCondition.cost(expects);
    }

    public double getScore() {
        return score;
    }

    public boolean getSuccess() {
        return success;
    }

    public List<OutputCondition> getExpects() {
        return expects;
    }

    private long getCycleTime() {
        return nar.memory.getCycleTime();
    }

    public void printResults(PrintStream p) {
        p.println(" @" + getCycleTime());
        for (OutputCondition e : expects) {
            p.println("  " + e);
        }
    }
    
}
