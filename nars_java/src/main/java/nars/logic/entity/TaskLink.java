/*
 * TaskLink.java
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
package nars.logic.entity;

import nars.core.Parameters;
import nars.io.Symbols;
import nars.logic.Terms.Termable;
import nars.logic.entity.Sentence.Sentenced;
import nars.logic.entity.tlink.TermLinkTemplate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Reference to a Task.
 * <p>
 * The reason to separate a Task and a TaskLink is that the same Task can be
 * linked from multiple Concepts, with different BudgetValue.
 * 
 * TaskLinks are unique according to the Task they reference
 */
public class TaskLink extends Item<String> implements TLink<Task>, Termable, Sentenced {

    /**
     * The Task linked
     */
    public final Task targetTask;

    private String name;

    public static String key(short type, short[] index, Task task) {
        if (Parameters.TASK_LINK_UNIQUE_BY_INDEX)
            return TermLinkTemplate.prefix(type, index, false) + Symbols.TLinkSeparator + task.sentence.name();
        else
            return task.sentence.name();
    }


    /* Remember the TermLinks, and when they has been used recently with this TaskLink */
    public final static class Recording {
    
        public final TermLink link;
        long time;

        public Recording(TermLink link, long time) {
            this.link = link;
            this.time = time;
        }

        public long getTime() {
            return time;
        }

        
        public void setTime(long t) {
            this.time = t;
        }

        @Override
        public String toString() {
            return link + "@" + time;
        }
    }
    
    Deque<Recording> records;
    

    
    /** The type of tlink, one of the above */
    public final short type;

    /** The index of the component in the component list of the compound, may have up to 4 levels */
    public final short[] index;


    public TaskLink(final Task t, final BudgetValue v) {
        super(v);
        this.type = TermLink.SELF;
        this.index = null;

        this.targetTask = t;

        this.name = null;
    }

    /**
     * Constructor
     * <p>
     *
     * @param t The target Task
     * @param template The TermLink template
     * @param v The budget
     */
    public TaskLink(final Task t, final TermLinkTemplate template, final BudgetValue v) {
        super(v);
        this.type = template.type;
        this.index = template.index;
        
        this.targetTask = t;
        
        this.name = null;
    }


    @Override
    public String name() {
        if (name == null) {
            this.name = key(type, index, targetTask);
        }
        return name;
    }

    @Override
    public int hashCode() {
        return name().hashCode();
    }

    public Deque<Recording> getRecords() {
        return records;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof TaskLink) {
            TaskLink t = (TaskLink)obj;
            //return t.name().equals(name());
            return t.targetTask.equals(targetTask);
        }
        return false;
    }    
    
    /**
     * Get one index by level
     * @param i The index level
     * @return The index value
     */
    @Override
    public final short getIndex(final int i) {
        if ((index != null) && (i < index.length)) {
            return index[i];
        } else {
            return -1;
        }
    }             

    /**
     * To check whether a TaskLink should use a TermLink, return false if they
     * interacted recently
     * <p>
     * called in TermLinkBag only
     *
     * @param termLink The TermLink to be checked
     * @param currentTime The current time
     * @return Whether they are novel to each other
     */
    public boolean novel(final TermLink termLink, final long currentTime, int noveltyHorizon, int recordLength) {


        final Term bTerm = termLink.target;
        if (bTerm.equals(targetTask.sentence.term)) {            
            return false;
        }

        if (noveltyHorizon == 0) return true;

        if (records==null) {
            records = new ArrayDeque(recordLength);
        }

        //TODO remove old entries from records if recordLength < records.size()  -- for dynamic adjusting of novelty parameters

        //iterating the FIFO deque from oldest (first) to newest (last)
        Iterator<Recording> ir = records.iterator();
        while (ir.hasNext()) {
            Recording r = ir.next();
            if (termLink.equals(r.link)) {
                if (currentTime < r.getTime() + noveltyHorizon) {
                    //too recent, not novel
                    return false;
                } else {
                    //happened long enough ago that we have forgotten it somewhat, making it seem more novel
                    r.setTime(currentTime);
                    ir.remove();
                    records.addLast(r);
                    return true;
                }
            }
            else if (currentTime > r.getTime() + noveltyHorizon) {
                //remove a record which will not apply to any other tlink
                ir.remove();
            }
        }
        
        
        //keep recordedLinks queue a maximum finite size
        while (!records.isEmpty() && records.size() + 1 >= recordLength) records.removeFirst();
        
        // add knowledge reference to recordedLinks
        records.addLast(new Recording(termLink, currentTime));
        
        return true;
    }

    @Override
    public String toString() {
        return name().toString();
    }
    

    /**
    * Get the target Task
    *
    * @return The linked Task
    */
    @Override public Task getTarget() {
        return getTask();
    }

    public Task getTask() { return targetTask; }

    @Override
    public void end() {
        if (records !=null) {
            records.clear();
            records = null;
        }
    }

    @Override
    public Term getTerm() {
        return getTarget().getTerm();
    }

    @Override
    public Sentence getSentence() {   return getTarget().sentence;  }

    
    
    
}
