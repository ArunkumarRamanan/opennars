package nars.logic;

import nars.core.Memory;
import nars.core.Parameters;
import nars.io.Symbols;
import nars.logic.entity.*;
import nars.logic.nal1.Inheritance;
import nars.logic.nal1.Negation;
import nars.logic.nal2.Similarity;
import nars.logic.nal3.Difference;
import nars.logic.nal3.Intersect;
import nars.logic.nal4.Image;
import nars.logic.nal4.ImageExt;
import nars.logic.nal4.ImageInt;
import nars.logic.nal4.Product;
import nars.logic.nal5.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Static utility class for static methods related to Terms
 * @author me
 */
public class Terms {

    public static boolean equalSubTermsInRespectToImageAndProduct(final Term a, final Term b) {
        if (a == null || b == null) {
            return false;
        }
        if (!((a instanceof CompoundTerm) && (b instanceof CompoundTerm))) {
            return a.equals(b);
        }
        if (a instanceof Inheritance && b instanceof Inheritance) {
            return equalSubjectPredicateInRespectToImageAndProduct(a, b);
        }
        if (a instanceof Similarity && b instanceof Similarity) {
            return equalSubjectPredicateInRespectToImageAndProduct(a, b) || equalSubjectPredicateInRespectToImageAndProduct(b, a);
        }
        Term[] A = ((CompoundTerm) a).term;
        Term[] B = ((CompoundTerm) b).term;
        if (A.length != B.length)
            return false;

        for (int i = 0; i < A.length; i++) {
            Term x = A[i];
            Term y = B[i];
            if (!x.equals(y)) {
                if (x instanceof Inheritance && y instanceof Inheritance) {
                    if (!equalSubjectPredicateInRespectToImageAndProduct(x, y)) {
                        return false;
                    } else {
                        continue;
                    }
                }
                if (x instanceof Similarity && y instanceof Similarity) {
                    if (!equalSubjectPredicateInRespectToImageAndProduct(x, y) && !equalSubjectPredicateInRespectToImageAndProduct(y, x)) {
                        return false;
                    } else {
                        continue;
                    }
                }
                return false;
            }
        }
        return true;
    }

    public static Term reduceUntilLayer2(final CompoundTerm _itself, Term replacement, Memory memory) {
        if (_itself == null)
            return null;
        
        Term reduced = reduceComponentOneLayer(_itself, replacement, memory);
        if (!(reduced instanceof CompoundTerm))
            return null;
        
        CompoundTerm itself = (CompoundTerm)reduced;
        int j = 0;
        for (Term t : itself.term) {
            Term t2 = unwrapNegation(t);
            if (!(t2 instanceof Implication) && !(t2 instanceof Equivalence) && !(t2 instanceof Conjunction) && !(t2 instanceof Disjunction)) {
                j++;
                continue;
            }
            Term ret2 = reduceComponentOneLayer((CompoundTerm) t2, replacement, memory);
            
            //CompoundTerm itselfCompound = itself;
            Term replaced = null;
            if (j < itself.term.length  )
                replaced = itself.setComponent(j, ret2, memory);
            
            if (replaced != null) {
                if (replaced instanceof CompoundTerm)
                    itself = (CompoundTerm)replaced;
                else
                    return replaced;
            }
            j++;
        }
        return itself;
    }
    /*
    @Deprecated public static Term make(final String op, final ArrayList<Term> arg, final Memory memory) {
    final int length = op.length();
    if (length == 1) {
    final char c = op.charAt(0);
    switch (c) {
    case Symbols.SET_EXT_OPENER:
    return SetExt.make(arg, memory);
    case Symbols.SET_INT_OPENER:
    return SetInt.make(arg, memory);
    case Symbols.INTERSECTION_EXT_OPERATORc:
    return IntersectionExt.make(arg, memory);
    case Symbols.INTERSECTION_INT_OPERATORc:
    return IntersectionInt.make(arg, memory);
    case Symbols.DIFFERENCE_EXT_OPERATORc:
    return DifferenceExt.make(arg, memory);
    case Symbols.DIFFERENCE_INT_OPERATORc:
    return DifferenceInt.make(arg, memory);
    case Symbols.PRODUCT_OPERATORc:
    return Product.make(arg, memory);
    case Symbols.IMAGE_EXT_OPERATORc:
    return ImageExt.make(arg, memory);
    case Symbols.IMAGE_INT_OPERATORc:
    return ImageInt.make(arg, memory);
    }
    }
    else if (length == 2) {
    //since these symbols are the same character repeated, we only need to compare the first character
    final char c1 = op.charAt(0);
    final char c2 = op.charAt(1);
    if (c1 == c2) {
    switch (c1) {
    case Symbols.NEGATION_OPERATORc:
    return Negation.make(arg, memory);
    case Symbols.DISJUNCTION_OPERATORc:
    return Disjunction.make(arg, memory);
    case Symbols.CONJUNCTION_OPERATORc:
    return Conjunction.make(arg, memory);
    }
    } else if (op.equals(Symbols.SEQUENCE_OPERATOR)) {
    return Conjunction.make(arg, TemporalRules.ORDER_FORWARD, memory);
    } else if (op.equals(Symbols.PARALLEL_OPERATOR)) {
    return Conjunction.make(arg, TemporalRules.ORDER_CONCURRENT, memory);
    }
    }
    throw new RuntimeException("Unknown Term operator: " + op);
    }
     */

