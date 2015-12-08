/*
 * CompoundTerm.java
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
package nars.term.compound;

import nars.Global;
import nars.Op;
import nars.nal.meta.match.Ellipsis;
import nars.nal.meta.match.EllipsisMatch;
import nars.nal.nal3.SetExt;
import nars.nal.nal3.SetInt;
import nars.nal.nal4.Product;
import nars.term.Term;
import nars.term.TermContainer;
import nars.term.Terms;
import nars.term.transform.CompoundTransform;
import nars.term.transform.FindSubst;
import nars.term.transform.Subst;
import nars.term.transform.VariableNormalization;
import nars.util.data.sexpression.IPair;
import nars.util.data.sexpression.Pair;
import nars.util.utf8.ByteBuf;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static nars.Symbols.*;
import static nars.util.data.Util.hashCombine;

/**
 * a compound term
 * TODO make this an interface extending Subterms
 */
public interface Compound<T extends Term> extends Term, IPair, TermContainer<T> {



    /**
     * Must be Term return type because the type of Term may change with different arguments
     */
    Term clone(final Term[] replaced);

    static void ensureFeasibleVolume(int vol) {
        if (vol > Global.COMPOUND_VOLUME_MAX) {
            throw new RuntimeException("volume limit exceeded");
        }
    }


    static void appendSeparator(final Appendable p, final boolean pretty) throws IOException {
        p.append(ARGUMENT_SEPARATOR);
        if (pretty) p.append(' ');
    }

    static void writeCompound1(final Op op, final Term singleTerm, Appendable writer, boolean pretty) throws IOException {
        writer.append(COMPOUND_TERM_OPENER);
        writer.append(op.str);
        writer.append(ARGUMENT_SEPARATOR);
        singleTerm.append(writer, pretty);
        writer.append(COMPOUND_TERM_CLOSER);
    }

    static byte[] newCompound1Key(final Op op, final Term singleTerm) {

        final byte[] opBytes = op.bytes;
        if (opBytes.length > 1)
            throw new RuntimeException("Compound1 operators must have a 1 char representation; invalid: " + op);
        byte opByte = opBytes[0];

        final byte[] termBytes = singleTerm.bytes();

        return ByteBuf.create(opBytes.length + termBytes.length)
                .add(opByte)
                .add(termBytes)
                .toBytes();
    }




    /** gets the set of unique recursively contained terms of a specific type
     * TODO generalize to a provided lambda predicate selector
     * */
    default Set<Term> unique(final Op type) {
        Set<Term> t = Global.newHashSet(0);
        //final int[] has = {0};
        recurseTerms((t1, superterm) -> {
            if (t1.op() == type)
                t.add(t1);
        });
        return t;
    }


    default Term substituted(Subst f) {

        Term s = Term.substituted(this, f);
        if (s!=null)
            return s;

        final int len = size();
        List<Term> sub = Global.newArrayList(len);

        EllipsisMatch post = null;


        boolean changed = false;


        for (int i = 0; i < len; i++) {
            //t holds the
            final Term t = term(i);

            //TODO change this to Compound.resolve(..,target) method call allowing any Compound to custom-handle its substitution contribution

            if (t instanceof Ellipsis) {

                Term te = f.getXY(t);
                if (te == null)
                    return this;

                changed = true; //even if the ellipsis matches blank it is a change

                EllipsisMatch am = (EllipsisMatch) te;
                if (am instanceof EllipsisMatch) {
                    if (post!=null) throw new RuntimeException("substitution alread involves a post-filter: " + post + " which conflicts with " + am);
                    if (!am.resolve(f, sub))
                        return this;
                    post = am;
                } else {
                    //default
//                    for (Term xx : am.term) {
//                        if (xx== Ellipsis.Shim)
//                            continue; //ignore any '..' which may be present in the expansion
//                        sub.add(xx);
//                    }
                    throw new RuntimeException("expected ellipsis match: " + am);
                }

            } else if (t == Ellipsis.Shim) {
                continue; //skip
            } else {
                // s holds a replacement substitution for t (i-th subterm of c)
                Term u = t.substituted(f);
                if (u == null)
                    u = t;
                else
                    changed |= (u!=t);


                sub.add(u);
            }
        }

        if (!changed)
            return this;

        final Term[] r = Terms.toArray(sub);

        if (post!=null) {
            return post.build(r, this);
        } else {
            //default
            return clone(r);
        }

    }



//    /**
//     * from: http://stackoverflow.com/a/19333201
//     */
//    public static <T> void shuffle(final T[] array, final Random random) {
//        int count = array.length;
//
//        //probabality for no shuffle at all:
//        if (random.nextInt(factorial(count)) == 0) return;
//
//        for (int i = count; i > 1; i--) {
//            final int a = i - 1;
//            final int b = random.nextInt(i);
//            if (b!=a) {
//                final T t = array[b];
//                array[b] = array[a];
//                array[a] = t;
//            }
//        }
//    }

