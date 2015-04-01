package nars.prototype;

import nars.Core;
import nars.Memory;
import nars.budget.Budget;
import nars.nal.Concept;
import nars.nal.Sentence;
import nars.nal.Task;
import nars.nal.tlink.TaskLink;
import nars.nal.tlink.TermLink;
import nars.nal.tlink.TermLinkKey;
import nars.budget.Bag;
import nars.budget.bag.CurveBag;
import nars.budget.bag.CurveBag.FairPriorityProbabilityCurve;
import nars.nal.term.Compound;
import nars.nal.term.Term;


public class Curve extends Default {
    public final boolean randomRemoval;
    public final CurveBag.BagCurve curve;

    public Curve() {
        this(true);
    }
    
    public Curve(boolean randomRemoval) {
        this(new FairPriorityProbabilityCurve(), randomRemoval);        
    }
    
    public Curve(CurveBag.BagCurve curve, boolean randomRemoval) {
        super();
        this.randomRemoval = randomRemoval;
        this.curve = curve;
    }
    

    @Override
    public Bag<Sentence<Compound>, Task<Compound>> newNovelTaskBag() {
        return new CurveBag(getNovelTaskBagSize(), curve, randomRemoval);
    }

    @Override
    public Bag<Term, Concept> newConceptBag() {
        return new CurveBag<>(getConceptBagSize(), curve, randomRemoval);
        //return new AdaptiveContinuousBag<>(getConceptBagSize());
    }

    

    @Override
    public Core newCore() {
        //return new BalancedSequentialMemoryCycle(newConceptBag(p), c);
        //return new DefaultCore(newConceptBag(), newSubconceptBag(), getConceptBuilder(), newNovelTaskBag());
        return super.newCore();
    }
    
    @Override
    public Concept newConcept(Budget b, final Term t, final Memory m) {


        Bag<String, TaskLink> taskLinks = new CurveBag<>(getConceptTaskLinks(), curve, randomRemoval);
        Bag<TermLinkKey, TermLink> termLinks = new CurveBag<>(getConceptTermLinks(), curve, randomRemoval);
        
        return new Concept(b, t, taskLinks, termLinks, m);        
    }
    
}