    /**
     * Try to remove a component from a compound
     *
     * @param t1 The compound
     * @param t2 The component
     * @param memory Reference to the memory
     * @return The new compound
     */
    public static Term reduceComponents(final CompoundTerm t1, final Term t2, final Memory memory) {
        final Term[] list;
        if (t1.operator() == t2.operator()) {
            list = t1.cloneTermsExcept(true, ((CompoundTerm) t2).term);
        } else {
            list = t1.cloneTermsExcept(true, new Term[] { t2 });
        }
        if (list != null) {
            if (list.length > 1) {
                return memory.term(t1, list);
            }
            if (list.length == 1) {
                if ((t1 instanceof Junction) || (t1 instanceof Intersect) || (t1 instanceof Difference)) {
                    return list[0];
                }
            }
        }
        return null;
    }

    public static Term reduceComponentOneLayer(CompoundTerm t1, Term t2, Memory memory) {
        Term[] list;
        if (t1.operator() == t2.operator()) {
            list = t1.cloneTermsExcept(true, ((CompoundTerm) t2).term);
        } else {
            list = t1.cloneTermsExcept(true, new Term[] { t2 });
        }
        if (list != null) {
            if (list.length > 1) {
                return memory.term(t1, list);
            } else if (list.length == 1) {
                return list[0];
            }
        }
        return t1;
    }


    public static Term unwrapNegation(final Term T) {
        if (T != null && T instanceof Negation) {
            return ((CompoundTerm) T).term[0];
        }
        return T;
    }

    public static boolean equalSubjectPredicateInRespectToImageAndProduct(final Term a, final Term b) {
        return equalSubjectPredicateInRespectToImageAndProduct(a, b, true);
    }

    public static boolean equalSubjectPredicateInRespectToImageAndProduct(final Term a, final Term b, boolean requireEqualImageRelation) {
        
        if (a == null || b == null) {
            return false;
        }
        
        if (!(a instanceof Statement) || !(b instanceof Statement)) {
            return false;
        }
        
        if (a.equals(b)) {
            return true;
        }
        
        Statement A = (Statement) a;
        Statement B = (Statement) b;
        
        /*
        //REMOVED this prevents the non-statement cases further down
            "if ((predA instanceof Product) && (subjB instanceof ImageInt))"
            "if ((predB instanceof Product) && (subjA instanceof ImageInt))"
                --both will never occur since this test prevents them
        if (!((A instanceof Similarity && B instanceof Similarity)
                || (A instanceof Inheritance && B instanceof Inheritance)))
            return false;*/
            
        Term subjA = A.getSubject();
        Term predA = A.getPredicate();
        Term subjB = B.getSubject();
        Term predB = B.getPredicate();

        Term ta = null, tb = null; //the compound term to put itself in the comparison set
        Term sa = null, sb = null; //the compound term to put its components in the comparison set

        if ((subjA instanceof Product) && (predB instanceof ImageExt)) {
            ta = predA; sa = subjA;
            tb = subjB; sb = predB;
        }
        if ((subjB instanceof Product) && (predA instanceof ImageExt)) {
            ta = subjA; sa = predA;
            tb = predB; sb = subjB;                
        }
        if ((predA instanceof ImageExt) && (predB instanceof ImageExt)) {
            ta = subjA; sa = predA;
            tb = subjB; sb = predB;                
        }

        if ((subjA instanceof ImageInt) && (subjB instanceof ImageInt)) {
            ta = predA; sa = subjA;
            tb = predB; sb = subjB;
        }

        if ((predA instanceof Product) && (subjB instanceof ImageInt)) {
            ta = subjA; sa = predA;
            tb = predB; sb = subjB;                
        }
        if ((predB instanceof Product) && (subjA instanceof ImageInt)) {
            ta = predA; sa = subjA;
            tb = subjB; sb = predB;
        }

        if (ta==null)
            return false;

        Term[] sat = ((CompoundTerm)sa).term;
        Term[] sbt = ((CompoundTerm)sb).term;

        //original code did not check relation index equality
        //https://code.google.com/p/open-nars/source/browse/trunk/nars_core_java/nars/language/CompoundTerm.java
        if (requireEqualImageRelation) {
            if (sa instanceof Image && sb instanceof Image) {
                if(((Image) sa).relationIndex != ((Image) sb).relationIndex) {
                    return false;
                }
            }
        }

        return containsAll(sat, ta, sbt, tb);

        /*
        for(Term sA : componentsA) {
            boolean had=false;
            for(Term sB : componentsB) {
                if(sA instanceof Variable && sB instanceof Variable) {
                    if(sA.name().equals(sB.name())) {
                        had=true;
                    }
                }
                else if(sA.equals(sB)) {
                    had=true;
                }
            }
            if(!had) {
                return false;
            }
        }
        */
    }

