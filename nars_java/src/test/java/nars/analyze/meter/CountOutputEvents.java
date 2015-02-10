package nars.analyze.meter;

import nars.core.AbstractPlugin;
import nars.core.Events;
import nars.core.NAR;
import nars.io.meter.Metrics;
import nars.io.meter.event.HitMeter;

import java.util.HashMap;
import java.util.Map;

/**
* Created by me on 2/10/15.
*/
public class CountOutputEvents extends AbstractPlugin {

//        public static final DoubleMeter numIn = new DoubleMeter("IN");
//        public static final DoubleMeter numOut = new DoubleMeter("OUT");

    final Map<Class, HitMeter> eventMeters = new HashMap();

    public CountOutputEvents(Metrics m) {
        super();

        for (Class c : getEvents()) {
            HitMeter h = new HitMeter(c.getSimpleName());
            eventMeters.put(c, h);
            m.addMeter(h);
        }

    }

    public static final Class[] ev = new Class[] {
            Events.IN.class,
            Events.EXE.class,
            Events.OUT.class,
            Events.ERR.class,
            Events.Answer.class,
    };

    @Override
    public Class[] getEvents() {
        return ev;
    }

    @Override
    public void onEnabled(NAR n) {

    }

    @Override
    public void onDisabled(NAR n) {

    }

    @Override
    public void event(Class event, Object[] args) {
        eventMeters.get(event).hit();
    }

    public void reset() {
        for (HitMeter h : eventMeters.values())
            h.reset();
    }
}
