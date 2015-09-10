package nars.sonification;

import com.google.common.collect.Lists;
import nars.*;
import nars.audio.SoundListener;
import nars.audio.SoundProducer;
import nars.audio.granular.Granulize;
import nars.audio.sample.SonarSample;
import nars.audio.synth.SineWave;
import nars.concept.Concept;
import nars.event.ConceptReaction;
import nars.event.FrameReaction;
import nars.guifx.NARfx;
import nars.io.out.TextOutput;
import nars.nar.experimental.Equalized;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sonifies the activity of concepts being activated and forgotten
 */
public class ConceptSonification extends FrameReaction {

    List<SonarSample> samples;

    private final Audio sound;
    Map<Concept, SoundProducer> playing = Global.newHashMap();
    float audiblePriorityThreshold = 0.8f;


    public ConceptSonification(NAR nar, Audio sound) throws IOException {
        super(nar);

        this.sound = sound;

        //Events.ConceptProcessed.class,
            /*Premise f = (Premise)args[0];
            update(f.getConcept());*/


        nar.memory.eventConceptProcessed.on(c -> {
            update(c.getConcept());
        });
        updateSamples();

        //TODO update all existing concepts on start?
    }

    public static void main(String[] args) throws LineUnavailableException, IOException {
        NAR n = new NAR(new Equalized(1000,8,3));
        new ConceptSonification(n, new Audio(16));
        n.believe("<a-->b>");
        n.believe("<b-->c>");
        n.stdout();
        n.loop(100);

        //NARfx.newWindow(n);
    }


    protected void updateSamples() throws IOException {

        //samples = Files.list(Paths.get("/home/me/share/wav")).
         //       map(p -> p.toAbsolutePath().toString() ).filter( x -> x.endsWith(".wav") ).collect(Collectors.toList());

        samples = Lists.newArrayList(
                SonarSample.digitize(t ->
                        (float) (Math.sin(t * 1000.0f) + 0.25 * Math.sin(Math.exp(t * 200.0f))),
                        44100 /* sample rate */, 0.5f /* clip duration */),
                SonarSample.digitize(t ->
                        (float) (Math.tan(t * 500.0f) + 0.25 * Math.sin(Math.cos(t * 200.0f))),
                        44100 /* sample rate */, 0.5f /* clip duration */)
        );

        Collections.shuffle(samples);
    }

    /** returns file path to load sample */
    SonarSample getSample(Concept c) {
        return samples.get(Math.abs(c.hashCode()) % samples.size());
    }

    public void update(Concept c) {
        if (c.getPriority() > audiblePriorityThreshold) {
            SoundProducer g = playing.get(c);
            if (g == null) {
                if (!samples.isEmpty()) {
                    SonarSample sp = getSample(c);
                    //do {
                        try {
                            g = new Granulize(sp, 0.1f, 0.1f);
                            //g = new SineWave(Video.hashFloat(c.hashCode()));
                        } catch (Exception e) {
                            samples.remove(sp);
                            g = null;
                            return;
                        }
                    //} while ((g == null) && (!samples.isEmpty()));

                    playing.put(c, g);
                    sound.play(g, 1f, 1);
                }
            }

            if (g!=null)
                update(c, g);
        }
        else {
            SoundProducer g = playing.remove(c);
            if (g!=null)
                g.stop();
        }
    }

    private void update(Concept c, SoundProducer g) {
        if (g instanceof Granulize) {
            ((Granulize)g).setStretchFactor(1f + 4f * (1f - c.getQuality()));
        }
        if (g instanceof SoundProducer.Amplifiable) {
            ((SoundProducer.Amplifiable)g).setAmplitude((c.getPriority() - audiblePriorityThreshold) / (1f - audiblePriorityThreshold));
        }
    }

    protected void updateConceptsPlaying() {
        for (Map.Entry<Concept, SoundProducer> e : playing.entrySet()) {
            update(e.getKey(), e.getValue());
        }
    }

    @Override
    public void onFrame() {
        updateConceptsPlaying();
    }


}
