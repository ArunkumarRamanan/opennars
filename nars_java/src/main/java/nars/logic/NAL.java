/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.logic;

import nars.core.*;
import nars.logic.entity.*;
import nars.logic.nal1.Negation;
import nars.logic.nal8.Operation;
import reactor.event.Event;
import reactor.filter.Filter;
import reactor.function.Supplier;
import reactor.rx.action.FilterAction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * NAL Reasoner Process.  Includes all reasoning process state and common utility methods that utilize it.
 * <p>
 * https://code.google.com/p/open-nars/wiki/SingleStepTestingCases
 * according to derived Task: if it contains a mental operator it is NAL9, if it contains a operation it is NAL8, if it contains temporal information it is NAL7, if it contains in/dependent vars it is NAL6, if it contains higher order copulas like &&, ==> or negation it is NAL5
 * <p>
 * if it contains product or image it is NAL4, if it contains sets or set operations like &, -, | it is NAL3
 * <p>
 * if it contains similarity or instances or properties it is NAL2
 * and if it only contains inheritance
 */
public abstract class NAL extends Event implements Runnable, Supplier<Task> {

    public interface DerivationFilter extends Plugin {


        /**
         * returns null if allowed to derive, or a String containing a short rejection reason for logging
         */
        public String reject(NAL nal, Task task, boolean revised, boolean single, Task parent, Sentence otherBelief);

        @Override
        public default boolean setEnabled(NAR n, boolean enabled) {
            return true;
        }
    }

    public final Memory memory;

    protected final Task currentTask;
    protected Sentence currentBelief;

    protected Stamp newStamp;
    protected StampBuilder newStampBuilder;

    /**
     * stores the tasks that this process generates, and adds to memory
     */
    public Deque<Task> newTasks = null;



    //TODO tasksDicarded

    public NAL(Memory mem, Task task) {
        this(mem, -1, task);
    }

    /** @param nalLevel the NAL level to limit processing of this reasoning context. set to -1 to use Memory's default value */
    public NAL(Memory mem, int nalLevel, Task task) {
        super(null);


        //setKey(getClass());
        setData(this);

        memory = mem;

        if ((nalLevel!=-1) && (nalLevel!=mem.nal()))
            throw new RuntimeException("Different NAL level than Memory not supported yet");

        currentTask = task;
        currentBelief = null;

        newStamp = null;
        newStampBuilder = null;
    }


    @Override
    public void run() {
        onStart();
        reason();
        onFinished();
    }

    protected void onStart() {
        /** implement if necessary in subclasses */
    }
    protected void onFinished() {
        /** implement if necessary in subclasses */
    }

    protected abstract void reason();

    protected int newTasksCount() {
        if (newTasks == null) return 0;
        return newTasks.size();
    }

    public void emit(final Class c, final Object... o) {
        memory.emit(c, o);
    }

    public int nal() {
        return memory.nal();
    }

    /**
     * whether at least NAL level N is enabled
     */
    public boolean nal(int n) {
        return nal() >= n;
    }




    public boolean deriveTask(final Task task, final boolean revised, final boolean single, Task parent, Sentence occurence2) {
        return deriveTask(task, revised, single, parent, occurence2, getCurrentBelief(), getCurrentTask());
    }

