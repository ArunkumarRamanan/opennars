package nars.operate;

import nars.NAR;
import nars.event.EventEmitter;
import nars.event.Reaction;

/**
* Created by me on 1/12/15.
*/
abstract public class AbstractOperator implements IOperator, Reaction {

    private EventEmitter.Registrations regist = null;

    @Override
    public boolean setEnabled(NAR n, boolean enabled) {
        if (enabled) {
            regist = n.memory.event.on(this, getEvents());

            onEnabled(n);

        }
        else  {
            cancel();

            onDisabled(n);
        }
        return true;
    }

    /** list of event channels to listen to */
    abstract public Class[] getEvents();

    /** called when plugin is enabled */
    abstract public void onEnabled(NAR n);

    /** called when plugin is disabled */
    abstract public void onDisabled(NAR n);

    /** manually cancel this plugin by removing its event registration */
    public boolean cancel() {
        if (regist!=null) {
            regist.cancel();
            regist = null;
            return true;
        }
        return false;
    }
}
