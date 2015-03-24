/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.energy.bag.experimental;

import nars.nal.entity.Item;
import nars.util.math.Distributor;
import reactor.jarjar.jsr166e.extra.AtomicDouble;

/**
 * TODO add a strict probability mode which excludes low priority items from exceeding
 * their proportion in a resulting reload() pending queue
 */
public class FairDelayBag<E extends Item<K>, K> extends DelayBag<K, E> {

    /** # of levels should be "fairly" low, reducing the complete cycle length of the distributor */
    final static int levels = 10;
    
    final static short[] distributor = Distributor.get(levels).order;
    
    
    public FairDelayBag(AtomicDouble forgetRate, int capacity) {
        super(forgetRate, capacity);
    }

    
    public FairDelayBag(AtomicDouble forgetRate, int capacity, int targetPendingBufferSize) {
        super(forgetRate, capacity, targetPendingBufferSize);
    }

    
    
    @Override
    protected boolean fireable(final E c) {    
        /** since distributor has a min value of 1, 
            subtract one so that items with low priority can be selected */
        final int currentLevel = distributor[reloadIteration % levels] - 1;
        return c.budget.getPriority() * levels >= currentLevel;
    }

    @Override
    protected void adjustActivationThreshold() {
        //do nothing about this, fireable in this impl doesn't depend on activation threshold
    }


//    @Override
//    public E UPDATE(BagSelector<K, E> selector) {
//        //TODO provide a full implementation
//        super.putInFast(selector);
//        //this needs to return the selected or created item, not the result of PUT which is overflow
//        return null;
//    }

}