    /**
     * iived task comes from the logic rules.
     *
     * @param task the derived task
     */
    public boolean deriveTask(final Task task, final boolean revised, final boolean single, Task parent, Sentence occurence2,
                              Sentence derivedCurrentBelief, Task derivedCurrentTask) {

        List<DerivationFilter> derivationFilters = memory.param.getDerivationFilters();

        if (derivationFilters != null) {
            for (int i = 0; i < derivationFilters.size(); i++) {
                DerivationFilter d = derivationFilters.get(i);
                String rejectionReason = d.reject(this, task, revised, single, parent, occurence2);
                if (rejectionReason != null) {
                    memory.removeTask(task, rejectionReason);
                    return false;
                }
            }
        }

        final Sentence occurence = parent != null ? parent.sentence : null;


        if (!task.budget.aboveThreshold()) {
            memory.removeTask(task, "Insufficient Budget");
            return false;
        }

        if (task.sentence != null && task.sentence.truth != null) {
            float conf = task.sentence.truth.getConfidence();
            if (conf < memory.param.confidenceThreshold.get()) {
                //no confidence - we can delete the wrongs out that way.
                memory.removeTask(task, "Insufficient confidence");
                return false;
            }
        }


        if (task.sentence.term instanceof Operation) {
            Operation op = (Operation) task.sentence.term;
            if (op.getSubject() instanceof Variable || op.getPredicate() instanceof Variable) {
                memory.removeTask(task, "Operation with variable as subject or predicate");
                return false;
            }
        }


        final Stamp stamp = task.sentence.stamp;
        if (occurence != null && !occurence.isEternal()) {
            stamp.setOccurrenceTime(occurence.getOccurenceTime());
        }
        if (occurence2 != null && !occurence2.isEternal()) {
            stamp.setOccurrenceTime(occurence2.getOccurenceTime());
        }
        if (stamp.latency > 0) {
            memory.logic.DERIVATION_LATENCY.set((double) stamp.latency);
        }

        final Term currentTaskContent = derivedCurrentTask.getTerm();
        if (derivedCurrentBelief != null && derivedCurrentBelief.isJudgment()) {
            final Term currentBeliefContent = derivedCurrentBelief.term;
            stamp.chainReplace(currentBeliefContent, currentBeliefContent);
        }
        //workaround for single premise task issue:
        if (currentBelief == null && single && currentTask != null && currentTask.sentence.isJudgment()) {
            stamp.chainReplace(currentTaskContent, currentTaskContent);
        }
        //end workaround
        if (currentTask != null && !single && currentTask.sentence.isJudgment()) {
            stamp.chainReplace(currentTaskContent,currentTaskContent);
        }
        //its a logic reason, so we have to do the derivation chain check to hamper cycles
        if (!revised) {
            Term taskTerm = task.getTerm();

            if (task.sentence.isJudgment()) {

                if (stamp.getChain().contains(taskTerm)) {
                    Term parentTaskTerm = task.getParentTask() != null ? task.getParentTask().getTerm() : null;
                    if ((parentTaskTerm == null) || (!Negation.areMutuallyInverse(taskTerm, parentTaskTerm))) {
                        memory.removeTask(task, "Cyclic Reasoning");
                        return false;
                    }
                }
            }

        }
//        else {
//            //its revision, of course its cyclic, apply evidental base policy
//            final int stampLength = stamp.baseLength;
//            for (int i = 0; i < stampLength; i++) {
//                final long baseI = stamp.evidentialBase[i];
//                for (int j = 0; j < stampLength; j++) {
//                    if ((i != j) && (baseI == stamp.evidentialBase[j])) {
//                        throw new RuntimeException("Overlapping Revision Evidence: Should have been discovered earlier: " + Arrays.toString(stamp.evidentialBase));
//
//                        //memory.removeTask(task, "Overlapping Revision Evidence");
//                        //"(i=" + i + ",j=" + j +')' /* + " in " + stamp.toString()*/
//                        //return false;
//                    }
//                }
//            }
//        }

        if (task.sentence.getOccurenceTime() > memory.time()) {
            memory.event.emit(Events.TaskDeriveFuture.class, task, this);
        }

        task.setParticipateInTemporalInduction(false);

        memory.event.emit(Events.TaskDerive.class, task, revised, single, occurence, occurence2, derivedCurrentTask);
        memory.logic.TASK_DERIVED.hit();

        addNewTask(task, "Derived");

        return true;
    }

    /* --------------- new task building --------------- */

    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param newTaskContent The content of the sentence in task
     * @param newTruth       The truth value of the sentence in task
     * @param newBudget      The budget value in task
     */
    @Deprecated
    public Task doublePremiseTask(final Term newTaskContent, final TruthValue newTruth, final BudgetValue newBudget, boolean temporalAdd) {
        CompoundTerm newContent = Sentence.termOrNull(newTaskContent);
        if (newContent == null)
            return null;
        return doublePremiseTask(newContent, newTruth, newBudget, temporalAdd);
    }