    private static boolean containsAll(Term[] sat, Term ta, Term[] sbt, Term tb) {
        //temporary set for fast containment check
        Set<Term> componentsA = Parameters.newHashSet(sat.length + 1);
        componentsA.add(ta);
        Collections.addAll(componentsA, sat);

        //test A contains B
        if (!componentsA.contains(tb))
            return false;
        for (Term bComponent : sbt)
            if (!componentsA.contains(bComponent))
                return false;

        return true;
    }

    /**
     * Collect TermLink templates into a list, go down one level except in
     * special cases
     * <p>
     *
     * @param componentLinks The list of TermLink templates built so far
     * @param type The type of TermLink to be built
     * @param term The CompoundTerm for which the links are built
     */
    public static void prepareComponentLinks(TermLink.TermLinkBuilder tlb, final short type, final CompoundTerm t) {
        
        boolean tEquivalence = (t instanceof Equivalence);
        boolean tImplication = (t instanceof Implication);


        //componentLinks.ensureCapacity(componentLinks.size() + t.complexity);

        for (short i = 0; i < t.term.length; ) {
            final Term ti = t.term[i];

            if (!ti.hasVar()) {
                tlb.addTemplate(new TermLinkTemplate(type, ti, i));
            }

            if ((tEquivalence || (tImplication && (i == 0))) && ((ti instanceof Conjunction) || (ti instanceof Negation))) {

                prepareComponentLinks(tlb, TermLink.COMPOUND_CONDITION, (CompoundTerm) ti);

            } else if (ti instanceof CompoundTerm) {
                final CompoundTerm cti = (CompoundTerm)ti;

                boolean t1ProductOrImage = (ti instanceof Product) || (ti instanceof Image);

                final short tiSize = (short)cti.term.length;
                for (short j = 0; j < tiSize; ) {
                    Term tj = cti.term[j];

                    if (!tj.hasVar()) {
                        TermLinkTemplate a;
                        if (t1ProductOrImage) {
                            if (type == TermLink.COMPOUND_CONDITION) {
                                a = new TermLinkTemplate(TermLink.TRANSFORM, tj, 0, i, j);
                            } else {
                                a = new TermLinkTemplate(TermLink.TRANSFORM, tj, i, j);
                            }
                        } else {
                            a = new TermLinkTemplate(type, tj, i, j);
                        }
                        tlb.addTemplate(a);
                    }

                    if ((tj instanceof Product) || (tj instanceof Image)) {
                        CompoundTerm ctj = (CompoundTerm)tj;

                        final short tjSize = (short) ctj.term.length;
                        for (short k = 0; k < tjSize; ) {
                            final Term tk = ctj.term[k];
                            
                            if (!tk.hasVar()) {
                                TermLinkTemplate b;
                                if (type == TermLink.COMPOUND_CONDITION) {
                                    b = new TermLinkTemplate(TermLink.TRANSFORM, tk, 0, i, j, k);
                                } else {
                                    b = new TermLinkTemplate(TermLink.TRANSFORM, tk, i, j, k);
                                }
                                tlb.addTemplate(b);
                            }

                            k++; //increment at end in case it's the last iteration we want to use max n-1, not n
                        }
                    }

                    j++; //increment at end in case it's the last iteration we want to use max n-1, not n
                }
            }

            i++; //increment at end in case it's the last iteration we want to use max n-1, not n
        }
    }