    static Term unwrap(Term x, boolean unwrapLen1SetExt, boolean unwrapLen1SetInt, boolean unwrapLen1Product) {
        if (x instanceof Compound) {
            Compound c = (Compound) x;
            if (c.size() == 1) {
                if ((unwrapLen1SetInt && (c instanceof SetInt)) ||
                        (unwrapLen1SetExt && (c instanceof SetExt)) ||
                        (unwrapLen1Product && (c instanceof Product))
                        ) {
                    return c.term(0);
                }
            }
        }

        return x;
    }


    @Override
    default void append(final Appendable p, final boolean pretty) throws IOException {

        boolean opener = appendTermOpener();
        if (opener)
            p.append(COMPOUND_TERM_OPENER);


        final boolean appendedOperator = appendOperator(p);


        appendArgs(p, pretty, appendedOperator);


        appendCloser(p);

    }

    default void appendArgs(Appendable p, boolean pretty, boolean appendedOperator) throws IOException {
        int nterms = size();
        for (int i = 0; i < nterms; i++) {
             if ((i != 0) || (/*i == 0 &&*/ nterms > 1 && appendedOperator)) {
                p.append(ARGUMENT_SEPARATOR);
                if (pretty)
                    p.append(' ');
            }

            term(i).append(p, pretty);
        }
    }

    default boolean appendOperator(Appendable p) throws IOException {
        p.append(op().str);
        return true;
    }

    default void appendCloser(Appendable p) throws IOException {
        p.append(COMPOUND_TERM_CLOSER);
    }

    default boolean appendTermOpener() {
        return true;
    }

    default Term term(int i, boolean unwrapLen1SetExt, boolean unwrapLen1SetInt, boolean unwrapLen1Product) {
        Term x = term(i);
        return Compound.unwrap(x, unwrapLen1SetExt, unwrapLen1SetInt, unwrapLen1Product);
    }


