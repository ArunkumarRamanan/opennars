/*
 * StructuralRules.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the abduction warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.logic;

import nars.core.Memory;
import nars.logic.entity.*;
import nars.logic.nal1.Inheritance;
import nars.logic.nal2.Similarity;
import nars.logic.nal3.*;
import nars.logic.nal4.ImageExt;
import nars.logic.nal4.ImageInt;
import nars.logic.nal4.Product;
import nars.logic.nal5.Conjunction;
import nars.logic.nal5.Disjunction;
import nars.logic.nal7.TemporalRules;

import java.util.List;

/**
 * Single-premise logic rules involving compound terms. Input are one
 * sentence (the premise) and one TermLink (indicating a component)
 */
public final class StructuralRules {

   

    /* -------------------- transform between compounds and term -------------------- */
    /**
     * {<S --> P>, S@(S&T)} |- <(S&T) --> (P&T)> {<S --> P>, S@(M-S)} |- <(M-P)
     * --> (M-S)>
     *
     * @param compound The compound term
     * @param index The location of the indicated term in the compound
     * @param statement The premise
     * @param side The location of the indicated term in the premise
     * @param nal Reference to the memory
     */
    static void structuralCompose2(CompoundTerm compound, short index, Statement statement, short side, NAL nal) {
        //final Memory mem = nal.memory;
        
        if (compound.equals(statement.term[side])) {
            return;
        }
        /*if (!memory.getCurrentTask().sentence.isJudgment() || (compound.size() == 1)) {
            return; // forward logic only
        }*/
        Term sub = statement.getSubject();
        Term pred = statement.getPredicate();
        List<Term> components = compound.asTermList();
        if (((side == 0) && components.contains(pred)) || ((side == 1) && components.contains(sub))) {
            return;
        }
        if (side == 0) {
            if (components.contains(sub)) {
                sub = compound;
                components.set(index, pred);
                pred = Memory.term(compound, components);
            }
        } else {
            if (components.contains(pred)) {
                components.set(index, sub);
                sub = Memory.term(compound, components);
                pred = compound;
            }
        }
        
        if ((sub == null) || (pred == null))
            return;
        
        Statement content;
        int order = statement.getTemporalOrder();
        if (switchOrder(compound, index)) {
            content = Statement.make(statement, pred, sub, TemporalRules.reverseOrder(order));
        } else {
            content = Statement.make(statement, sub, pred, order);
        }
        
        if (content == null)
            return;
        
        Sentence sentence = nal.getCurrentTask().sentence;
        TruthValue truth = TruthFunctions.deduction(sentence.truth, nal.memory.param.reliance.floatValue());
        BudgetValue budget = BudgetFunctions.compoundForward(truth, content, nal);
        nal.singlePremiseTask(content, truth, budget);
    }

    /**
     * {<(S*T) --> (P*T)>, S@(S*T)} |- <S --> P>
     *
     * @param statement The premise
     * @param nal Reference to the memory
     */
    static void structuralDecompose2(Statement statement, int index, NAL nal) {
        Term subj = statement.getSubject();
        Term pred = statement.getPredicate();
        if (subj.operator() != pred.operator()) {
            return;
        }
        
        if (!(subj instanceof Product) && !(subj instanceof SetExt) && !(subj instanceof SetInt)) {
            return; // no abduction on other compounds for now, but may change in the future
        }
        
        CompoundTerm sub = (CompoundTerm) subj;
        CompoundTerm pre = (CompoundTerm) pred;
        if (sub.size() != pre.size() || sub.size() <= index) {
            return;
        }
        
        Term t1 = sub.term[index];
        Term t2 = pre.term[index];
        Statement content;
        int order = statement.getTemporalOrder();
        if (switchOrder(sub, (short) index)) {
            content = Statement.make(statement, t2, t1, TemporalRules.reverseOrder(order));
        } else {
            content = Statement.make(statement, t1, t2, order);
        }
        if (content == null) {
            return;
        }
        Task task = nal.getCurrentTask();
        Sentence sentence = task.sentence;
        TruthValue truth = sentence.truth;
        BudgetValue budget;
        if (sentence.isQuestion() || sentence.isQuest()) {
            budget = BudgetFunctions.compoundBackward(content, nal);
        } else {
            budget = BudgetFunctions.compoundForward(truth, content, nal);
        }
        nal.singlePremiseTask(content, truth, budget);
    }

    /**
     * List the cases where the direction of inheritance is revised in
     * conclusion
     *
     * @param compound The compound term
     * @param index The location of focus in the compound
     * @return Whether the direction of inheritance should be revised
     */
    private static boolean switchOrder(CompoundTerm compound, short index) {
        //TODO use Image base class for both comparisons

        return ((((compound instanceof DifferenceExt) || (compound instanceof DifferenceInt)) && (index == 1))
                || ((compound instanceof ImageExt) && (index != ((ImageExt) compound).relationIndex))
                || ((compound instanceof ImageInt) && (index != ((ImageInt) compound).relationIndex)));
    }