   public static void prepareComponentLinks(TermLink.TermLinkBuilder t, CompoundTerm ct) {
        short type = (ct instanceof Statement) ? TermLink.COMPOUND_STATEMENT : TermLink.COMPOUND;   // default
        prepareComponentLinks(t, type, ct);
    }

    //TODO move this to a utility method
    public static <T> int indexOf(final T[] array, final T v) {
        int i = 0;
        for (final T e : array) {
            if (v.equals(e)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /** compres a set of terms (assumed to be unique) with another set to find if their
     * contents match. they can be in different order and still match.  this is useful for
     * comparing whether compound terms in which order doesn't matter (ex: conjunction)
     * are equivalent.
     */ 
    public static <T> boolean containsAll(final T[] container, final T[] content) {
        if (container.length < content.length) {
            return false;
        }
        for (final T x : content) {
            if (!contains(container, x))
                return false;
        }
        return true;
    }
    
    /** a contains any of b  NOT TESTED YET */
    public static boolean containsAny(final Term[] a, final Collection<Term> b) {
        for (final Term bx : b) {
            if (contains(a, bx))
                return true;
        }
        for (final Term ax : a) {
            if (ax instanceof CompoundTerm)
                if (containsAny(((CompoundTerm)ax).term, b))
                    return true;
        }
        
        return false;
    }

    public static <T> boolean contains(final T[] container, final T v) {
        for (final T e : container) {
            if (v.equals(e)) {
                return true;
            }
        }
        return false;
    }

    public static boolean equals(final Term[] a, final Term[] b) {
        if (a.length!=b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(b[i]))
                return false;            
        }
        return true;
    }

    public static void verifyNonNull(Collection t) {
        for (Object o : t)
            if (o == null)
                throw new RuntimeException("Element null in: " + t);
    }

    public static void verifyNonNull(Term... t) {
        for (Object o : t)
            if (o == null)
                throw new RuntimeException("Element null in: " + Arrays.toString(t));
    }    
    
    public static Term[] verifySortedAndUnique(final Term[] arg, boolean allowSingleton) {        
        if (arg.length == 0) {
            throw new RuntimeException("Needs >0 components");
        }
        if (!allowSingleton && (arg.length == 1)) {
            throw new RuntimeException("Needs >1 components: " + Arrays.toString(arg));
        }
        Term[] s = Term.toSortedSetArray(arg);
        if (arg.length!=s.length) {
            throw new RuntimeException("Contains duplicates: " + Arrays.toString(arg));
        }
        int j = 0;
        for (Term t : s) {
            if (!t.equals(arg[j++]))
                throw new RuntimeException("Un-ordered: " + Arrays.toString(arg) + " , correct order=" + Arrays.toString(s));
        }        
        return s;
    }

    /**
     * comparison that will match constant terms, allowing variables to match regardless
     * ex: (&&,<a --> b>,<b --> c>) also contains <a --> #1>
     */
    public static boolean containsVariablesAsWildcard(final Term[] term, final Term b) {
        CompoundTerm bCompound = (b instanceof CompoundTerm) ? ((CompoundTerm)b) : null;
        for (Term a : term) {
            if (a.equals(b)) return true;
            
            if ((a instanceof CompoundTerm) && (bCompound!=null))  {
                if (((CompoundTerm)a).equalsVariablesAsWildcards(bCompound))
                        return true;
            }
        }
        return false;
    }

    
    /** true if any of the terms contains a variable */
    public static boolean containsVariables(Term... args) {
        for (Term t : args) {
            if (t.hasVar())
                return true;
        }
        return false;
    }

    public static boolean levelValid(Term t, int nal) {
        Symbols.NativeOperator o = t.operator();
        int minLevel = o.level;
        if (minLevel > 0) {
            if (nal < minLevel)
                return false;
        }
        if (t instanceof CompoundTerm) {
            for (Term sub : ((CompoundTerm)t).term) {
                if (!levelValid(sub, nal))
                    return false;
            }
        }
        return true;
    }

    public static boolean levelValid(Sentence sentence, int nal) {
        if (nal >= 8) return true;

        Term t = sentence.getTerm();
        if (!sentence.isEternal() && nal < 7) return false;
        return levelValid(t, nal);
    }


    /** has, or is associated with a specific term */
    public static interface Termable<TT extends Term> {
        public TT getTerm();
    }
    
}
