package nars.util.version;

import nars.Global;
import org.jgrapht.util.ArrayUnenforcedSet;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;


public final class VersionMap<X,Y> extends AbstractMap<X, Y>  {

    private final Versioning context;
    private final Map<X, Versioned<Y>> map;

    public VersionMap(Versioning context) {
        this(context,
                //new LinkedHashMap<>()
                Global.newHashMap()
        );
    }

    public VersionMap(Versioning context, Map<X, Versioned<Y>/*<Y>*/> map) {
        super();
        this.context = context;
        this.map = map;
    }

    @Override
    public final boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public Set<X> keySet() {
        return map.keySet();
    }

    //    @Override
//    public final void forEach(BiConsumer<? super X, ? super Y> action) {
//        map.forEach((BiConsumer<? super X, ? super Versioned<Y>>) action);
//    }

    @Override
    public Y remove(Object key) {
        Versioned<Y> x = map.remove(key);
        if (x == null)
            return null;

        Y value = x.get();
        x.delete();
        return value;
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new RuntimeException("unimpl yet");
    }

    @Override
    public void clear() {
        throw new RuntimeException("unimpl yet");
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

//    @Override
//    public final void putAll(Map<? extends X, ? extends Y> m) {
//        m.forEach(this::put);
//    }

    /** avoid using this if possible because it involves transforming the entries from the internal map to the external form */
    @Override public Set<Entry<X, Y>> entrySet() {
        ArrayUnenforcedSet<Entry<X,Y>> e = new ArrayUnenforcedSet<>(size());
        map.forEach( (k, v) -> {
            Y vv = v.get();
            e.add(new AbstractMap.SimpleEntry<>(k, vv));
        });
        return e;
    }

    @Override
    public void putAll(Map<? extends X, ? extends Y> m) {
        if (m instanceof VersionMap) {
            VersionMap<X,Y> o = (VersionMap)m;
            o.map.forEach((k,v) -> put(k, v.get()));
        }
        else {
            //default
            super.putAll(m);
        }
    }

    /**
     * records an assignment operation
     * follows semantics of set()
     */
    public Y put(X key, Y value) {
        getOrCreateIfAbsent(key).set(value);
        return null;
    }

    /** follows semantics of thenSet() */
    public Versioning thenPut(X key, Y value) {
        getOrCreateIfAbsent(key).thenSet(value);
        return context;
    }

    public Versioned getOrCreateIfAbsent(X key) {
        return map.computeIfAbsent(key,
                RemovingVersionedEntry::new);
    }

    /** this implementation removes itself from the map when it is reverted to
     *  times prior to its appearance in the map */
    final class RemovingVersionedEntry extends Versioned<Y> {

        private final X key;

        public RemovingVersionedEntry(X key) {
            super(context);
            this.key = key;
        }

        @Override
        boolean revertNext(int before) {
            boolean v = super.revertNext(before);
            if (size == 0)
                removeFromMap();
            return v;
        }

        public void clear() {
//            super.clear();
//            removeFromMap();
            throw new RuntimeException("what is this supposed to do");
        }

        private final void removeFromMap() {
            VersionMap.this.remove(key);
        }
    }


    public final Y get(/*X*/Object key) {
        Versioned<Y> v = version((X) key);
        if (v!=null) return v.get();
        return null;
    }

    public Y get(X key, Supplier<Y> ifAbsentPut) {
        //TODO use compute... Map methods
        Y o = get(key);
        if (o == null) {
            o = ifAbsentPut.get();
            put(key, o);
        }
        return o;
    }

    public final Versioned<Y> version(X key) {
        return map.computeIfPresent(key, (k, v) -> {
            if (v == null || v.isEmpty())
                return null;
            return v;
        });
    }
}