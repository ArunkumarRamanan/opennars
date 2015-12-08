//package nars.process.concept;
//
//import nars.Op;
//import nars.Symbols;
//import nars.budget.Budget;
//import nars.budget.BudgetFunctions;
//import nars.link.TermLink;
//import nars.nal.nal5.Conjunction;
//import nars.nal.nal5.Disjunction;
//import nars.nal.nal5.Equivalence;
//import nars.nal.nal5.Implication;
//import nars.process.ConceptProcess;
//import nars.task.DefaultTask;
//import nars.task.Sentence;
//import nars.task.Task;
//import nars.task.TaskSeed;
//import nars.term.Compound;
//import nars.term.Term;
//import nars.term.Variables;
//import nars.truth.Truth;
//import nars.truth.TruthFunctions;
//import org.apache.commons.collections.map.Flat3Map;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//
//import static nars.term.Terms.reduceUntilLayer2;
//import static nars.term.Terms.unwrapNegation;
//import static nars.truth.TruthFunctions.*;
//
///**
// * Because of the re-use of temporary collections, each thread must have its own
// * instance of this class.
// */
//public class DeduceSecondaryVariableUnification extends ConceptFireTaskTerm {
//
//    //TODO decide if f.currentBelief needs to be checked for null like it was originally
//
//
//
//    @Override
//    public final boolean apply(final ConceptProcess f, final TermLink termLink) {
//        final Task task = f.getTaskLink().getTask();
//
//
//        if (task == null || task.isQuestOrQuestion()) {
//            return true;
//        }
//
//        final Term firstTerm = task.getTerm();
//
//        if (!firstTerm.hasVar()) {
//            return true;
//        }
//
//        //lets just allow conjunctions, implication and equivalence for now
//        if (!((firstTerm instanceof Disjunction || firstTerm instanceof Conjunction || firstTerm instanceof Equivalence || firstTerm instanceof Implication))) {
//            return true;
//        }
//
//
//        if (dedSecondLayerVariableUnification(task, f)) {
//            //unification ocurred, done reasoning in this cycle if it's judgment
//            if (task.isJudgment())
//                return false;
//        }
//        return true;
//    }
//
//    boolean dedSecondLayerVariableUnification(final Task task, final ConceptProcess nal) {
//
////        final Random r = nal.getRandom();
////
////
////        //these are intiailized further into the first cycle below. afterward, they are clear() and re-used for subsequent cycles to avoid reallocation cost
////        List<Term> terms_dependent = null;
////        List<Term> terms_independent = null;
////        Map<Term, Term> Values = null;
////        Map<Term, Term> smap = null;
////    /*Map<Term, Term> Values2 = null;
////    Map<Term, Term> Values3 = null;
////    Map<Term, Term> Values4 = null;*/
////
////        final Truth taskTruth = task.getTruth();
////
////        //final int dur = nal.duration();
////
////        boolean unifiedAnything = false;
////
////        int remainingUnifications = 1;
////
////        final Term firstTerm = task.getTerm();
////
////        for (int k = 0; k < Global.DED_SECOND_UNIFICATION_ATTEMPTS; k++) {
////            Concept secondConcept = nal.nar.memory.nextConcept(new Predicate<Concept>() {
////
////                @Override
////                public boolean test(Concept concept) {
////                    //prevent unification with itself
////                    if (concept.getTerm().equals(firstTerm)) return false;
////                    return concept.hasBeliefs();
////
////                }
////
////            }, Global.DED_SECOND_UNIFICATION_DEPTH);
////
////            if (secondConcept == null) {
////                //no more concepts, stop
////                break;
////            }
////
////            final Term secTerm = secondConcept.getTerm();
////
////            final Task secondConceptStrongestBelief = secondConcept.getBeliefs().top();
////            final Task second_belief = secondConceptStrongestBelief;
////
////            //getBeliefRandomByConfidence(task.sentence.isEternal());
////
////            final Truth truthSecond = second_belief.getTruth();
////
////            if (terms_dependent == null) {
////                terms_dependent = Global.newArrayList(0);
////                terms_independent = Global.newArrayList(0);
////
////                //TODO use one Map<Term, Term[]> instead of 4 Map<Term,Term> (values would be 4-element array)
////                Values = newVariableSubstitutionMap();
////                /*Values2 = newVariableSubstitutionMap();
////                Values3 = newVariableSubstitutionMap();
////                Values4 = newVariableSubstitutionMap();*/
////                smap = newVariableSubstitutionMap();
////            }
////
////            else {
////                //we have to select a random belief
////                terms_dependent.clear();
////                terms_independent.clear();
////            }
////            unifiedAnything = dedSecondLayerUnifyAttempt(task, nal, r, terms_dependent, terms_independent, Values, smap, taskTruth, unifiedAnything, (Compound) firstTerm, secTerm, secondConceptStrongestBelief, second_belief, truthSecond);
////
////
////            remainingUnifications--;
////
////            if (remainingUnifications == 0) {
////                break;
////            }
////
////        }
//
////        return unifiedAnything;
//        return false;
//    }
//
//    private boolean dedSecondLayerUnifyAttempt(Task task, ConceptProcess nal, Random r, List<Term> terms_dependent, List<Term> terms_independent, Map<Term, Term> values, Map<Term, Term> smap, Truth taskTruth, boolean unifiedAnything, Compound firstTerm, Term secTerm, Task secondConceptStrongestBelief, Task second_belief, Truth truthSecond) {
//        //ok, we have selected a second concept, we know the truth value of a belief of it, lets now go through taskterms term
//        //for two levels, and remember the terms which unify with second
//        Term[] components_level1 = firstTerm.term;
//        Term secterm_unwrap = unwrapNegation(secTerm);
//
//        for (final Term T1 : components_level1) {
//            final Term T1_unwrap = unwrapNegation(T1);
//
//            dedSecondLayerReduce(nal, r, terms_dependent, terms_independent, values, smap, firstTerm, secTerm, secterm_unwrap, T1_unwrap);
//
//            if ((T1_unwrap instanceof Implication) || (T1_unwrap instanceof Equivalence) || (T1_unwrap instanceof Conjunction) || (T1_unwrap instanceof Disjunction)) {
//                Term[] components_level2 = ((Compound) T1_unwrap).term;
//
//                for (final Term T2 : components_level2) {
//                    Term T2_unwrap = unwrapNegation(T2);
//
//                    dedSecondLayerReduce(nal, r, terms_dependent, terms_independent, values, smap, firstTerm, secTerm, secterm_unwrap, T2_unwrap);
//                }
//            }
//        }
//
//
//        if (task.getTruth() == null)
//            throw new RuntimeException("Task sentence truth must be non-null: " + task);
//
//
//        if (!terms_dependent.isEmpty()) {
//            dedSecondLayerVariableUnificationTerms(nal, task,
//                    second_belief, terms_dependent,
//                    anonymousAnalogy(taskTruth, truthSecond),
//                    taskTruth, truthSecond, false);
//        }
//
//        if (!terms_independent.isEmpty()) {
//            dedSecondLayerVariableUnificationTerms(nal, task,
//                    second_belief, terms_independent,
//                    deduction(taskTruth, truthSecond),
//                    taskTruth, truthSecond, true);
//        }
//
//        final boolean taskIsGoal = task.isGoal();
//        final long taskOcc = task.getOccurrenceTime();
//
//        final int termsIndependent = terms_independent.size();
//        for (int i = 0; i < termsIndependent; i++) {
//
//            Compound result = Sentence.termOrNull(terms_independent.apply(i));
//
//            if (result == null) {
//                //changed from return to continue to allow furhter processing
//                continue;
//            }
//
//            Truth truth;
//
//            char mark = Symbols.JUDGMENT;
//
//            if (taskIsGoal) {
//                truth = TruthFunctions.desireInd(taskTruth, truthSecond);
//                mark = Symbols.GOAL;
//            } else {
//                truth = deduction(taskTruth, truthSecond);
//            }
//
//
//            //REMOVED THIS WHICH APPARENTLY CAME FROM: https://github.com/opennars/opennars/commit/2faf08c41a6015bf0722b73cf769724afdcb541f
//            //because side=1 produces a constant condition in the block
//            /*
//            int order = taskSentence.getTemporalOrder();
//            int side = 1;
//            boolean eternal = true;
//            long time = Stamp.ETERNAL;
//            if ((order != ORDER_NONE) && (order!=ORDER_INVALID) && (!taskSentence.taskIsGoal()) && (!taskSentence.isQuest())) {
//                long baseTime = second_belief.getOccurrenceTime();
//                if (baseTime == Stamp.ETERNAL) {
//                    baseTime = nal.time();
//                }
//                long inc = order * dur;
//                time = (side == 0) ? baseTime+inc : baseTime-inc;
//                eternal = false;
//                //nal.getTheNewStamp().setOccurrenceTime(time);
//            }
//            */
//
//
//
//            //same as above?
//
//            long occ = taskOcc;
//            if (!second_belief.isEternal()) {
//                occ = second_belief.getOccurrenceTime();
//            }
//
//
//
//            nal.setBelief(task);
//
//            final TaskSeed seed = nal.newTask(result)
//                    .punctuation(mark)
//                    .truth(truth)
//                    .budgetCompoundForward(result, nal)
//                    .parent(task, second_belief, occ);
//
//            Task newTask = nal.derive(seed, false, false, true /* allow overlap */);
//
//            if (null!=newTask) {
//
//                //nal.emit(Events.ConceptUnification.class, newTask, firstTerm, secondConcept, second_belief);
//                nal.memory().logic.DED_SECOND_LAYER_VARIABLE_UNIFICATION.hit();
//
//                unifiedAnything = true;
//
//            }
//
//        }
//        return unifiedAnything;
//    }
//
//    private void dedSecondLayerReduce(ConceptProcess nal, Random r, List<Term> terms_dependent, List<Term> terms_independent, Map<Term, Term> values, Map<Term, Term> smap, Compound firstTerm, Term secTerm, Term secterm_unwrap, Term t1_unwrap) {
//        smap.clear();
//        values.clear(); //we are only interested in first variables
//        if (Variables.findSubstitute(Op.VAR_DEPENDENT, t1_unwrap, secterm_unwrap, values, smap, r)) {
//            dedSecondLayerReduce(nal, terms_dependent, values, firstTerm, secTerm);
//        }
//
//
//        smap.clear();
//        values.clear();
//        if (Variables.findSubstitute(Op.VAR_INDEPENDENT, t1_unwrap, secterm_unwrap, values, smap, r)) {
//            dedSecondLayerReduce(nal, terms_independent, values, firstTerm, secTerm);
//        }
//    }
//
//    private static void dedSecondLayerReduce(ConceptProcess nal, List<Term> termsPendent, Map<Term, Term> values, Compound firstTerm, Term secTerm) {
//
//
//        Compound ctaskterm_subs = firstTerm;
//        ctaskterm_subs = ctaskterm_subs.applySubstituteToCompound(values);
//        Term taskterm_subs = reduceUntilLayer2(ctaskterm_subs, secTerm, nal.memory());
//        if (taskterm_subs != null && !(Variables.indepVarUsedInvalid(taskterm_subs))) {
//            termsPendent.add(taskterm_subs);
//        }
//    }
//
//    private static void dedSecondLayerVariableUnificationTerms(final ConceptProcess nal, Task task, Task second_belief, List<Term> terms_dependent, Truth truth, Truth t1, Truth t2, boolean strong) {
//
//        final int tds = terms_dependent.size();
//
//        for (int i = 0; i < tds; i++) {
//
//
//            Term tdg = terms_dependent.apply(i);
//            if (!Sentence.invalidSentenceTerm(tdg)) continue;
//
//            final Compound result = (Compound)tdg;
//
//            truth = unifySecondLayerVariable(result, task, second_belief, truth, t1, t2, strong, nal);
//
//
//        }
//    }
//
//    private static Truth unifySecondLayerVariable(Compound result, Task task, Task second_belief, Truth truth, Truth t1, Truth t2, boolean strong, ConceptProcess nal) {
//        final char punc;
//        if (task.isGoal()) {
//            if (strong) {
//                truth = desireInd(t1, t2);
//            } else {
//                truth = desireDed(t1, t2);
//            }
//            punc = Symbols.GOAL;
//        }
//        else
//            punc = Symbols.JUDGMENT;
//
//
//        Budget budget = BudgetFunctions.compoundForward(truth, result, nal);
//
//
//        long occ = task.getOccurrenceTime();
//        if (!second_belief.isEternal()) {
//            occ = second_belief.getOccurrenceTime();
//        }
//
//
//        Task dummy = new DefaultTask(second_belief, budget, task, null);
//
//        nal.setBelief(task);
//
//        if (nal.derive(nal.newTask(result)
//                .punctuation(punc)
//                .truth(truth)
//                .budget(budget)
//                .parent(task, second_belief, occ), false, false, false)!=null) {
//
//
//            nal.memory().logic.DED_SECOND_LAYER_VARIABLE_UNIFICATION_TERMS.hit();
//
//        }
//        return truth;
//    }
//
//    /*
//    The current NAL-6 misses another way to introduce a second variable by induction:
//  IN: <<lock1 --> (/,open,$1,_)> ==> <$1 --> key>>.
//  IN: <lock1 --> lock>.
//OUT: <(&&,<#1 --> lock>,<#1 --> (/,open,$2,_)>) ==> <$2 --> key>>.
//    http://code.google.com/p/open-nars/issues/detail?id=40&can=1
//    */
//
//    private static Map<Term, Term> newVariableSubstitutionMap() {
//        //TODO give appropraite size
//        //return Global.newHashMap();
//        return new Flat3Map();
//    }
//
//}
