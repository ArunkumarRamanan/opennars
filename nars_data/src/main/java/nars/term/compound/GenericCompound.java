package nars.term.compound;

import com.gs.collections.api.block.predicate.primitive.IntObjectPredicate;
import nars.Op;
import nars.term.*;
import nars.term.op.Operator;
import nars.util.utf8.ByteBuf;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Symbols.COMPOUND_TERM_CLOSERbyte;


public class GenericCompound<T extends Term> implements Compound<T> {

    public final TermVector<T> terms;
    public final Op op;
    public final int relation;

    protected final transient int hash;
    private transient boolean normalized = false;


    protected GenericCompound(Op op, T... subterms) {
        this(op, subterms, 0);
    }

    public GenericCompound(Op op, T[] subterms, int relation) {

        this.op = op;

        TermVector<T> terms = this.terms = op.isCommutative() ?
                TermSet.newTermSetPresorted(subterms) :
                new TermVector(subterms);
        this.hash = Compound.hash(terms, op, relation+1);
        this.relation = relation;
    }

    protected GenericCompound(Op op, TermVector subterms, int relation) {
        this.op = op;
        this.terms = subterms;
        this.relation = relation;
        this.hash = Compound.hash(terms, op, relation+1);
    }

    @Override
    public final Op op() {
        return op;
    }

    @Override
    public final boolean isCommutative() {
        return op.isCommutative();
    }

    public void append(Appendable p, boolean pretty) throws IOException {

        switch (op) {
            case SET_INT_OPENER:
            case SET_EXT_OPENER:
                Compounds.setAppend(this, p, pretty);
                break;
            case PRODUCT:
                Compounds.productAppend(this, p, pretty);
                break;
            case IMAGE_INT:
            case IMAGE_EXT:
                Compounds.imageAppend(this, p, pretty);
                break;
            default:
                if (op.isStatement()) {
                    if (Op.isOperation(this)) {
                        Operator.operationAppend((Compound) term(0), (Operator) term(1), p, pretty); //TODO Appender
                    }
                    else {
                        Statement.append(this, p, pretty);
                    }
                } else {
                    Compound.appendCompound(this, p, pretty);
                }
                break;
        }



    }


    @Override
    public String toStringCompact() {
        return toString(false);
    }



    @Override
    public final int compareTo(Object o) {
        if (this == o) return 0;

        Term t = (Term) o;
        //int diff = op().compareTo(t.op());
        int diff = Integer.compare(op().ordinal(), t.op().ordinal());
        if (diff != 0) return diff;

        Compound c = (Compound)t;
        diff = Integer.compare(relation(), c.relation());
        if (diff != 0) return diff;

        return subterms().compareTo( c.subterms() );
    }


    @Override
    public final void addAllTo(Collection<Term> set) {
        terms.addAllTo(set);
    }

    @Override
    public Term clone(Term[] replaced) {
        return Compounds.the(op(), replaced, relation);
    }

    @Override
    public final TermVector<T> subterms() {
        return terms;
    }

    @Override
    public final boolean equals(Object that) {
        if (this == that)
            return true;

        if (!(that instanceof Compound)) {
            return false;
        }

        Compound c = (Compound)that;
        return
            (hash == c.hashCode()) &&
            (terms.equals(c.subterms())) &&
            (op == c.op()) &&
            (relation == c.relation());

    }


    /**
     * recursively set duration to interval subterms
     */
    @Override
    public void setDuration(int duration) {
        if (TermMetadata.hasTemporals(this)) {
            int n = size();
            for (int i = 0; i < n; i++)
                term(i).setDuration(duration);
        }
    }


    @Override
    public final int hashCode() {
        return hash;
    }


    @Override
    public final int varDep() {
        return terms.varDep();
    }

    @Override
    public final int varIndep() {
        return terms.varIndep();
    }

    @Override
    public final int varQuery() {
        return terms.varQuery();
    }

    @Override
    public final int vars() {
        return terms.vars();
    }

//    public final Term[] cloneTermsReplacing(int index, Term replaced) {
//        return terms.cloneTermsReplacing(index, replaced);
//    }

    public final boolean isEmpty() {
        return terms.isEmpty();
    }

    public final boolean contains(Object o) {
        return terms.contains(o);
    }


    @Override
    public final void forEach(Consumer<? super T> action, int start, int stop) {
        terms.forEach(action, start, stop);
    }

    @Override
    public final void forEach(Consumer<? super T> c) {
        terms.forEach(c);
    }

    @Override public T[] terms() {
        return terms.term;
    }

    @Override
    public final Term[] terms(IntObjectPredicate<T> filter) {
        return terms.terms(filter);
    }

    @Override
    public final Iterator<T> iterator() {
        return terms.iterator();
    }

    @Override
    public final  T[] termsCopy() {
        return terms.termsCopy();
    }

    @Override
    public final int structure() {
        return terms.structure() | (1 << op.ordinal());
    }

    @Override
    public final T term(int i) {
        return terms.term(i);
    }

    @Override
    public Term termOr(int index, Term resultIfInvalidIndex) {
        return terms.termOr(index, resultIfInvalidIndex);
    }

    @Override
    public final boolean containsTerm(Term target) {
        return terms.containsTerm(target);
    }

    @Override
    public final int size() {
        return terms.size();
    }

    @Override
    public final int complexity() {
        return terms.complexity();
    }

    @Override
    public final int volume() {
        return terms.volume();
    }

    @Override
    public final boolean impossibleSubTermVolume(int otherTermVolume) {
        return terms.impossibleSubTermVolume(otherTermVolume);
    }

    /**
     * searches for a subterm
     * TODO parameter for max (int) level to scan down
     */
    @Override
    public final boolean containsTermRecursively(Term target) {

        if (impossibleSubterm(target)) return false;

        return terms.containsTermRecursively(target);
    }



    @Override public final boolean isNormalized() {
        return normalized;
    }


    @Override
    public final void setNormalized(boolean b) {
        normalized = b;
    }

    @Override
    public int bytesLength() {
        int len = /* opener byte */1 + (op.isImage() ? 1 : 0);

        int n = size();
        for (int i = 0; i < n; i++) {
            len += term(i).bytesLength() + 1 /* separator or closer if end*/;
        }

        return len;
    }

    @Override
    public String toString() {
        return toString(false); //TODO make this default to false
    }

    @Override
    public int relation() {
        return relation;
    }

    @Override
    public final byte[] bytes() {

        ByteBuf b = ByteBuf.create(bytesLength());

        b.add((byte) op().ordinal()); //header

        if (op().isImage()) {
            b.add((byte) relation); //header
        }

        appendSubtermBytes(b);

        if (op().maxSize != 1) {
            b.add(COMPOUND_TERM_CLOSERbyte); //closer
        }

        return b.toBytes();
    }


    @Override
    public void appendSubtermBytes(ByteBuf b) {
        terms.appendSubtermBytes(b);
    }

    @Override
    public final boolean and(Predicate<Term> v) {
        return v.test(this) && terms.and(v);
    }
    @Override
    public final boolean or(Predicate<Term> v) {
        return v.test(this) || terms.or(v);
    }




}
