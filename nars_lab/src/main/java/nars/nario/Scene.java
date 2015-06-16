package nars.nario;

import automenta.vivisect.Audio;
import automenta.vivisect.audio.SoundListener;

import java.awt.*;


public abstract class Scene implements SoundListener {
    public Audio sound;
    public static boolean[] keys = new boolean[16];

    public void toggleKey(int key, boolean isPressed)
    {
        keys[key] = isPressed;
    }

    public final void setSound(Audio sound) {
        //sound.setListener(this);
        //this.sound = sound;
    }

    public abstract void init();

    public abstract void tick();

    public abstract void render(Graphics og, float alpha);
}