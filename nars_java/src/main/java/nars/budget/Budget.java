/*
 * BudgetValue.java
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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.budget;

import nars.Memory;
import nars.Global;
import nars.io.Symbols;
import nars.io.Texts;
import nars.nal.BudgetFunctions;
import nars.nal.Sentence;
import nars.nal.TruthValue;

import static nars.Global.BUDGET_EPSILON;
import static nars.Global.TRUTH_EPSILON;
import static nars.nal.BudgetFunctions.m;
import static nars.nal.UtilityFunctions.*;

/**
 * A triple of priority (current), durability (decay), and quality (long-term average).
 * TODO abstract/subclasses
 *      interface Budgetable
 *      ForgettableBudgetValue - includes the last forget time
 *      some subclasses / adapter classes for statistics,
 * monitoring or event notification on changes
 */
public class Budget implements Cloneable {

    /** The character that marks the two ends of a budget value */
    private static final char MARK = Symbols.BUDGET_VALUE_MARK;
    /** The character that separates the factors in a budget value */
    private static final char SEPARATOR = Symbols.VALUE_SEPARATOR;


    /** The relative share of time resource to be allocated */
    private float priority;

    /**
     * The percent of priority to be kept in a constant period; All priority
     * values "decay" over time, though at different rates. Each item is given a
     * "durability" factor in (0, 1) to specify the percentage of priority level
     * left after each reevaluation
     */
    private float durability;

    /** The overall (context-independent) evaluation */
    private float quality;

    /** time at which this budget was last forgotten, for calculating accurate memory decay rates */
    long lastForgetTime = -1;

    public Budget(char punctuation, TruthValue qualityFromTruth) {
        this(punctuation == Symbols.JUDGMENT ? Global.DEFAULT_JUDGMENT_PRIORITY :
                        (punctuation == Symbols.QUESTION ?  Global.DEFAULT_QUESTION_PRIORITY :
                                (punctuation == Symbols.GOAL ? Global.DEFAULT_GOAL_PRIORITY :
                                        Global.DEFAULT_QUEST_PRIORITY )),
                punctuation, qualityFromTruth);
    }

    public Budget(final float p, char punctuation, TruthValue qualityFromTruth) {
        this(p,
                punctuation == Symbols.JUDGMENT ? Global.DEFAULT_JUDGMENT_DURABILITY :
                        (punctuation == Symbols.QUESTION ?  Global.DEFAULT_QUESTION_DURABILITY :
                                (punctuation == Symbols.GOAL ? Global.DEFAULT_GOAL_DURABILITY :
                                        Global.DEFAULT_QUEST_DURABILITY )),
                qualityFromTruth);
    }

    public Budget(final float p, final float d, final TruthValue qualityFromTruth) {
        this(p, d, qualityFromTruth !=
                null ? BudgetFunctions.truthToQuality(qualityFromTruth) : 1f);
    }


    /**
     * Constructor with initialization
     * @param p Initial priority
     * @param d Initial durability
     * @param q Initial quality
     */
    public Budget(final float p, final float d, final float q) {
        setPriority(p);
        setDurability(d);
        setQuality(q);
    }

    /**
     * Cloning constructor
     * @param v Budget value to be cloned
     */
    public Budget(final Budget v) {
        this(v.getPriority(), v.getDurability(), v.getQuality());
    }

    /**
     * Cloning method
     */
    @Override
    public Budget clone() {
        return new Budget(this.getPriority(), this.getDurability(), this.getQuality());
    }

    /**
     * priority: adds the value of another budgetvalue to this; all components max at 1.0
     * durability: max(this, b) (similar to merge)
     * quality: max(this, b)    (similar to merge)
     * */
    public Budget accumulate(Budget b) {
        return accumulate(b.getPriority(), b.getDurability(), b.getQuality());
    }

    public Budget accumulate(final float addPriority, final float addDurability, final float addQuality) {
        setPriority(Math.min(1f, getPriority() + addPriority)); //add priority
        setDurability(m(getDurability(), addDurability)); //max durab
        setQuality(m(getQuality(), addQuality)); //max quali
        return this;
    }

    /** set all quantities to zero */
    public Budget zero() {
        setPriority(0);
        setDurability(0);
        setQuality(0);
        return this;
    }

    /**
     * Get priority value
     * @return The current priority
     */
    public float getPriority() {
        return priority;
    }

    /**
     * Change priority value
     * @param v The new priority
     * @return whether the operation had any effect
     */
    public final boolean setPriority(float v) {
        if(v>1.0) {
            v=1.0f;
            //throw new RuntimeException("priority value above 1");
        }
        else if (v < 0) {
            throw new RuntimeException("negative priority value");
        }

        final float dp;
        if (priority >= v) dp = priority - v;
        else dp = v - priority;
        if (dp < BUDGET_EPSILON)
            return false;

        priority = v;
        return true;
    }

