package nars.logic.reason.concept;

import com.google.common.collect.Lists;
import nars.core.Memory;
import nars.core.Parameters;
import nars.io.Symbols;
import nars.logic.BudgetFunctions;
import nars.logic.NAL;
import nars.logic.TruthFunctions;
import nars.logic.Variables;
import nars.logic.entity.*;
import nars.logic.nal5.Conjunction;
import nars.logic.nal5.Implication;
import nars.logic.nal7.TemporalRules;
import nars.logic.reason.ConceptFire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static nars.logic.Terms.equalSubTermsInRespectToImageAndProduct;

/**
* Created by me on 2/7/15.
*/
public class TemporalInductionChain extends ConceptFireTaskTerm {


    @Override
    public boolean apply(ConceptFire f, TaskLink taskLink, TermLink termLink) {

        if (!f.nal(7)) return true;
        final Sentence belief = f.getCurrentBelief();
        if (belief == null) return true;

        final Memory memory = f.memory;

        final Term beliefTerm = belief.getTerm();


        //this is a new attempt/experiment to make nars effectively track temporal coherences
        if (beliefTerm instanceof Implication &&
                (beliefTerm.getTemporalOrder() == TemporalRules.ORDER_FORWARD || beliefTerm.getTemporalOrder() == TemporalRules.ORDER_CONCURRENT)) {

            final int chainSamples = Parameters.TEMPORAL_INDUCTION_CHAIN_SAMPLES;

            //prevent duplicate inductions
            Set<Object> alreadyInducted = Parameters.newHashSet(chainSamples);

            for (int i = 0; i < chainSamples; i++) {

                Concept next = memory.concepts.sampleNextConcept();
                if (next == null) continue;

                Term t = next.getTerm();

                if ((t instanceof Implication) && (alreadyInducted.add(t))) {

                    Implication implication = (Implication) t;

                    if (implication.isForward() || implication.isConcurrent()) {

                        Sentence s = next.getBestBelief();
                        if (s!=null) {
                            temporalInductionChain(s, belief, f);
                            temporalInductionChain(belief, s, f);
                        }
                    }
                }
            }
        }

        return true;

    }

    // { A =/> B, B =/> C } |- (&/,A,B) =/> C
    // { A =/> B, (&/,B,...) =/> C } |-  (&/,A,B,...) =/> C
    //https://groups.google.com/forum/#!topic/open-nars/L1spXagCOh4
    public static boolean temporalInductionChain(final Sentence s1, final Sentence s2, final NAL nal) {

        //prevent trying question sentences, causes NPE
        if ((s1.truth == null) || (s2.truth == null))
            return false;

        //try if B1 unifies with B2, if yes, create new judgement
        Implication S1=(Implication) s1.term;
        Implication S2=(Implication) s2.term;
        Term A=S1.getSubject();
        Term B1=S1.getPredicate();
        Term B2=S2.getSubject();
        Term C=S2.getPredicate();
        ArrayList<Term> args=null;

        int beginoffset=0;
        if(B2 instanceof Conjunction) {
            Conjunction CB2=((Conjunction)B2);
            if(CB2.getTemporalOrder()==TemporalRules.ORDER_FORWARD) {
                if(A instanceof Conjunction && A.getTemporalOrder()==TemporalRules.ORDER_FORWARD) {
                    Conjunction ConjA=(Conjunction) A;
                    args=new ArrayList(CB2.term.length+ConjA.term.length);
                    beginoffset=ConjA.size();

                    Collections.addAll(args, ConjA.term);
                } else {
                    args = new ArrayList(CB2.term.length + 1);
                    args.add(A);
                    beginoffset=1;
                }
                Collections.addAll(args, CB2.term);
            }
        }
        else {
            args= Lists.newArrayList(A, B1);
        }

        if(args==null)
            return false;

        //ok we have our B2, no matter if packed as first argument of &/ or directly, lets see if it unifies
        Term[] term = args.toArray(new Term[args.size()]);
        Term realB2 = term[beginoffset];

        Map<Term, Term> res1 = Parameters.newHashMap();
        Map<Term, Term> res2 = Parameters.newHashMap();

        if(Variables.findSubstitute(Symbols.VAR_INDEPENDENT, B1, realB2, res1, res2)) {
            //ok it unifies, so lets create a &/ term
            for(int i=0;i<term.length;i++) {
                final Term ti = term[i];
                if (ti instanceof CompoundTerm) {
                    Term ts = ((CompoundTerm)ti).applySubstitute(res1);
                    if(ts!=null)
                        term[i] = ts;
                }
                else {
                    term[i] = res1.getOrDefault(ti,ti);
                }
            }
            int order1=s1.getTemporalOrder();
            int order2=s2.getTemporalOrder();

            //check if term has a element which is equal to C
            for(Term t : term) {
                if(equalSubTermsInRespectToImageAndProduct(t, C)) {
                    return false;
                }
                for(Term u : term) {
                    if(u!=t) { //important: checking reference here is as it should be!
                        if(equalSubTermsInRespectToImageAndProduct(t, u)) {
                            return false;
                        }
                    }
                }
            }

            Term S = Conjunction.make(term,order1);
            Implication whole=Implication.make(S, C,order2);

            if(whole!=null) {
                TruthValue truth = TruthFunctions.deduction(s1.truth, s2.truth);
                BudgetValue budget = BudgetFunctions.forward(truth, nal);
                budget.setPriority((float) Math.min(0.99, budget.getPriority()));

                return nal.doublePremiseTask(whole, truth, budget,
                        nal.newStamp(s1, s2),
                        true);
            }
        }
        return false;
    }

}
