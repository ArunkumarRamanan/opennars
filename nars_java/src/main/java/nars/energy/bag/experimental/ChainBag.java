package nars.energy.bag.experimental;

import nars.Memory;
import nars.Global;
import nars.nal.entity.Item;
import nars.energy.Bag;
import nars.energy.BagSelector;
import nars.util.data.CuckooMap;
import nars.util.data.linkedlist.DD;
import nars.util.data.linkedlist.DDList;
import nars.util.data.linkedlist.DDNodePool;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * ChainBag repeatedly cycles through a linked list containing
 * the set of items, stored in an arbitrary order.
 *
 * Probabalistic selection is decided according to a random function
 * of an item's priority, with options for normalizing against
 * the a priority range encountered in a sliding window.
 *
 * This allows it to maximize the dynamic range across the bag's contents
 * regardless of their absolute priority distribution (percentile vs.
 * percentage).
 *
 * Probability can be further weighted by a curve function to
 * fine-tune behavior.
 *
 */
public class ChainBag<V extends Item<K>, K> extends Bag<K, V> {


    private final Mean mean; //priority mean, continuously calculated
    private int capacity;
    private float mass;
    DD<V> current = null;

    public Frequency removal = new Frequency();

    double minMaxMomentum = 0.98;

    private final DDNodePool<V> nodePool = new DDNodePool(16);

    V nextRemoval = null;

    /**
     * mapping from key to item
     */
    public final Map<K, DD<V>> index;

    /**
     * array of lists of items, for items on different level
     */
    public final DDList<V> chain;

    private float PERCENTILE_THRESHOLD_FOR_EMERGENCY_REMOVAL = 0.5f;
    private double estimatedMax = 0.5f;
    private double estimatedMin = 0.5f;
    private double estimatedMean;


    public ChainBag(int capacity) {
        super();

        this.capacity = capacity;
        this.mass = 0;
        this.index = new CuckooMap(capacity * 2);
        this.chain = new DDList(0, nodePool);
        this.mean = new Mean();

    }


    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public float mass() {
        return mass;
    }

    @Override
    public V pop() {
        if (size() == 0) return null;
        DD<V> d = next();
        if (d==null) return null;

        removal.addValue(d.item.getPriority());
        return remove(d.item.name());
    }

    @Override
    public V peekNext() {
        DD<V> d = next();
        if (d!=null) return d.item;
        return null;
    }

    @Override
    public V update(BagSelector<K, V> selector) {

        final K key = selector.name();
        final DD<V> bx;
        if (key != null) {
            bx = index.get(key);
        }
        else {
            bx = next();
        }


        if ((bx == null) || (bx.item == null)) {
            //allow selector to provide a new instance
            V n = selector.newItem();
            if (n!=null) {
                V overflow = put(n);
                if (overflow!=null)
                    selector.overflow(overflow);
                return n; //return the new instance
            }
            //no instance provided, nothing to do
            return null;
        }

        final V b = bx.item;

        //allow selector to modify it, then if it returns non-null, reinsert
        final V c = selector.update(b);
        if (c!=null) {
            bx.item = c;
            updatePercentile(c.getPriority());
            return c;
        }
        else
            return b;

    }

    @Override
    public V put(V newItem) {

        if (nextRemoval!=null && nextRemoval.getPriority() > newItem.getPriority())
            return newItem; //too low priority to add to this

        DD<V> d = chain.add(newItem);
        DD<V> previous = index.put(newItem.name(), d);
        if (previous!=null) {
            //displaced an item with the same key
            merge(previous.item.budget,  newItem.budget);
            updatePercentile(previous.item.getPriority());
            return null;
        }
        else {
            boolean atCapacity = (size() >= capacity());

            if (atCapacity && nextRemoval!=null) {
                V overflow = remove(nextRemoval.name());
                nextRemoval = null;
                return overflow;
            }
            else {
                //bag will remain over-budget until a removal candidate is decided
            }
        }
        updatePercentile(newItem.getPriority());
        return null;
    }