    /**
     * Change durability value
     * @param d The new durability
     */
    public boolean setDurability(float d) {
        if (Global.DEBUG) ensureBetweenZeroAndOne(d);

        if(d>=1.0) {
            d = (float) (1.0-TRUTH_EPSILON);
            //throw new RuntimeException("durability value above or equal 1");
        }
        /*else if (d < BUDGET_EPSILON) {
            System.err.println("zero durability");
        }*/

        final float dp;
        if (durability >= d) dp = durability - d;
        else dp = d - durability;
        if (dp < BUDGET_EPSILON)
            return false;

        durability = d;
        return true;
    }

    /**
     * Change quality value
     * @param v The new quality
     */
    public boolean setQuality(final float v) {
        if (Global.DEBUG) ensureBetweenZeroAndOne(v);

        final float dp;
        if (quality >= v) dp = quality - v;
        else dp = v - quality;
        if (dp < BUDGET_EPSILON)
            return false;

        quality = v;
        return true;
    }

    public static void ensureBetweenZeroAndOne(float v) {
        if (Float.isNaN(v))
            throw new RuntimeException("Priority is NaN");
        if (v > 1.0f)
            throw new RuntimeException("Priority > 1.0: " + v);
        if (v < 0f)
            throw new RuntimeException("Priority < 1.0: " + v);
    }

    /**
     * Increase priority value by a percentage of the remaining range
     * @param v The increasing percent
     */
    public void incPriority(final float v) {
        setPriority( (float) Math.min(1.0, or(priority, v)));
    }

    /** AND's (multiplies) priority with another value */
    public void andPriority(final float v) {
        setPriority( and(priority, v) );
    }

    /**
     * Decrease priority value by a percentage of the remaining range
     * @param v The decreasing percent
     */
    public void decPriority(final float v) {
        setPriority( and(priority, v) );
    }

    /**
     * Get durability value
     * @return The current durability
     */
    public float getDurability() {
        return durability;
    }



    /**
     * Increase durability value by a percentage of the remaining range
     * @param v The increasing percent
     */
    public void incDurability(final float v) {
        setDurability(or(durability, v));
    }

    /**
     * Decrease durability value by a percentage of the remaining range
     * @param v The decreasing percent
     */
    public void decDurability(final float v) {
        setDurability(and(durability, v));
    }

    /**
     * Get quality value
     * @return The current quality
     */
    public float getQuality() {
        return quality;
    }


    /**
     * Increase quality value by a percentage of the remaining range
     * @param v The increasing percent
     */
    public void incQuality(final float v) {
        quality = or(quality, v);
    }

    /**
     * Decrease quality value by a percentage of the remaining range
     * @param v The decreasing percent
     */
    public void decQuality(final float v) {
        quality = and(quality, v);
    }

    /**
     * Merge one BudgetValue into another
     * @param that The other Budget
     * @return whether the merge had any effect
     */
    public final boolean merge(final Budget that) {
        return BudgetFunctions.merge(this, that);
    }

//    /**
//     * returns true if this budget is greater in all quantities than another budget,
//     * used to prevent a merge that would have no consequence
//     * NOT TESTED
//     * @param other
//     * @return
//     */
//    public boolean greaterThan(final BudgetValue other) {
//        return (getPriority() - other.getPriority() > Parameters.BUDGET_THRESHOLD) &&
//                (getDurability()- other.getDurability()> Parameters.BUDGET_THRESHOLD) &&
//                (getQuality() - other.getQuality() > Parameters.BUDGET_THRESHOLD);
//    }


    /**
     * To summarize a BudgetValue into a single number in [0, 1]
     * @return The summary value
     */
    public float summary() {
        return aveGeo(priority, durability, quality);
    }

    public float summary(float additionalPriority) {
        return aveGeo(Math.min(1.0f, priority + additionalPriority), durability, quality);
    }


    public boolean equalsByPrecision(final Object that) {
        if (that instanceof Budget) {
            final Budget t = ((Budget) that);
            float dPrio = Math.abs(getPriority() - t.getPriority());
            if (dPrio >= TRUTH_EPSILON) return false;
            float dDura = Math.abs(getDurability() - t.getDurability());
            if (dDura >= TRUTH_EPSILON) return false;
            float dQual = Math.abs(getQuality() - t.getQuality());
            if (dQual >= TRUTH_EPSILON) return false;
            return true;
        }
        return false;
    }


    /**
     * Whether the budget should get any processing at all
     * <p>
     * to be revised to depend on how busy the system is
     * @return The decision on whether to process the Item
     */
    public boolean aboveThreshold() {
        return (summary() >= Global.BUDGET_THRESHOLD);
    }

    /* Whether budget is above threshold, with the involvement of additional priority (saved previously, or boosting)
     * @param additionalPriority saved credit to contribute to possibly push it over threshold
     */
    public boolean aboveThreshold(float additionalPriority) {
        return (summary(additionalPriority) >= Global.BUDGET_THRESHOLD);
    }