    @Override
    default StringBuilder toStringBuilder(boolean pretty) {
        StringBuilder sb = new StringBuilder();
        try {
            append(sb, pretty);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb;
    }

    /** returns how many subterms were modified, or -1 if failure (ex: results in invalid term) */
    default <T extends Term> int transform(final CompoundTransform<Compound<T>, T> trans, Term[] target, final int level) {
        final int n = size();

        int modifications = 0;

        for (int i = 0; i < n; i++) {
            Term x = this.term(i);
            if (x == null)
                throw new RuntimeException("null subterm");

            if (trans.test(x)) {

                Term y = trans.apply( (Compound<T>)this, (T) x, level);
                if (y == null)
                    return -1;

                if (!x.equals(y)) {
                    modifications++;
                    x = y;
                }

            } else if (x instanceof Compound) {
                //recurse
                Compound cx = (Compound) x;
                if (trans.testSuperTerm(cx)) {

                    Term[] yy = new Term[cx.size()];
                    int submods = cx.transform(trans, yy, level + 1);

                    if (submods == -1) return -1;
                    if (submods > 0) {
                        x = cx.clone(yy);
                        modifications++;
                    }
                }
            }
            target[i] = x;
        }

        return modifications;
    }


    /**
     * extracts a subterm provided by the address tuple
     * returns null if specified subterm does not exist
     */
    default <X extends Term> X subterm(final int... address) {
        Term ptr = this;
        for (final int i : address) {
            if (ptr instanceof Compound) {
                ptr = ((Compound)ptr).term(i);
            }
        }
        return (X) ptr;
    }









//    @Override
//    public boolean equals(final Object that) {
//        if (!(that instanceof CompoundTerm))
//            return false;
//        
//        final CompoundTerm t = (CompoundTerm)that;
//        return name().equals(t.name());
//        
//        /*if (hashCode() != t.hashCode())
//            return false;
//        
//        if (operate() != t.operate())
//            return false;
//        
//        if (size() != t.size())
//            return false;
//        
//        for (int i = 0; i < term.size(); i++) {
//            final Term c = term.apply(i);
//            if (!c.equals(t.componentAt(i)))
//                return false;
//        }
//        
//        return true;*/
//        
//    }




    /**
     * Normalizes if contain variables which need to be finalized for use in a Sentence
     * May return null if the resulting compound term is invalid
     */
    @Override
    default <T extends Term> T normalized() {
        if (isNormalized())
            return (T) this;

        final Compound result = VariableNormalization.normalizeFast(this).get();
        if (result == null)
            return null;

        result.setNormalized(true);

        return (T) result;
    }

    default <X extends Compound> X transform(final CompoundTransform t) {
        return transform(t, true);
    }

    default <X extends Compound> X transform(final CompoundTransform t, boolean requireEqualityForNewInstance) {
        if (t.testSuperTerm(this)) {

            Term[] cls = new Term[size()];

            int mods = transform(t, cls, 0);

            if (mods == -1) {
                return null;
            }
            else if (!requireEqualityForNewInstance || (mods > 0)) {
                return (X) clone(cls);
            }
            //else if mods==0, fall through:
        }
        return (X) this; //nothing changed
    }


    @Override
    default boolean levelValid(final int nal) {
        if (!op().levelValid(nal))
            return false;

        //TODO use structural hash
        int n = size();
        for (int i = 0; i < n; i++) {
            Term sub = term(i);
            if (!sub.levelValid(nal))
                return false;
        }
        return true;
    }

    //boolean transform(CompoundTransform<Compound<T>, T> trans, int depth);



//    /**
//     * returns result of applySubstitute, if and only if it's a CompoundTerm.
//     * otherwise it is null
//     */
//    default Compound applySubstituteToCompound(Map<Term, Term> substitute) {
//        Term t = Term.substituted(this,
//                new MapSubst(substitute));
//        if (t instanceof Compound)
//            return ((Compound) t);
//        return null;
//    }


    TermContainer<T> subterms();

//    public Term[] cloneTermsReplacing(final Term from, final Term to) {
//        Term[] y = new Term[length()];
//        int i = 0;
//        for (Term x : term) {
//            if (x.equals(from))
//                x = to;
//            y[i++] = x;
//        }
//        return y;
//    }


    @Override
    default String toStringCompact() {
        return toString(false);
    }

    @Override
    default String toString(boolean pretty) {
        return toStringBuilder(pretty).toString();
    }

    @Override
    default Object _car() {
        //if length > 0
        return term(0);
    }

    /**
     * cdr or 'rest' function for s-expression interface when arity > 1
     */
    @Override
    default Object _cdr() {
        final int len = size();
        if (len == 1) throw new RuntimeException("Pair fault");
        if (len == 2) return term(1);
        if (len == 3) return new Pair(term(1), term(2));
        if (len == 4) return new Pair(term(1), new Pair(term(2), term(3)));

        //this may need tested better:
        Pair p = null;
        for (int i = len - 2; i >= 0; i--) {
            if (p == null)
                p = new Pair(term(i), term(i + 1));
            else
                p = new Pair(term(i), p);
        }
        return p;
    }

    @Override
    default Object setFirst(Object first) {
        throw new RuntimeException(this + " not modifiable");
    }

    @Override
    default Object setRest(Object rest) {
        throw new RuntimeException(this + " not modifiable");
    }

    void addAllTo(Collection<Term> set);

    /** universal compound hash function */
    static <T extends Term> int hash(CompoundN c, int hashSalt) {
        int h = hashCombine( c.subterms().hashCode(), c.op().ordinal() );
        if (hashSalt!=0)
            h = hashCombine(h, hashSalt);
        return h;
    }

    /**
     * unification matching entry point (default implementation)
     *
     * @param y compound to match against (the instance executing this method is considered 'x')
     * @param subst the substitution context holding the match state
     * @return whether match was successful or not, possibly having modified subst regardless
     *
     * implementations may assume that y's .op() already matches this, and that
     * equality has already determined to be false.
     * */
    default boolean match(Compound y, FindSubst subst) {

        //TODO in compiled Compound's for patterns, include
        //# of PATTERN_VAR so that at this point, if
        //# vars of the expected pattern are zero,
        //and since it doesnt equal, there is no match to test

        if (!Ellipsis.hasEllipsis(this)) { //PRECOMPUTABLE
            if (matchCompoundEx(y)) {
                return matchSubterms(y, subst);
            } else {
                return false;
            }
        } else {
            return subst.matchCompoundWithEllipsis(this, y);
        }
    }

    /**
     * default implementation
     *
     * X contains no ellipsis to consider (simple/fast)
     * this implementation can assume that Y has the same size as this, same op.
     * any additional metadata checks are performed in matchCompoundEx() which by default returns true
     */
    default boolean matchSubterms(final Compound Y, final FindSubst subst) {

        int size = Y.size();

        if (size == 1) {
            return matchSubterm(0, Y, subst);
        } else {
            if (isCommutative()) {
                return subst.matchPermute(this, Y); //commutative, try permutations
            } else {
                return matchLinear(Y.subterms(), subst); //non-commutative (must all match), or no permutation necessary (0 or 1 arity)
            }
        }
    }

    default boolean matchLinear(TermContainer y, FindSubst subst) {
        int s = size();
        if (s == 2) {
            //HACK - match smallest (least specific) first
            int v0 = term(0).volume();
            int v1 = term(1).volume();
            if (v0 <= v1) {
                return matchSubterm(0, y, subst)&&
                       matchSubterm(1, y, subst);
            } else {
                return matchSubterm(1, y, subst)&&
                       matchSubterm(0, y, subst);
            }
        } else {
            return subst.matchLinear(this, y, 0, size());
        }
    }

    default boolean matchSubterm(int Xn, final TermContainer Y, final FindSubst subst) {
        return subst.match(term(Xn), Y.term(Xn));
    }

    /** implementatoins may assume that y has:
     *      equal op()
     *
     *  (and that no ellipsis is involved.)
     */
    default boolean matchCompoundEx(Compound y) {
        //final int yStructure = y.structure();

        return
                //this term as a pattern involves anything y does not?
                //((yStructure | structure()) == yStructure) &&
                //&&
                // ?? && ( y.volume() >= volume() )
                //same size
                (size()==y.size());
    }

    default boolean matchCompoundExEllipsis(Compound y, Ellipsis e) {
        //TODO a more careful handling when an Ellipsis term is involved in the match
        return true;
    }

    default Term clone(TermContainer subs) {
        return clone(subs.terms());
    }


//    public int countOccurrences(final Term t) {
//        final AtomicInteger o = new AtomicInteger(0);
//
//        if (equals(t)) return 1;
//
//        recurseTerms((n, p) -> {
//            if (n.equals(t))
//                o.incrementAndGet();
//        });
//
//        return o.apply();
//    }


//    public static class InvalidTermConstruction extends RuntimeException {
//        public InvalidTermConstruction(String reason) {
//            super(reason);
//        }
//    }


//    /**
//     * single term version of makeCompoundName without iteration for efficiency
//     */
//    @Deprecated
//    protected static CharSequence makeCompoundName(final Op op, final Term singleTerm) {
//        int size = 2; // beginning and end parens
//        String opString = op.toString();
//        size += opString.length();
//        final CharSequence tString = singleTerm.toString();
//        size += tString.length();
//        return new StringBuilder(size).append(COMPOUND_TERM_OPENER).append(opString).append(ARGUMENT_SEPARATOR).append(tString).append(COMPOUND_TERM_CLOSER).toString();
//    }

    //    @Deprecated public static class UnableToCloneException extends RuntimeException {
//
//        public UnableToCloneException(String message) {
//            super(message);
//        }
//
//        @Override
//        public synchronized Throwable fillInStackTrace() {
//            /*if (Parameters.DEBUG) {
//                return super.fillInStackTrace();
//            } else {*/
//                //avoid recording stack trace for efficiency reasons
//                return this;
//            //}
//        }
//
//
//    }


}


//    /** performs a deep comparison of the term structure which should have the same result as normal equals(), but slower */
//    @Deprecated public boolean equalsByTerm(final Object that) {
//        if (!(that instanceof CompoundTerm)) return false;
//
//        final CompoundTerm t = (CompoundTerm)that;
//
//        if (operate() != t.operate())
//            return false;
//
//        if (getComplexity()!= t.getComplexity())
//            return false;
//
//        if (getTemporalOrder()!=t.getTemporalOrder())
//            return false;
//
//        if (!equals2(t))
//            return false;
//
//        if (term.length!=t.term.length)
//            return false;
//
//        for (int i = 0; i < term.length; i++) {
//            if (!term[i].equals(t.term[i]))
//                return false;
//        }
//
//        return true;
//    }
//
//
//
//
//    /** additional equality checks, in subclasses, only called by equalsByTerm */
//    @Deprecated public boolean equals2(final CompoundTerm other) {
//        return true;
//    }

//    /** may be overridden in subclass to include other details */
//    protected int calcHash() {
//        //return Objects.hash(operate(), Arrays.hashCode(term), getTemporalOrder());
//        return name().hashCode();
//    }

//
//    /**
//     * Orders among terms: variable < atomic < compound
//     *
//     * @param that The Term to be compared with the current Term
//\     * @return The order of the two terms
//     */
//    @Override
//    public int compareTo(final AbstractTerm that) {
//        if (this == that) return 0;
//
//        if (that instanceof CompoundTerm) {
//            final CompoundTerm t = (CompoundTerm) that;
//            if (size() == t.size()) {
//                int opDiff = this.operate().ordinal() - t.operate().ordinal(); //should be faster faster than Enum.compareTo
//                if (opDiff != 0) {
//                    return opDiff;
//                }
//
//                int tDiff = this.getTemporalOrder() - t.getTemporalOrder(); //should be faster faster than Enum.compareTo
//                if (tDiff != 0) {
//                    return tDiff;
//                }
//
//                for (int i = 0; i < term.length; i++) {
//                    final int diff = term[i].compareTo(t.term[i]);
//                    if (diff != 0) {
//                        return diff;
//                    }
//                }
//
//                return 0;
//            } else {
//                return size() - t.size();
//            }
//        } else {
//            return 1;
//        }
//    }