    protected DD<V> next() {
        final int s = size();
        if (s == 0) return null;
        final boolean atCapacity = s >= capacity();

        DD<V> next = after(current);

        if (s == 1)
            return next;

        do {


            /*if (next == null) {
                throw new RuntimeException("size = " + size() + " yet there is no first node in chain");
            }*/

            final V ni = next.item;

            /*if (ni == null) {
                throw new RuntimeException("size = " + size() + " yet iterated cell with null item");
            }*/

            final double percentileEstimate = getPercentile(ni.getPriority());

            if (selectPercentile(percentileEstimate))
                break;


            if (atCapacity) {
                considerRemoving(next, percentileEstimate);
            }

            next = after(next);

        } while (true);

        return current = next;
    }


    /** updates the adaptive percentile measurement; should be called on put and when budgets update  */
    private void updatePercentile(final float priority) {
        //DescriptiveStatistics percentile is extremely slow
        //contentStats.getPercentile(ni.getPriority())
        //approximate percentile using max/mean/min

        this.mean.increment(priority);
        final double mean = this.mean.getResult();

        final double momentum = minMaxMomentum;


        estimatedMax = (estimatedMax < priority) ? priority : (1.0 - momentum) * mean + (momentum) * estimatedMax;
        estimatedMin = (estimatedMin > priority) ? priority : (1.0 - momentum) * mean + (momentum) * estimatedMin;
        estimatedMean = this.mean.getResult();
    }

    /** uses the adaptive percentile data to estimate a percentile of a given priority */
    private double getPercentile(final float priority) {

        final double mean = this.estimatedMean;

        final double upper, lower;
        if (priority < mean) {
            lower = estimatedMin;
            upper = mean;
        }
        else if (priority == mean) {
            return 0.5f;
        }
        else {
            upper = estimatedMax;
            lower = mean;
        }

        final double perc = (priority - lower) / (upper-lower);

        final double minPerc = 1.0 / size();

        if (perc < minPerc) return minPerc;

        return perc;
    }

    protected boolean considerRemoving(final DD<V> d, final double percentileEstimate) {
        //TODO improve this based on adaptive statistics measurement
        final V item = d.item;
        final float p = item.getPriority();
        final V nr = nextRemoval;
        if (nr==null) {
            if (percentileEstimate < PERCENTILE_THRESHOLD_FOR_EMERGENCY_REMOVAL) {
                nextRemoval = item;
                return true;
            }
        }
        else if (nr != item) {
            if (p < nr.getPriority()) {
                nextRemoval = item;
                return true;
            }
        }

        return false;
    }

    protected boolean selectPercentile(final double percentileEstimate) {
        return Memory.randomNumber.nextFloat() < percentileEstimate;
    }

    protected boolean selectPercentage(V v) {
        return Memory.randomNumber.nextFloat() < v.getPriority();
    }

    protected DD<V> after(final DD<V> d) {
        final DD<V> n = d!=null ? d.next : null;
        if ((n == null) || (n.item == null))
            return chain.getFirstNode();
        return n;
    }

    @Override
    public int size() {
        final int s1 = index.size();
        if (Global.DEBUG) {
            final int s2 = chain.size();
            if (s1 != s2)
                throw new RuntimeException(this + " bag fault; inconsistent index");
        }
        return s1;
    }


    @Override
    public Iterator<V> iterator() {
        return chain.iterator();
    }

    @Override
    public void clear() {

        chain.clear();
        index.clear();
        mass = 0;
        current = null;
    }



    @Override
    public V remove(K key) {
        DD<V> d = index.remove(key);
        if (d!=null) {
            V v = d.item; //save it here because chain.remove will nullify .item field
            chain.remove(d);

            if (Global.DEBUG) size();

            return v;
        }

        return null;
    }



    @Override
    public V get(final K key) {
        final DD<V> d = index.get(key);
        return (d!=null) ? d.item : null;
    }

    @Override
    public Set<K> keySet() {
        return index.keySet();
    }

}