    public static Budget budgetIfAboveThreshold(float pri, float dur, float qua) {
        if (aveGeo(pri, dur, qua) >= Global.BUDGET_THRESHOLD)
            return new Budget(pri, dur, qua);
        return null;
    }

//    /**
//     * Whether budget is above threshold, with the involvement of additional priority (saved previously, or boosting)
//     * @param additionalPriority
//     * @return NaN if neither aboveThreshold, nor aboveThreshold with additional priority; 0 if no additional priority necessary to make above threshold, > 0 if that amount of the additional priority was "spent" to cause it to go above threshold
//     */
//    public float aboveThreshold(float additionalPriority) {
//        float s = summary();
//        if (s >= Parameters.BUDGET_THRESHOLD)
//            return 0;
//        if (summary(additionalPriority) >= Parameters.BUDGET_EPSILON) {
//            //calculate how much was necessary
//
//            float dT = Parameters.BUDGET_THRESHOLD - s; //difference between how much needed
//
//            //TODO solve for additional:
//            //  newSummary - s = dT
//            //  ((priority+additional)*(duration)*(quality))^(1/3) - s = dT;
//
//            float used = 0;
//        }
//        return Float.NaN;
//    }



    /**
     * Fully display the BudgetValue
     * @return String representation of the value
     */
    @Override
    public String toString() {
        return MARK + Texts.n4(priority) + SEPARATOR + Texts.n4(durability) + SEPARATOR + Texts.n4(quality) + MARK;
    }

    /**
     * Briefly display the BudgetValue
     * @return String representation of the value with 2-digit accuracy
     */
    public String toStringExternal() {
        //return MARK + priority.toStringBrief() + SEPARATOR + durability.toStringBrief() + SEPARATOR + quality.toStringBrief() + MARK;

        final CharSequence priorityString = Texts.n2(priority);
        final CharSequence durabilityString = Texts.n2(durability);
        final CharSequence qualityString = Texts.n2(quality);
        return new StringBuilder(1 + priorityString.length() + 1 + durabilityString.length() + 1 + qualityString.length() + 1)
                .append(MARK)
                .append(priorityString).append(SEPARATOR)
                .append(durabilityString).append(SEPARATOR)
                .append(qualityString)
                .append(MARK)
                .toString();
    }

    /** 1 digit resolution */
    public String toStringExternal1(boolean includeQuality) {
        final char priorityString = Texts.n1(priority);
        final char durabilityString = Texts.n1(durability);
        StringBuilder sb = new StringBuilder(1 + 1 + 1 + (includeQuality ? 1 : 0) + 1)
                .append(MARK)
                .append(priorityString).append(SEPARATOR)
                .append(durabilityString);

        if (includeQuality)
            sb.append(SEPARATOR).append(Texts.n1(quality));

        return sb.append(MARK).toString();
    }

    /**
     * linear interpolate the priority value to another value
     * @see https://en.wikipedia.org/wiki/Linear_interpolation
     */
    /*public void lerpPriority(final float targetValue, final float momentum) {
        if (momentum == 1.0) 
            return;
        else if (momentum == 0) 
            setPriority(targetValue);
        else
            setPriority( (getPriority() * momentum) + ((1f - momentum) * targetValue) );
    }*/

    /** returns the period in time: currentTime - lastForgetTime and sets the lastForgetTime to currentTime */
    public long setLastForgetTime(final long currentTime) {
        long period;
        if (this.lastForgetTime == -1)
            period = 0;
        else
            period = currentTime - lastForgetTime;

        lastForgetTime = currentTime;

        return period;
    }

    public long getLastForgetTime() {
        return lastForgetTime;
    }

    /** creates a new budget value appropriate for a given sentence type and memory's current parameters */
    public static Budget newDefault(Sentence s, Memory memory) {
        float priority, durability;
        switch (s.punctuation) {
            case Symbols.JUDGMENT:
                priority = Global.DEFAULT_JUDGMENT_PRIORITY;
                durability = Global.DEFAULT_JUDGMENT_DURABILITY;
                break;
            case Symbols.QUEST:
            case Symbols.QUESTION:
                priority = Global.DEFAULT_QUESTION_PRIORITY;
                durability = Global.DEFAULT_QUESTION_DURABILITY;
                break;
            case Symbols.GOAL:
                priority = Global.DEFAULT_GOAL_PRIORITY;
                durability = Global.DEFAULT_GOAL_DURABILITY;
                break;
            default:
                throw new RuntimeException("Unknown sentence type: " + s.punctuation);
        }
        return new Budget(priority, durability, s.getTruth());
    }

    public Budget set(final float p, final float d, final float q) {
        setPriority(p);
        setDurability(d);
        setQuality(q);
        return this;
    }

    public Budget set(final Budget b) {
        return set(b.getPriority(), b.getDurability(), b.getQuality());
    }

    public boolean isNew() {
        return this.lastForgetTime==-1;
    }

    /** call this after an item has been used (ex: fired) to distinguish it from being
        new. this allows new items to remain unforgotten until they are first used.  */
    public void setUsed(long now) {
        if (isNew())
            setLastForgetTime(now);
    }


    /** indicates an implementation has, or is associated with a specific BudgetValue */
    public interface Budgetable {
        public Budget getBudget();
    }
}
