package nars.nal.op;

import nars.term.Term;
import nars.term.compound.Compound;
import nars.term.transform.MapSubst;
import org.jetbrains.annotations.Nullable;


public class substitute extends ImmediateTermTransform {

    @Override public Term function(Compound p) {
        final Term[] xx = p.terms();

        //term to possibly transform
        final Term term = xx[0];

        //original term (x)
        final Term x = xx[1];

        //replacement term (y)
        final Term y = xx[2];

//        Term a = m.apply(x, false);
//        if (a == null)
//            return false;
//
//        Term b = m.apply(y, false);
//        if (b == null)
//            return false;

        return subst(p, term, x, y);
    }

    @Nullable
    public Term subst(Compound p, Term term, Term x, Term y) {
        MapSubst m = new MapSubst(x, y);

        //if (!x.equals(y)) {

        if (substitute(p, m, x, y)) {
            return term.apply(m,
                !(this instanceof substituteIfUnifies)
            );
        }

        return null; //(this instanceof substituteIfUnifies) ? term : null;
    }

    protected boolean substitute(Compound p, MapSubst m, Term a, Term b) {
        //for subclasses to override; here it just falls through true
        return true;
    }
}