    public Task doublePremiseTask(CompoundTerm newTaskContent, final TruthValue newTruth, final BudgetValue newBudget, boolean temporalAdd) {
        return doublePremiseTask(newTaskContent, newTruth, newBudget, temporalAdd, getCurrentBelief(), getCurrentTask());
    }

    public Task doublePremiseTask(CompoundTerm newTaskContent, final TruthValue newTruth, final BudgetValue newBudget, boolean temporalAdd, Sentence subbedBelief, Task subbedTask) {
        if (!newBudget.aboveThreshold()) {
            return null;
        }

        newTaskContent = Sentence.termOrNull(newTaskContent);
        if (newTaskContent == null)
            return null;

        Task derived = null;


        final Sentence newSentence;

        try {
            newSentence = new Sentence(newTaskContent, subbedTask.sentence.punctuation, newTruth, getTheNewStamp());
        }
        catch (CompoundTerm.UnableToCloneException e) {
            System.err.println(e.toString());
            return null;
        }

        final Task newTask = Task.make(newSentence, newBudget, subbedTask, subbedBelief);

        if (newTask != null) {
            boolean added = deriveTask(newTask, false, false, null, null);
            if (added)
                derived = newTask;
        }

        temporalAdd = temporalAdd && nal(7);

        //"Since in principle it is always valid to eternalize a tensed belief"
        if (temporalAdd && Parameters.IMMEDIATE_ETERNALIZATION) { //temporal induction generated ones get eternalized directly

            TruthValue truthEt = TruthFunctions.eternalize(newTruth);
            Stamp st = getTheNewStamp().clone();
            st.setEternal();
            final Sentence newSentence2 = new Sentence(newTaskContent, subbedTask.sentence.punctuation, truthEt, st);
            final Task newTask2 = Task.make(newSentence2, newBudget, subbedTask, subbedBelief);
            if (newTask2 != null) {
                deriveTask(newTask2, false, false, null, null);
            }
        }

        return derived;
    }

    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth The truth value of the sentence in task
     * @param newBudget The budget value in task
     * @param revisible Whether the sentence is revisible
     */
    //    public void doublePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget, boolean revisible) {
    //        if (newContent != null) {
    //            Sentence taskSentence = currentTask.getSentence();
    //            Sentence newSentence = new Sentence(newContent, taskSentence.getPunctuation(), newTruth, newStamp, revisible);
    //            Task newTaskAt = new Task(newSentence, newBudget, currentTask, currentBelief);
    //            derivedTask(newTaskAt, false, false);
    //        }
    //    }

    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     */
    public boolean singlePremiseTask(CompoundTerm newContent, TruthValue newTruth, BudgetValue newBudget) {
        return singlePremiseTask(newContent, getCurrentTask().sentence.punctuation, newTruth, newBudget);
    }

    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param newContent  The content of the sentence in task
     * @param punctuation The punctuation of the sentence in task
     * @param newTruth    The truth value of the sentence in task
     * @param newBudget   The budget value in task
     */
    public boolean singlePremiseTask(final CompoundTerm newContent, final char punctuation, final TruthValue newTruth, final BudgetValue newBudget) {

        if (!newBudget.aboveThreshold())
            return false;

        Task parentTask = getCurrentTask().getParentTask();
        if (parentTask != null) {
            if (parentTask.getTerm() == null) {
                return false;
            }
            if (newContent == null) {
                return false;
            }
            if (newContent.equals(parentTask.getTerm())) {
                return false;
            }
        }
        Sentence taskSentence = getCurrentTask().sentence;
        if (taskSentence.isJudgment() || getCurrentBelief() == null) {
            setNextNewStamp(new Stamp(taskSentence.stamp, time()));
        } else {
            // to answer a question with negation in NAL-5 --- move to activated task?
            setNextNewStamp(new Stamp(getCurrentBelief().stamp, time()));
        }

        if (newContent.subjectOrPredicateIsIndependentVar()) {
            return false;
        }

        Sentence newSentence = new Sentence(newContent, punctuation, newTruth, getTheNewStamp());
        Task newTask = Task.make(newSentence, newBudget, getCurrentTask());
        if (newTask != null) {
            return deriveTask(newTask, false, true, null, null);
        }
        return false;
    }