    /*
    @Override
    public boolean equals(final Object that) {
        return (that instanceof Term) && (compareTo((Term) that) == 0);
    }
    */


//
//
//
//
//    /**
//     * Orders among terms: variable < atomic < compound
//     *
//     * @param that The Term to be compared with the current Term
//\     * @return The order of the two terms
//     */
//    @Override
//    public int compareTo(final Term that) {
//        /*if (!(that instanceof CompoundTerm)) {
//            return getClass().getSimpleName().compareTo(that.getClass().getSimpleName());
//        }
//        */
//        return -name.compareTo(that.name());
//            /*
//            if (size() == t.size()) {
//                int opDiff = this.operate().ordinal() - t.operate().ordinal(); //should be faster faster than Enum.compareTo
//                if (opDiff != 0) {
//                    return opDiff;
//                }
//
//                for (int i = 0; i < term.length; i++) {
//                    final int diff = term[i].compareTo(t.term[i]);
//                    if (diff != 0) {
//                        return diff;
//                    }
//                }
//
//                return 0;
//            } else {
//                return size() - t.size();
//            }
//        } else {
//            return 1;
//            */
//    }



//    @Override
//    public int compareTo(final Object that) {
//        if (that == this) return 0;
//
//        // variables have earlier sorting order than non-variables
//        if (!(that instanceof Compound)) return 1;
//
//        final Compound c = (Compound) that;
//
//        int opdiff = compareClass(this, c);
//        if (opdiff != 0) return opdiff;
//
//        return compare(c);
//    }

//    public static int compareClass(final Object b, final Object c) {
//        Class c1 = b.getClass();
//        Class c2 = c.getClass();
//        int h = Integer.compare(c1.hashCode(), c2.hashCode());
//        if (h != 0) return h;
//        return c1.getName().compareTo(c2.getName());
//    }

//    /**
//     * compares only the contents of the subterms; assume that the other term is of the same operator type
//     */
//    public int compareSubterms(final Compound otherCompoundOfEqualType) {
//        return Terms.compareSubterms(term, otherCompoundOfEqualType.term);
//    }


//    final static int maxSubTermsForNameCompare = 2; //tunable
//
//    protected int compare(final Compound otherCompoundOfEqualType) {
//
//        int l = length();
//
//        if ((l != otherCompoundOfEqualType.length()) || (l < maxSubTermsForNameCompare))
//            return compareSubterms(otherCompoundOfEqualType);
//
//        return compareName(otherCompoundOfEqualType);
//    }
//
//
//    public int compareName(final Compound c) {
//        return super.compareTo(c);
//    }

//    public final void recurseSubtermsContainingVariables(final TermVisitor v, Term parent) {
//        if (hasVar()) {
//            v.visit(this, parent);
//            //if (this instanceof Compound) {
//            for (Term t : term) {
//                t.recurseSubtermsContainingVariables(v, this);
//            }
//            //}
//        }
//    }

//    @Override
//    public boolean equals(final Object that) {
//        if (this == that)
//            return true;
//
//        if (!(that instanceof Compound)) return false;
//        Compound c = (Compound) that;
//        if (contentHash != c.contentHash ||
//                structureHash != c.structureHash ||
//                volume != c.volume)
//            return false;
//
//        final int s = this.length();
//        Term[] x = this.term;
//        Term[] y = c.term;
//        if (x != y) {
//            boolean canShare =
//                    (structureHash &
//                    ((1 << Op.SEQUENCE.ordinal()) | (1 << Op.PARALLEL.ordinal()))) == 0;
//
//            for (int i = 0; i < s; i++) {
//                Term a = x[i];
//                Term b = y[i];
//                if (!a.equals(b))
//                    return false;
//            }
//            if (canShare) {
//                this.term = (T[]) c.term;
//            }
//            else {
//                this.term = this.term;
//            }
//        }
//
//        if (structure2() != c.structure2() ||
//                op() != c.op())
//            return false;
//
//        return true;
//    }

//    @Override
//    public boolean equals(final Object that) {
//        if (this == that)
//            return true;
//        if (!(that instanceof Compound)) return false;
//
//        Compound c = (Compound) that;
//        if (contentHash != c.contentHash ||
//                structureHash != c.structureHash
//                || volume() != c.volume()
//                )
//            return false;
//
//        final int s = this.length();
//        Term[] x = this.term;
//        Term[] y = c.term;
//        for (int i = 0; i < s; i++) {
//            Term a = x[i];
//            Term b = y[i];
//            if (!a.equals(b))
//                return false;
//        }
//
//        return true;
//    }

