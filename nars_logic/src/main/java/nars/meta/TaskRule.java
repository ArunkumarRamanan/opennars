package nars.meta;

import nars.nal.nal1.Inheritance;
import nars.nal.nal4.Product;
import nars.premise.Premise;
import nars.process.NAL;
import nars.task.Sentence;
import nars.task.Task;
import nars.term.Atom;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.term.transform.CompoundTransform;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A rule which produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class TaskRule extends Rule<Premise,Task> {

    private final Term[] preconditions; //the terms to match

    private final PostCondition[] postconditions;
    //it has certain pre-conditions, all given as predicates after the two input premises





    public TaskRule(Product premises, Product result) {
        super(premises, result);


        //1. construct precondition term array
        //Term[] terms = terms();

        Term[] precon = this.preconditions = premises.terms();
        Term[] postcons = result.terms();

        //The last entry is the postcondition
        this.normalizeDestructively();
        postconditions = new PostCondition[postcons.length / 2]; //term_1 meta_1 ,..., term_2 meta_2 ...

        int k = 0;
        for (int i = 0; i < postcons.length; ) {
            Term t = postcons[i++];
            if (i >= postcons.length)
                throw new RuntimeException("invalid rule: missing meta term for postcondition involving " + t);
            postconditions[k++] = new PostCondition(t,
                    ((Product)postcons[i++]).terms() );
        }

    }

    @Override
    protected void init(Term... term) {
        super.init(term);
    }

    public Product premise() {
        return (Product)term(0);
    }

    public Product result() {
        return (Product) term(1);
    }

    public int premiseCount() {
        return premise().length();
    }


    public static final Set<Atom> reservedPostconditions = new HashSet(6);
    static {
        reservedPostconditions.add(Atom.the("Truth"));
        reservedPostconditions.add(Atom.the("Stamp"));
        reservedPostconditions.add(Atom.the("Desire"));
        reservedPostconditions.add(Atom.the("Order"));
        reservedPostconditions.add(Atom.the("Info"));
        reservedPostconditions.add(Atom.the("Event"));
    }


    public static class TaskRuleNormalization implements CompoundTransform<Compound,Term> {


        @Override
        public boolean test(Term term) {
            if (term instanceof Atom) {
                String name = term.toString();
                return (Character.isUpperCase(name.charAt(0)));
            }
            return false;
        }

        @Override
        public Term apply(Compound containingCompound, Term v, int depth) {

            //do not alter postconditions
            if ((containingCompound instanceof Inheritance) && reservedPostconditions.contains(((Inheritance)containingCompound).getPredicate()))
                return v;

            return new Variable("%" + v.toString());
        }
    }

    final static TaskRuleNormalization taskRuleNormalization = new TaskRuleNormalization();

    @Override
    public TaskRule normalizeDestructively() {
        this.transform(taskRuleNormalization);
        this.invalidate();
        return this;
    }

    public TaskRule normalize() {
        return this;
    }

    public void forward(Task task, Sentence belief, Term beliefTerm, NAL nal) {
        //if preconditions are met:
        for (PostCondition p : postconditions)
            p.apply(preconditions, task, belief, beliefTerm, nal);
    }


    //TEMPORARY for testing, to make sure the postcondition equality guarantees rule equality
    public boolean deepEquals(Object obj) {
        /*
        the precondition uniqueness is guaranted because they exist as the terms of the rule meta-term which equality is already tested for
         */
        if (super.equals(obj)) {
            if (!Arrays.equals(postconditions, ((TaskRule)obj).postconditions)) {
                throw new RuntimeException(this + " and " + obj + " have equal Rule Product but inequal postconditions");
            }

            return true;
        }
        return false;
    }

}