    public boolean singlePremiseTask(Sentence newSentence, BudgetValue newBudget) {
        /*if (!newBudget.aboveThreshold()) {
            return false;
        }*/
        Task newTask = new Task(newSentence, newBudget, getCurrentTask());
        return deriveTask(newTask, false, true, null, null);
    }


    public long time() {
        return memory.time();
    }



    /**
     * @return the currentTask
     */
    public Task getCurrentTask() {
        return currentTask;
    }





    /**
     * @return the newStamp
     */
    public Stamp getTheNewStamp() {
        if (newStamp == null) {
            //if newStamp==null then newStampBuilder must be available. cache it's return value as newStamp
            newStamp = newStampBuilder.build();
            newStampBuilder = null;
        }
        return newStamp;
    }
    public Stamp getTheNewStampForRevision() {
        if (newStamp == null) {
            if (newStampBuilder.overlapping()) {
                newStamp = null;
            }
            else {
                newStamp = newStampBuilder.build();
            }
            newStampBuilder = null;
        }
        return newStamp;
    }

    /**
     * @param newStamp the newStamp to set
     */
    public Stamp setNextNewStamp(Stamp newStamp) {
        this.newStamp = newStamp;
        this.newStampBuilder = null;
        return newStamp;
    }

    interface StampBuilder {
        Stamp build();
        public Stamp getFirst();
        public Stamp getSecond();

        default public boolean overlapping() {
            /*final int stampLength = stamp.baseLength;
            for (int i = 0; i < stampLength; i++) {
                final long baseI = stamp.evidentialBase[i];
                for (int j = 0; j < stampLength; j++) {
                    if ((i != j) && (baseI == stamp.evidentialBase[j])) {
                        throw new RuntimeException("Overlapping Revision Evidence: Should have been discovered earlier: " + Arrays.toString(stamp.evidentialBase));
                    }
                }
            }*/

            long[] a = getFirst().toSet();
            long[] b = getSecond().toSet();
            for (long ae : a) {
                for (long be : b) {
                    if (ae == be) return true;
                }
            }
            return false;
        }
    }

    /**
     * creates a lazy/deferred StampBuilder which only constructs the stamp if getTheNewStamp() is actually invoked
     */
    public void setNextNewStamp(final Stamp first, final Stamp second, final long time) {
        newStamp = null;
        newStampBuilder = new NewStampBuilder(first, second, time);
    }

    /**
     * @return the currentBelief
     */
    public Sentence getCurrentBelief() {
        return currentBelief;
    }




    /**
     * tasks added with this method will be buffered by this NAL instance;
     * at the end of the processing they can be reviewed and filtered
     * then they need to be added to memory with inputTask(t)
     */
    protected void addNewTask(Task t, String reason) {
        t.setReason(reason);

        if (newTasks==null)
            newTasks = new ArrayDeque(4);

        newTasks.add(t);
    }


    /** called from consumers of the tasks that this context generates */
    @Override public Task get() {
        if (newTasks == null || newTasks.isEmpty())
            return null;
        return newTasks.removeFirst();
    }

    /**
     * Activated task called in MatchingRules.trySolution and
     * Concept.processGoal
     *
     * @param budget          The budget value of the new Task
     * @param sentence        The content of the new Task
     * @param candidateBelief The belief to be used in future logic, for
     *                        forward/backward correspondence
     */
    public void addSolution(final Task currentTask, final BudgetValue budget, final Sentence sentence, final Sentence candidateBelief) {
        addNewTask(new Task(sentence, budget, currentTask, sentence, candidateBelief),
                "Activated");
    }


    /**
     * for lazily constructing a stamp, in case it will not actually be necessary to completely construct a stamp
     */
    private static class NewStampBuilder implements StampBuilder {
        private final Stamp first;
        private final Stamp second;
        private final long time;

        public NewStampBuilder(Stamp first, Stamp second, long time) {
            this.first = first;
            this.second = second;
            this.time = time;
        }

        @Override public Stamp getFirst() { return first; }
        @Override public Stamp getSecond() { return second; }

        @Override
        public Stamp build() {
            return new Stamp(first, second, time);
        }
    }
}
