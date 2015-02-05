/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.util.graph.experimental;

import com.google.common.base.Objects;
import nars.core.Events;
import nars.core.Events.ConceptForget;
import nars.core.Events.ConceptNew;
import nars.core.NAR;
import nars.core.Parameters;
import nars.event.Reaction;
import nars.io.Symbols;
import nars.logic.NALOperator;
import nars.logic.Terms.Termable;
import nars.logic.entity.*;
import nars.logic.nal4.Image;

import java.util.*;

/**
 *each of those rows can be a representation of something like a 'multiconcept' or 'aggregated concept' which combines concept data from related concepts
and tasks where the only differ by the top-level operator, tense, freq, conf,etc
 EXPERIMENTAL
 */
public class Idea implements Iterable<Concept> {
   
    final public Set<Concept> concepts = Collections.synchronizedSet(new HashSet());
    final CharSequence key;
    final Set<SentenceType> feature = new HashSet();
    final Set<NALOperator> operators = new HashSet<NALOperator>();


    public static CharSequence getKey(Termable tt) {
        Term t = tt.getTerm();
        if (t instanceof CompoundTerm) {
            CompoundTerm ct = (CompoundTerm)t;
            
            //TODO use an array -> strong conversion that eliminates the ' ' after comma, saving 1 char each term
            
            if (!ct.isCommutative()) {
                //if not commutative (order matters): key = list of subterms
                String s = Arrays.toString(ct.term).replaceFirst("\\[", "(");
                
                if (ct instanceof Image) {
                    int index = ((Image)ct).relationIndex;
                    s += "." + index;
                }
                
                return s;
            }            
            else {
                //key = sorted set of subterms
                return Term.toSortedSet(ct.term).toString();
            }
        }
        else {
            return t.name();
        }
    }
    
    
    public Idea(Concept c) {
        super();
        this.key = getKey(c.term);
        add(c);
    }
    
    public Idea(Iterable<Concept> c) {
        super();        
        this.key = getKey(c.iterator().next());
        for (Concept x : c)
            add(x);
    }
    
    public Set<NALOperator> operators() {
        return operators;
    }
    

    /** returns a sample term (ex: first concept's term);
        all Concepts will have equal sub-components */
    public Term getSampleTerm() {
        return concepts.iterator().next().getTerm();
    }
    
    /** number of concepts represented in this Idea */
    public int size() {        
        return concepts.size();
    }
    
    /** # of terms, which will be equal in all Concept terms */
    public int getArity() {        
        Term sampleTerm = getSampleTerm();
        if (sampleTerm instanceof CompoundTerm) {
            return ((CompoundTerm)sampleTerm).term.length;
        }
        return 1;
    }
    
    public class SentenceType implements Comparable<SentenceType> {
        
        public final NALOperator op;
        public final char punc;
                
        transient private final int hash;
        private ArrayList sentences;