    /**
     * {<S --> P>, P@(P|Q)} |- <S --> (P|Q)>
     *
     * @param compound The compound term
     * @param index The location of the indicated term in the compound
     * @param statement The premise
     * @param nal Reference to the memory
     */
    static void structuralCompose1(CompoundTerm compound, short index, Statement statement, NAL nal) {
        if (!nal.getCurrentTask().sentence.isJudgment()) {
            return;     // forward logic only
        }
        Term component = compound.term[index];
        Task task = nal.getCurrentTask();
        Sentence sentence = task.sentence;
        int order = sentence.getTemporalOrder();
        TruthValue truth = sentence.truth;
        
        final float reliance = nal.memory.param.reliance.floatValue();
        TruthValue truthDed = TruthFunctions.deduction(truth, reliance);
        TruthValue truthNDed = TruthFunctions.negation(TruthFunctions.deduction(truth, reliance));
        
        Term subj = statement.getSubject();
        Term pred = statement.getPredicate();
        
        if (component.equals(subj)) {
            if (compound instanceof IntersectionExt) {
                structuralStatement(compound, pred, order, truthDed, nal);
            } else if (compound instanceof IntersectionInt) {
            } else if ((compound instanceof DifferenceExt) && (index == 0)) {
                structuralStatement(compound, pred, order, truthDed, nal);
            } else if (compound instanceof DifferenceInt) {
                if (index == 0) {
                } else {
                    structuralStatement(compound, pred, order, truthNDed, nal);
                }
            }
        } else if (component.equals(pred)) {
            if (compound instanceof IntersectionExt) {
            } else if (compound instanceof IntersectionInt) {
                structuralStatement(subj, compound, order, truthDed, nal);
            } else if (compound instanceof DifferenceExt) {
                if (index == 0) {
                } else {
                    structuralStatement(subj, compound, order, truthNDed, nal);
                }
            } else if ((compound instanceof DifferenceInt) && (index == 0)) {
                structuralStatement(subj, compound, order, truthDed, nal);
            }
        }
    }

    /**
     * {<(S|T) --> P>, S@(S|T)} |- <S --> P> {<S --> (P&T)>, P@(P&T)} |- <S --> P>
     *
     * @param compound The compound term
     * @param index The location of the indicated term in the compound
     * @param statement The premise
     * @param nal Reference to the memory
     */
    static void structuralDecompose1(CompoundTerm compound, short index, Statement statement, NAL nal) {
//        if (!memory.getCurrentTask().sentence.isJudgment()) {
//            return;
//        }

        Term component = compound.term[index];
        Task task = nal.getCurrentTask();
        Sentence sentence = task.sentence;
        int order = sentence.getTemporalOrder();
        TruthValue truth = sentence.truth;
        
        if (truth == null) {
            return;
        }
        
        final float reliance = nal.memory.param.reliance.floatValue();
        TruthValue truthDed = TruthFunctions.deduction(truth, reliance);
        TruthValue truthNDed = TruthFunctions.negation(TruthFunctions.deduction(truth, reliance));
        
        Term subj = statement.getSubject();
        Term pred = statement.getPredicate();
        if (compound.equals(subj)) {
            if (compound instanceof IntersectionInt) {
                structuralStatement(component, pred, order, truthDed, nal);
            } else if ((compound instanceof SetExt) && (compound.size() > 1)) {
                Term[] t1 = new Term[]{component};
                structuralStatement(new SetExt(t1), pred, order, truthDed, nal);
            } else if (compound instanceof DifferenceInt) {
                if (index == 0) {
                    structuralStatement(component, pred, order, truthDed, nal);
                } else {
                    structuralStatement(component, pred, order, truthNDed, nal);
                }
            }
        } else if (compound.equals(pred)) {
            if (compound instanceof IntersectionExt) {
                structuralStatement(subj, component, order, truthDed, nal);
            } else if ((compound instanceof SetInt) && (compound.size() > 1)) {
                structuralStatement(subj, new SetInt(component), order, truthDed, nal);
            } else if (compound instanceof DifferenceExt) {
                if (index == 0) {
                    structuralStatement(subj, component, order, truthDed, nal);
                } else {
                    structuralStatement(subj, component, order, truthNDed, nal);
                }
            }
        }
    }

    /**
     * Common final operations of the above two methods
     *
     * @param subject The subject of the new task
     * @param predicate The predicate of the new task
     * @param truth The truth value of the new task
     * @param nal Reference to the memory
     */
    private static void structuralStatement(Term subject, Term predicate, int order, TruthValue truth, NAL nal) {
        Task task = nal.getCurrentTask();
        Term oldContent = task.getTerm();
        if (oldContent instanceof Statement) {
            Statement content = Statement.make((Statement) oldContent, subject, predicate, order);
            if (content != null) {
                BudgetValue budget = BudgetFunctions.compoundForward(truth, content, nal);
                nal.singlePremiseTask(content, truth, budget);
            }
        }
    }

