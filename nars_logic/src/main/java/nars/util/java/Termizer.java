package nars.util.java;

import nars.nal.nal1.Negation;
import nars.term.Atom;
import nars.term.Term;

/**
 * Converts POJOs to NAL Term's, and vice-versa
 */
public interface Termizer {

    Atom TRUE = Atom.the("true");
    Negation FALSE = Atom.notThe("true");
    Atom VOID = Atom.the("void");
    Atom EMPTY = Atom.the("empty");
    Atom NULL = Atom.the("null");

    Term term(Object o);
    Object object(Term t);

}