    /* UNTESTED
    public Compound clone(VariableTransform t) {
        if (!hasVar())
            throw new RuntimeException("this VariableTransform clone should not have been necessary");

        Compound result = cloneVariablesDeep();
        if (result == null)
            throw new RuntimeException("unable to clone: " + this);

        result.transformVariableTermsDeep(t);

        result.invalidate();

        return result;
    } */




//    /**
//     * true if equal operate and all terms contained
//     */
//    public boolean containsAllTermsOf(final Term t) {
//        if ((op() == t.op())) {
//            return Terms.containsAll(term, ((Compound) t).term);
//        } else {
//            return this.containsTerm(t);
//        }
//    }

//    /**
//     * Try to add a component into a compound
//     *
//     * @param t1 The compound
//     * @param t2 The component
//     * @param memory Reference to the memory
//     * @return The new compound
//     */
//    public static Term addComponents(final CompoundTerm t1, final Term t2, final Memory memory) {
//        if (t2 == null)
//            return t1;
//
//        boolean success;
//        Term[] terms;
//        if (t2 instanceof CompoundTerm) {
//            terms = t1.cloneTerms(((CompoundTerm) t2).term);
//        } else {
//            terms = t1.cloneTerms(t2);
//        }
//        return Memory.make(t1, terms, memory);
//    }



//    /**
//     * Recursively check if a compound contains a term
//     * This method DOES check the equality of this term itself.
//     * Although that is how Term.containsTerm operates
//     *
//     * @param target The term to be searched
//     * @return Whether the target is in the current term
//     */
//    @Override
//    public boolean equalsOrContainsTermRecursively(final Term target) {
//        if (this.equals(target)) return true;
//        return containsTermRecursively(target);
//    }

/**
 * override in subclasses to avoid unnecessary reinit
 */
    /*public CompoundTerm _clone(final Term[] replaced) {
        if (Terms.equals(term, replaced)) {
            return this;
        }
        return clone(replaced);
    }*/

//    @Override
//    public int containedTemporalRelations() {
//        if (containedTemporalRelations == -1) {
//
//            /*if ((this instanceof Equivalence) || (this instanceof Implication))*/
//            {
//                int temporalOrder = this.getTemporalOrder();
//                switch (temporalOrder) {
//                    case TemporalRules.ORDER_FORWARD:
//                    case TemporalRules.ORDER_CONCURRENT:
//                    case TemporalRules.ORDER_BACKWARD:
//                        containedTemporalRelations = 1;
//                        break;
//                    default:
//                        containedTemporalRelations = 0;
//                        break;
//                }
//            }
//
//            for (final Term t : term)
//                containedTemporalRelations += t.containedTemporalRelations();
//        }
//        return this.containedTemporalRelations;
//    }



//    /**
//     * Gives a set of all (unique) contained term, recursively
//     */
//    public Set<Term> getContainedTerms() {
//        Set<Term> s = Global.newHashSet(complexity());
//        for (Term t : term) {
//            s.add(t);
//            if (t instanceof Compound)
//                s.addAll(((Compound) t).getContainedTerms());
//        }
//        return s;
//    }






//    /**
//     * forced deep clone of terms
//     */
//    public ArrayList<Term> cloneTermsListDeep() {
//        ArrayList<Term> l = new ArrayList(length());
//        for (final Term t : term)
//            l.add(t.clone());
//        return l;
//    }