        public SentenceType(NALOperator o, char c) {
            this.op = o;
            this.punc = c;
            this.hash = Objects.hashCode(op, punc);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof SentenceType)) return false;
            SentenceType x = (SentenceType)o;
            return x.op == op && x.punc == punc;
        }

        @Override
        public int compareTo(SentenceType t) {
            int i = op.compareTo(t.op);
            if (i != 0) return i;
            return Character.compare(punc, t.punc);
        }

        @Override
        public String toString() {
            return op.toString() + " " + punc;
        }
        
        public List<Sentence> getSentences() {
            if (sentences == null) {
                sentences = new ArrayList();
                for (Concept c : concepts) {
                    switch (punc) {
                        case Symbols.JUDGMENT:
                            sentences.addAll(c.beliefs);
                            break;
                        case Symbols.QUESTION:
                            sentences.addAll(Task.getSentences(c.questions));
                            break;
                        case Symbols.QUEST:
                            sentences.addAll(Task.getSentences(c.quests));
                            break;
                        case Symbols.GOAL:
                            sentences.addAll(c.goals);
                            break;
                    }
                }
            }
            return sentences;
        }
                
    }
    
    public Collection<Sentence> getSentences(SentenceType o) {
        List<Sentence> s = new ArrayList();
        for (Concept c : this) {
            if (c.term.operator() == o.op) {
                s.addAll(c.getSentences(o.punc));
            }
        }
        return s;
    }
    
    /** returns the set of all operator+punctuation concatenations */
    public Set<SentenceType> getSentenceTypes() {
        return feature;
    }
    
    /**
     * includes the concept in this idea.  it's ok to repeat add a 
     * concept again since they are stored as Set
     */
    public boolean add(Concept c) {
        if (Parameters.DEBUG)
            ensureMatchingConcept(c);
  
        boolean b = concepts.add(c);
        
        if (b) {
            update();
        }
        
        return b;
    }

    public void update() {
        
        operators.clear();
        feature.clear();
        
        for (Concept c : this) {
            NALOperator o = c.operator();
            operators.add(o);
            
            if (!c.beliefs.isEmpty())
                feature.add(new SentenceType(o, Symbols.JUDGMENT));
            if (!c.questions.isEmpty())
                feature.add(new SentenceType(o, Symbols.QUESTION));
            if (!c.goals.isEmpty())
                feature.add(new SentenceType(o, Symbols.GOAL));
            if (!c.quests.isEmpty())
                feature.add(new SentenceType(o, Symbols.QUEST));
        }
        
    }
    
    public boolean remove(Concept c) {
        if (Parameters.DEBUG)
            ensureMatchingConcept(c);
        
        boolean b = concepts.remove(c);
        if (b)
            update();
        return b;
    }
    
    public CharSequence key() {
        return key;
    }

    protected void ensureMatchingConcept(Concept c) {
        CharSequence ckey = getKey(c.term);
        if (!ckey.equals(key))
            throw new RuntimeException(c + " does not belong in Idea " + key);          }

    @Override
    public String toString() {
        return key() + concepts.toString();
    }

    @Override
    public Iterator<Concept> iterator() {
        return concepts.iterator();
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj==this) return true;
        if (obj instanceof Idea) {
            return key.equals(((Idea)obj).key);
        }
        return false;
    }
    
    public static class IdeaSet extends HashMap<CharSequence,Idea> implements Reaction {
        private final NAR nar;

        public IdeaSet(NAR n) {
            super();
            this.nar = n;
            enable(true);
        }

        @Override
        public void event(Class event, Object[] args) {
            if (event == ConceptNew.class) {
                add((Concept)args[0]);
            }
            else if (event == ConceptForget.class) {
                remove((Concept)args[0]);
            }
            else if (event == Events.TaskImmediateProcessed.class) {
                update((Concept)args[2]);
            }
        }
        
        
        public void enable(boolean enabled) {
            
            clear();
                        
            nar.memory.event.set(this, enabled, 
                    ConceptNew.class, ConceptForget.class, Events.TaskImmediateProcessed.class);
            
            if (enabled) {
                ///add existing
                for (Concept c : nar.memory.concepts)
                    add(c);            
            }
            
        }
        
        public Idea get(Termable t) {
            return get(Idea.getKey(t.getTerm()));
        }

        public Idea update(Concept c) {
            Idea existing = get(c);
            if (existing != null) {
                existing.update();
            }
            return existing;
        }

        public Idea add(Concept c) {
            Idea existing = get(c);
            if (existing == null) {
                existing = new Idea(c);
                put(Idea.getKey(c), existing); //calculating getKey() twice can be avoided by caching it when it's uesd to get Idea existing above
            }
            else {
                existing.add(c);
            }
            return existing;
        }
        
        public Idea remove(Concept c) {
            Idea existing = get(c);
            if (existing != null) {
                existing.remove(c);
            }
            return existing;
        }
        
    }
    
}
