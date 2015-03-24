package nars;

/**
 * NAR design parameters which define a NAR at initialization.
 * All the information necessary to create a new NAR (builder / parameter object)
 * These do not change after initialization.
 * For runtime parameters, @see Param
 */
abstract public class ProtoNAR extends Global {

    public final Param param = new Param();

    abstract public Core newCore();

    public ProtoNAR() {
    }
            
    
//    /** initial runtime parameters */
//    public Param getParam() {
//        return param;
//    }
//    
//    public Bag<Task<Term>,Sentence<Term>> getNovelTaskBag() {
//        return novelTaskBag;
//    }
//    
//    public Attention getAttention() {
//        return attention;
//    }
//    
//    public ConceptBuilder getConceptBuilder() {
//        return conceptBuilder;
//    }

    protected Memory newMemory(Param p) {
        return new Memory(getMaximumNALLevel(), p, newCore());
    }

    protected abstract int getMaximumNALLevel();

    /** called after NAR created, for initializing it */
    public void init(NAR nar) {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public String toJSON() {
        return Param.json.toJson(this);
    }
    
}
