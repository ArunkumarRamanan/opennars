/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.nal.rule;

import nars.Events;
import nars.nal.NAL;
import nars.nal.entity.*;

/** Firing a concept (reasoning event). Derives new Tasks via reasoning rules
 *
 *  Concept
 *     Task
 *     TermLinks
 *
 * */
public class ConceptProcess extends NAL {

    protected final TaskLink currentTaskLink;
    protected final Concept currentConcept;

    protected TermLink currentTermLink;
    private Concept currentBeliefConcept;
    private int termLinksToFire;
    private int termlinkMatches;


    public ConceptProcess(Concept concept, TaskLink taskLink) {
        this(concept, taskLink, concept.memory.param.termLinkMaxReasoned.get());
    }

    public ConceptProcess(Concept concept, TaskLink taskLink, int termLinkCount) {
        super(concept.memory, taskLink.getTask());
        this.currentTaskLink = taskLink;
        this.currentConcept = concept;

        this.termLinksToFire = termLinkCount;
    }



    /**
     * @return the currentConcept
     */
    public Concept getCurrentConcept() {
        return currentConcept;
    }


    protected void beforeFinish() {

    }

    @Override
    protected void onFinished() {
        beforeFinish();


        emit(Events.ConceptFired.class, this);
        memory.logic.TASKLINK_FIRE.hit();


        if (newTasks!=null && !newTasks.isEmpty()) {
            memory.taskAdd(newTasks);
        }
    }


    protected void processTask() {

        currentTaskLink.budget.setUsed(memory.time());

        setCurrentTermLink(null);
        reasoner.fire(this);
    }

    protected void processTerms() {

        final int noveltyHorizon = memory.param.noveltyHorizon.get();

        int termLinkSelectionAttempts = termLinksToFire;

        //TODO early termination condition of this loop when (# of termlinks) - (# of non-novel) <= 0
        int numTermLinks = getCurrentConcept().termLinks.size();

        currentConcept.updateTermLinks();

        int termLinksSelected = 0;
        while (termLinkSelectionAttempts-- > 0) {

            int numAddedTasksBefore = newTasksCount();

            final TermLink bLink = nextTermLink(currentTaskLink, memory.time(), noveltyHorizon);
            if (bLink != null) {
                //novel termlink available

                processTerm(bLink);

                termLinksSelected++;

                int numAddedTasksAfter = newTasksCount();

                emit(Events.TermLinkSelected.class, bLink, this, numAddedTasksBefore, numAddedTasksAfter);
                memory.logic.TERM_LINK_SELECT.hit();
            }

        }
        /*
        System.out.println(termLinksSelected + "/" + termLinksToFire + " took " +  termlinkMatches + " matches over " + numTermLinks + " termlinks" + " " + currentTaskLink.getRecords());
        currentConcept.taskLinks.printAll(System.out);*/
    }

    final Concept.TermLinkNovel termLinkNovel = new Concept.TermLinkNovel();

    /**
     * Replace default to prevent repeated logic, by checking TaskLink
     *
     * @param taskLink The selected TaskLink
     * @param time The current time
     * @return The selected TermLink
     */
    TermLink nextTermLink(final TaskLink taskLink, final long time, int noveltyHorizon) {

        int toMatch = memory.param.termLinkMaxMatched.get();

        //optimization case: if there is only one termlink, we will never get anything different from calling repeatedly
        if (currentConcept.termLinks.size() == 1) toMatch = 1;

        termLinkNovel.set(taskLink, time, noveltyHorizon, memory.param.termLinkRecordLength.get());

        for (int i = 0; (i < toMatch); i++) {

            final TermLink termLink = currentConcept.termLinks.forgetNext(memory.param.termLinkForgetDurations, memory);
            termlinkMatches++;

            if (termLink == null)
                return null;

            if (termLinkNovel.apply(termLink)) {
                return termLink;
            }

        }

        return null;
    }


    /**
     * Entry point of the logic engine
     *
     * @param tLink The selected TaskLink, which will provide a task
     * @param bLink The selected TermLink, which may provide a belief
     */
    protected void processTerm(TermLink bLink) {
        setCurrentTermLink(bLink);

        Sentence belief = getCurrentBelief();

        reasoner.fire(this);

        if (belief!=null) {
            emit(Events.BeliefReason.class, belief, getCurrentTaskLink().getTarget(), this);
        }
    }

    @Override
    protected void process() {

        currentConcept.budget.setUsed(memory.time());

        processTask();

        if (currentTaskLink.type != TermLink.TRANSFORM) {

            processTerms();

        }
                

    }





    /**
     * @return the currentBeliefLink
     */
    public TermLink getCurrentTermLink() {
        return currentTermLink;
    }

    /**
     * @param currentTermLink the currentBeliefLink to set
     */
    public void setCurrentTermLink(TermLink currentTermLink) {

        if (currentTermLink == null) {
            this.currentBelief = null;
            this.currentTermLink = null;
            this.currentBeliefConcept = null;

        }
        else {

            this.currentTermLink = currentTermLink;
            currentTermLink.budget.setUsed(memory.time());

            Term beliefTerm = currentTermLink.getTerm();

            this.currentBeliefConcept = memory.concept(beliefTerm);
            if (this.currentBeliefConcept!=null) {
                this.currentBelief = currentBeliefConcept.getBelief(this, getCurrentTask());
            }
            else {
                this.currentBelief = null;
            }
        }
    }

    /**
     * @return the currentTerm
     */
    public Term getCurrentTerm() {
        return currentConcept.getTerm();
    }

    /**
     * @return the currentTaskLink
     */
    public TaskLink getCurrentTaskLink() {
        return currentTaskLink;
    }





    /** the current belief concept */
    public Concept getCurrentBeliefConcept() {
        return currentBeliefConcept;
    }



    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append('[').append(currentConcept).append(':').append(currentTaskLink).append(',').append(currentTermLink).append(']');
        return sb.toString();
    }


}