    /*static void shuffle(final Term[] list, final Random randomNumber) {
        if (list.length < 2)  {
            return;
        }


        int n = list.length;
        for (int i = 0; i < n; i++) {
            // between i and n-1
            int r = i + (randomNumber.nextInt() % (n-i));
            Term tmp = list[i];    // swap
            list[i] = list[r];
            list[r] = tmp;
        }
    }*/

/*        public static void shuffle(final Term[] ar,final Random rnd)
        {
            if (ar.length < 2)
                return;



          for (int i = ar.length - 1; i > 0; i--)
          {
            int index = randomNumber.nextInt(i + 1);
            // Simple swap
            Term a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
          }

        }*/

/**
 * Check whether the compound contains a certain component
 * Also matches variables, ex: (&&,<a --> b>,<b --> c>) also contains <a --> #1>
 *  ^^^ is this right? if so then try containsVariablesAsWildcard
 *
 * @param t The component to be checked
 * @return Whether the component is in the compound
 */
//return Terms.containsVariablesAsWildcard(term, t);
//^^ ???

//    /**
//     * Try to replace a component in a compound at a given index by another one
//     *
//     * @param index   The location of replacement
//     * @param subterm The new component
//     * @return The new compound
//     */
//    public Term cloneReplacingSubterm(final int index, final Term subterm) {
//
//        final boolean e = (subterm != null) && (op() == subterm.op());
//
//        //if the subterm is alredy equivalent, just return this instance because it will be equivalent
//        if (subterm != null && (e) && (term[index].equals(subterm)))
//            return this;
//
//        List<Term> list = asTermList();//Deep();
//
//        list.remove(index);
//
//        if (subterm != null) {
//            if (!e) {
//                list.add(index, subterm);
//            } else {
//                //splice in subterm's subterms at index
//                for (final Term t : term) {
//                    list.add(t);
//                }
//
//                /*Term[] tt = ((Compound) subterm).term;
//                for (int i = 0; i < tt.length; i++) {
//                    list.add(index + i, tt[i]);
//                }*/
//            }
//        }
//
//        return Memory.term(this, list);
//    }


//    /**
//     * Check whether the compound contains all term of another term, or
//     * that term as a whole
//     *
//     * @param t The other term
//     * @return Whether the term are all in the compound
//     */
//    public boolean containsAllTermsOf_(final Term t) {
//        if (t instanceof CompoundTerm) {
//        //if (operate() == t.operate()) {
//            //TODO make unit test for containsAll
//            return Terms.containsAll(term, ((CompoundTerm) t).term );
//        } else {
//            return Terms.contains(term, t);
//        }
//    }

