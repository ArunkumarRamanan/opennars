/*
 * Product.java
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

package nars.nal.nal4;

import nars.Op;
import nars.term.Atom;
import nars.term.Compound;
import nars.term.Term;

import java.util.Collection;
import java.util.List;

/**
 * A Product is a sequence of 1 or more terms.
 */
public interface Product<T extends Term> extends Term, Iterable<T> {

    Product empty = new ProductN();

    /**
     * Get the operate of the term.
     * @return the operate of the term
     */
    @Override
    default Op op() {
        return Op.PRODUCT;
    }

    /**
     * Try to make a Product from an ImageExt/ImageInt and a component. Called by the logic rules.
     * @param image The existing Image
     * @param component The component to be added into the component list
     * @param index The index of the place-holder in the new Image -- optional parameter
     * @return A compound generated or a term it reduced to
     */
    static Term make(final Compound image, final Term component, final int index) {
        Term[] argument = image.cloneTerms();
        argument[index] = component;
        return make(argument);
    }

    static Product make(final Term[] pre, final Term... suf) {
        final int pLen = pre.length;
        final int sLen = suf.length;
        Term[] x = new Term[pLen + suf.length];
        System.arraycopy(pre, 0, x, 0, pLen);
        System.arraycopy(suf, 0, x, pLen, sLen);
        return make(x);
    }

    static Product make(final Collection<Term> t) {
        return make(t.toArray(new Term[t.size()]));
    }
//    static Product makeFromIterable(final Iterable<Term> t) {
//        return make(Iterables.toArray(t, Term.class));
//    }

    static Product only(final Term the) {
        return new Product1(the);
    }

    /** 2 term constructor */
    static Product make(final Term a, final Term b) {
        return new ProductN(a, b);
    }

    /** creates from a sublist of a list */
    static Product make(final List<Term> l, int from, int to) {
        Term[] x = new Term[to - from];

        for (int j = 0, i = from; i < to; i++)
            x[j++] = l.get(i);

        return make(x);
    }

    static Product make(final Term... arg) {
        if (arg.length == 1)
            return only(arg[0]);

        return new ProductN(arg);
    }

    static Product make(final String... argAtoms) {
        return Product.make( Atom.the(argAtoms) );
    }

//    Term[] cloneTermsReplacing(final Term from, final Term to);

    Term[] cloneTerms();

    Term term(int i);

    Term[] terms();


    default Object first() {
        return term(0);
    }


    /** apply Atom.quoteI */
    static Product arrayToStringAtomProduct(final Object[] args) {
        if (args.length == 0) return Product.empty;
        Term[] x = new Term[args.length];
        for (int i = 0; i < args.length; i++)
            x[i] = Atom.quote(args[i].toString() );
        return Product.make(x);
    }
}
