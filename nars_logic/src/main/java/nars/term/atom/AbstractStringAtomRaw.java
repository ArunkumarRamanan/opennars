package nars.term.atom;

import nars.Op;
import nars.term.Term;

import java.io.IOException;

/** implemented with a native Java string.
 *  this should be the ideal choice for JDK9
 *  since it does Utf8 internally and many
 *  string operations are intrinsics.  */
public abstract class AbstractStringAtomRaw extends Atomic  {

    public final String id;

    protected AbstractStringAtomRaw(String id) {
        this.id = id;
    }


    @Override
    public int hashCode() {
        /** for Op.ATOM, we use String hashCode() as-is, avoiding need to calculate or store a hash mutated by the Op */
        return id.hashCode();
    }

    @Override
    public abstract Op op();

    @Override
    public abstract int structure();

    @Override
    public void append(Appendable w, boolean pretty) throws IOException {
        w.append(id);
    }

    /** preferably use toCharSequence if needing a CharSequence; it avoids a duplication */
    @Override
    public StringBuilder toStringBuilder(boolean pretty) {
        return new StringBuilder(id);
    }



    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object x) {
        if (this == x) return true;

        if (x instanceof AbstractStringAtomRaw) {
            AbstractStringAtomRaw ax = (AbstractStringAtomRaw) x;
            return id.equals(ax.id) && ax.op() == op();
        }

        return false;
    }

    /**
     * @param that The Term to be compared with the current Term
     */
    @Override
    public int compareTo(Object that) {
        if (that==this) return 0;

        Term t = (Term)that;
        int d = Integer.compare(op().ordinal(), t.op().ordinal());
        if (d!=0) return d;

        //if (that instanceof AbstractStringAtomRaw) {
            //if the op is the same, it will be a subclass of atom
            //which should have an ordering determined by its byte[]
            return id.compareTo(((AbstractStringAtomRaw)that).id);
        //}

    }




}
