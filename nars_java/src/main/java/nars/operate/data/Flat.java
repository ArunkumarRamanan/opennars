package nars.operate.data;

import nars.nal.entity.Compound;
import nars.nal.entity.Term;
import nars.nal.nal3.SetTensional;
import nars.nal.nal4.Product;
import nars.nal.nal5.Conjunction;
import nars.nal.nal8.TermFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * recursively collects the contents of set/list compound term argument's
 * into a list, to one of several resulting term types:
 *      product
 *      set (TODO)
 *      conjunction (TODO)
 *
 * TODO recursive version with order=breadth|depth option
 */
abstract public class Flat extends TermFunction {

    public Flat(String name) {
        super(name);
    }

    @Override
    public Term function(Term[] x) {
        List<Term> l = new ArrayList();
        collect(x, l);
        return result(l);
    }
    public static List<Term> collect(Term[] x, List<Term> l) {
        for (Term a : x) {
            if ((a instanceof Product) || (a instanceof SetTensional) || (a instanceof Conjunction)) {
                collect( ((Compound)a).term, l);
            }
            else
                l.add(a);
        }
        return l;
    }

    abstract public Term result(List<Term> terms);

    public static class AsProduct extends Flat {

        public AsProduct() {
            super("^flatProduct");
        }

        @Override
        public Term result(List<Term> terms) {
            return new Product(terms);
        }

    }





    //public Flat(boolean productOrSet, boolean breadthOrDepth) {
        //generate each of the 4 different operate names

    //}
}
