package nars.util.graph;

import nars.core.NAR;
import nars.logic.MemoryObserver;
import nars.logic.entity.Concept;
import nars.logic.entity.Task;
import nars.util.data.CuckooMap;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;


public class TaskGraph  {

    private final MemoryObserver reaction;
    private boolean started;


    final Deque<Task> log = new ArrayDeque();
    public final Map<Task, Float> y = new CuckooMap<>();

    int maxItems = 32;
    private float earliestCreationTime = -1;

    public TaskGraph(NAR n) {

        reaction = new MemoryObserver(n.memory, false) {

            @Override
            public void output(Class channel, Object... args) {

            }

            @Override
            public void onConceptAdd(Concept concept) {

            }

            @Override
            public void onCycleStart(long clock) {

            }

            @Override
            public void onCycleEnd(long clock) {

            }

            @Override
            public void onTaskAdd(Task task) {
                next(task);
            }

            @Override
            public void onTaskRemove(Task task, String reason) {

            }
        };

        start();
    }


    public void next(Task o) {

        if (!log.isEmpty())
            if (log.getLast().equals(o))
                return; //duplicate at the same time

        while (1 + log.size() >= maxItems) {
            log.removeFirst();
        }

        log.addLast(o);


        //TODO check all elements, in case they are out of order
        earliestCreationTime = log.getFirst().getCreationTime();
    }

    public float getEarliestCreationTime() {
        return earliestCreationTime;
    }

    public static class TaskSequenceEdge extends DefaultEdge {

        private final Task from;
        private final Task to;

        public TaskSequenceEdge(Task from, Task to) {
            super();
            this.from = from;
            this.to = to;
        }

        @Override
        protected Object getSource() {
            return from;
        }

        @Override
        protected Object getTarget() {
            return to;
        }
    }

    public NARGraph get() {
        NARGraph g = new NARGraph();
        Task previous = null;

        y.clear();
        float cy = 0;

        //iterate in reverse to display the newest copy of a task and not an older one
        Iterator<Task> ii = log.descendingIterator();
        while (ii.hasNext()) {

            Task o = ii.next();

            if (y.containsKey(o))
                continue;

            g.addVertex(o);
            y.put(o, cy);

            if ((previous!=null) && (!previous.equals(o))) {
                g.addEdge(previous, o, new TaskSequenceEdge(previous, o));
            }

            previous = o;

            cy++;
        }

        return g;
    }

    public void start() {
        if (started) return;
        reaction.setActive(true);
        started = true;
    }
    public void stop() {
        if (!started) return;
        started = false;
        reaction.setActive(false);
    }

}
