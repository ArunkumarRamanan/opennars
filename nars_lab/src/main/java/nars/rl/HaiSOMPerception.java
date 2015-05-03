package nars.rl;

import jurls.reinforcementlearning.domains.RLEnvironment;
import nars.NAR;
import nars.nal.Task;
import nars.nal.nal1.Inheritance;
import nars.nal.term.Term;
import nars.rl.hai.Hsom;

import java.util.Collections;

/** TODO inputs the perceived data in a raw numerically discretized form for each dimension */

public class HaiSOMPerception implements Perception {

    private final String id;
    private final float confidence;
    private int somSize;
    private Hsom som = null;
    private QLAgent agent;
    private RLEnvironment env;

    public HaiSOMPerception(String id, int somSize, float confidence) {
        this.id = id;
        this.somSize = somSize;
        this.confidence = confidence;
    }

    @Override
    public void init(RLEnvironment env, QLAgent agent) {
        this.env = env;

        this.agent = agent;

        if (somSize == -1) somSize = env.numStates()+1;
        som = new Hsom(somSize, env.numStates());
    }


    @Override
    public Iterable<Task> perceive(NAR nar, double[] input, double t) {

        som.learn(input);
        //int s = som.winnerx * env.inputDimension() + som.winnery;
        //System.out.println(Arrays.toString(input) + " " + reward );
        //System.out.println(som.winnerx + " " + som.winnery + " -> " + s);

        //System.out.println(Arrays.deepToString(q));
        // agent.learn(s, reward);

        int x = som.winnerx;
        int y = som.winnery;

        return Collections.singleton(
                //TODO avoid String parsing
                nar.task("<state --> [(*," + id + x + "," + id + y + ")]>. :|: %1.00;" + confidence + '%')
        );
    }

    @Override
    public boolean isState(Term t) {
        //TODO better pattern recognizer
        String s = t.toString();
        if ((t instanceof Inheritance) /*&& (t.getComplexity() == 6)*/) {
            if (s.startsWith("<state --> [(*," + id) && s.endsWith(")]>")) {
                //System.out.println(t + " " + t.getComplexity());
                return true;
            }
        }
        return false;
    }
}
