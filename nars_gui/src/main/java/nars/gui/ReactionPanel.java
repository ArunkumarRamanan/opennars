package nars.gui;

import automenta.vivisect.swing.NPanel;
import nars.NAR;
import nars.event.EventEmitter;
import nars.event.Reaction;

import java.awt.*;

abstract public class ReactionPanel extends NPanel implements Reaction {

    public final NAR nar;
    private EventEmitter.Registrations reg;

    public ReactionPanel(NAR n) {
        super();
        this.nar = n;
    }
    public ReactionPanel(NAR n, LayoutManager l) {
        this(n);
        setLayout(l);
    }


    @Override
    protected void visibility(boolean appearedOrDisappeared) {
        if (appearedOrDisappeared) {
            if (reg==null)
                reg = nar.memory.event.on(this, getEvents());
        }
        else {
            if (reg!=null)
                reg.cancel();
        }
    }

    abstract public Class[] getEvents();

}
