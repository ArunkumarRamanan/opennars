package nars.gui;

import automenta.vivisect.Video;
import nars.Memory;
import nars.NAR;
import nars.ProtoNAR;
import nars.prototype.Default;

/**
 * NARSwing for a Real-time NAR
 */
public class NARSwingRT {

    public static void main(String[] args) {
        Video.themeInvert();

        ProtoNAR d = new Default();
        d.param.setTiming(Memory.Timing.RealMS);
        d.param.duration.set(500);

        NAR n = new NAR(d);

        NARSwing s = new NARSwing(n);
        s.newKeyboardInput();

        s.setSpeed(0.5f);
    }
}