    /* -------------------- set transform -------------------- */
    /**
     * {<S --> {P}>} |- <S <-> {P}>
     *
     * @param compound The set compound
     * @param statement The premise
     * @param side The location of the indicated term in the premise
     * @param nal Reference to the memory
     */
    static void transformSetRelation(CompoundTerm compound, Statement statement, short side, NAL nal) {
        if (compound.size() > 1) {
            return;
        }
        if (statement instanceof Inheritance) {
            if (((compound instanceof SetExt) && (side == 0)) || ((compound instanceof SetInt) && (side == 1))) {
                return;
            }
        }
        Term sub = statement.getSubject();
        Term pre = statement.getPredicate();
        Statement content;
        if (statement instanceof Inheritance) {
            content = Similarity.make(sub, pre);
        } else {
            if (((compound instanceof SetExt) && (side == 0)) || ((compound instanceof SetInt) && (side == 1))) {
                content = Inheritance.make(pre, sub);
            } else {
                content = Inheritance.make(sub, pre);
            }
        }
        if (content == null) {
            return;
        }

        Task task = nal.getCurrentTask();
        Sentence sentence = task.sentence;
        TruthValue truth = sentence.truth;
        BudgetValue budget;
        if (sentence.isJudgment()) {
            budget = BudgetFunctions.compoundForward(truth, content, nal);
        } else {
            budget = BudgetFunctions.compoundBackward(content, nal);
        }
        nal.singlePremiseTask(content, truth, budget);
    }



    /* --------------- Disjunction and Conjunction transform --------------- */
    /**
     * {(&&, A, B), A@(&&, A, B)} |- A, or answer (&&, A, B)? using A {(||, A,
     * B), A@(||, A, B)} |- A, or answer (||, A, B)? using A
     *
     * @param compound The premise
     * @param component The recognized component in the premise
     * @param compoundTask Whether the compound comes from the task
     * @param nal Reference to the memory
     */
    static boolean structuralCompound(CompoundTerm compound, Term component, boolean compoundTask, int index, NAL nal) {
        if (component.hasVar()) {
            return false;
        }
        
        if ((compound instanceof Conjunction) && (compound.getTemporalOrder() == TemporalRules.ORDER_FORWARD) && (index != 0)) {
            return false;
        }        
        
        final Term content = compoundTask ? component : compound;
        
        
        Task task = nal.getCurrentTask();

        Sentence sentence = task.sentence;
        TruthValue truth = sentence.truth;

        final float reliance = nal.memory.param.reliance.floatValue();

        BudgetValue budget;
        if (sentence.isQuestion() || sentence.isQuest()) {
            budget = BudgetFunctions.compoundBackward(content, nal);
        } else /* if (sentence.isJudgment() || sentence.isGoal()) */ {

            if ((!compoundTask && compound instanceof Disjunction) ||
                            (compoundTask && compound instanceof Conjunction)) {
                /*
                <a --> b>.     (||,<a --> b>,<x --> y>)?
                        compound-task=false, but since its a disjunction it should be answered
                <a --> b>.     (||,<a --> b>,<x --> y>).
                        compound-task=true, and since its a disjunction, it is not valid to derive <x --> y> since its not know if both or just <a --> b> is true
                <a --> b>.     (&&,<a --> b>,<x --> y>)?
                        compound-task=false, but since its a conjunction it can not be answered as long as <x --> y> is not known to be true
                <a --> b>.     (&&,<a --> b>,<x --> y>).
                       compound-task=true, and since its a conjunction, it is valid to derive <x --> y>
                */

                truth = TruthFunctions.deduction(truth, reliance);
            } else {
                TruthValue v1, v2;
                v1 = TruthFunctions.negation(truth);
                v2 = TruthFunctions.deduction(v1, reliance);
                truth = TruthFunctions.negation(v2);
            }

            budget = BudgetFunctions.forward(truth, nal);
        }

        if (content instanceof CompoundTerm)
            return nal.singlePremiseTask((CompoundTerm)content, truth, budget);
        else
            return false;
    }

    /* --------------- Negation related rules --------------- */
    /**
     * {A, A@(--, A)} |- (--, A)
     *
     * @param content The premise
     * @param nal Reference to the memory
     */
    public static void transformNegation(CompoundTerm content, NAL nal) {
        Task task = nal.getCurrentTask();
        Sentence sentence = task.sentence;
        TruthValue truth = sentence.truth;

        BudgetValue budget;
        
        if (sentence.isJudgment() || sentence.isGoal()) {
            truth = TruthFunctions.negation(truth);
            budget = BudgetFunctions.compoundForward(truth, content, nal);
        } else {
            budget = BudgetFunctions.compoundBackward(content, nal);
        }
        nal.singlePremiseTask(content, truth, budget);
    }

}
