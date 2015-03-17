package ca.nengo.test;

import automenta.vivisect.Video;
import ca.nengo.model.AgentNode;
import ca.nengo.model.Node;
import ca.nengo.model.SimulationException;
import ca.nengo.model.StructuralException;
import ca.nengo.model.impl.DefaultNetwork;
import ca.nengo.model.impl.NetworkImpl;
import ca.nengo.ui.Nengrow;
import ca.nengo.ui.lib.world.WorldObject;
import ca.nengo.ui.model.node.UINetwork;
import ca.nengo.ui.model.plot.LinePlot;
import ca.nengo.ui.model.plot.StringView;
import ca.nengo.ui.model.widget.PadNode;
import ca.nengo.ui.model.widget.SliderNode;
import nars.build.Default;
import nars.core.Events;
import nars.core.Memory;
import nars.core.NAR;
import nars.core.Parameters;
import nars.gui.NARSwing;
import nars.io.Output;
import nars.logic.entity.Task;
import nars.logic.entity.Term;
import nars.logic.nal1.Inheritance;
import nars.logic.nal3.SetExt;
import nars.logic.nal8.Operation;
import nars.logic.nal8.Operator;
import nars.logic.nal8.TermFunction;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by me on 3/3/15.
 */
public class TestNARNode extends Nengrow {


    float time = 0;

    public static void main(String[] args) {
        new TestNARNode().window(800,600);
    }

    public Node newAgentNodeDemo() throws StructuralException {
        NetworkImpl network = new DefaultNetwork<>();

        NAR nar = new NAR(new Default());
        NARNode an = new NARNode("NARBot1", nar);
        Video.themeInvert();
        new NARSwing(nar);

        //an.setMovementBounds(new Rectangle2D.Double(0, 0, 500.01, 500.01));
        network.addNode(an);


        network.addNode(new StringView("Text1"));
        network.addNode(new LinePlot("Plot1"));
        network.addNode(new SliderNode("A", 0, 0, 1f));
        network.addNode(new SliderNode("B", 0, 0, 50f));
        network.addNode(new PadNode("XY", 2, 0, 8, 4, 0, 8));


        return network;
    }


    public static class NARNode extends AgentNode {

        public final NAR nar;

        public NARNode(String name, NAR nar) {
            super(name);
            this.nar = nar;
            nar.memory.setSelf(Term.get(name));
            nar.param.noiseLevel.set(25);
            initOperators();

            new Output(nar) {

                final float speakThreshold = 0.9f;

                @Override
                public void event(Class event, Object[] args) {
                    if (event == Events.OUT.class) {
                        Task t= (Task)args[0];
                        if (t.getPriority() > speakThreshold)
                            say(t.sentence.toString());
                    }

                }
            };
        }

        @Override
        public void run(float startTime, float endTime) throws SimulationException {
            super.run(startTime, endTime);

            nar.step(1); //TODO scale # cycles to actual time elapsed
        }

        protected void initOperators() {

            //access to world objects
            nar.on(new Operator("^object") {

                @Override
                protected List<Task> execute(Operation operation, Term[] args, Memory memory) {
                    return null;
                }
            });

            /*nar.addPlugin(new SynchronousSentenceFunction("^near") {


                @Override
                protected Collection<Sentence> function(Memory memory, Term[] x) {
                    if (x.length > 1) {
                        Term obj =
                    }
                    return null;
                }

            });*/

            nar.on(new TermFunction("^see") {


                @Override
                public Term function(Term[] x) {


                    Collection<WorldObject> intersects;

                    if (x.length == 0) {
                        intersects = ui.intersecting(null);
                    } else {
                        Term t = x[0];
                        String st = t.toString();

                        double ww = Math.max(ui.getWidth(), ui.getHeight());
                        double dx = 0, dy = 0;
                        double heading = Double.NaN;
                        switch (st) {
                            case "front":
                                heading = getHeading();
                                break;
                            case "back":
                                heading = -getHeading();
                                break;
                            case "left":
                                heading = getHeading() - Math.PI / 2;
                                break;
                            case "right":
                                heading = getHeading() + Math.PI / 2;
                                break;
                            default:
                                dx = dy = 0;
                                break;
                        }

                        if (Double.isFinite(heading)) {
                            dx = ww * Math.cos(heading);
                            dy = ww * Math.sin(heading);
                        }
                        intersects = ui.intersecting(null, dx, dy);
                    }

                    intersects.add(ui.getNetworkParent());

                    Set<Term> t = new HashSet(intersects.size());

                    for (WorldObject w : intersects) {
                        if (w == NARNode.this.ui) continue;
                        String ww = w.getName().trim();
                        if (ww.isEmpty()) {
                            if (Parameters.DEBUG)
                                System.err.println("Warning: " + w + " (" + w.getClass() + ") has empty name");
                            continue;
                        }
                        t.add(Term.text(ww));
                    }

                    return Inheritance.make(SetExt.make(t), Term.get("intersects"));
                }


            });

            nar.on(new Operator("^move") {

                @Override
                protected List<Task> execute(Operation operation, Term[] args, Memory memory) {

                    double dx = 64;
                    boolean error = true;
                    if (args.length > 1) {
                        Term param = args[0];
                        String ps = param.toString();

                        error = false;
                        double d = 0;
                        switch (ps) {
                            case "front":
                                d = +dx;
                                break;
                            case "back":
                                d = -dx;
                                break;
                            default:
                                error = true;
                        }
                        if (!error) {
                            forward(d);
                        }
                    }

                    return null;
                }
            });
            nar.on(new Operator("^turn") {

                @Override
                protected List<Task> execute(Operation operation, Term[] args, Memory memory) {
                    double dA = Math.PI / 4; //radians

                    boolean error = true;
                    if (args.length > 1) {
                        Term param = args[0];
                        String ps = param.toString();

                        error = false;
                        switch (ps) {
                            case "left":
                                rotate(-dA);
                                break;
                            case "right":
                                rotate(+dA);
                                break;
                            case "north":
                                heading(-Math.PI / 2);
                                break;
                            case "south":
                                heading(Math.PI / 2);
                                break;
                            case "east":
                                heading(0);
                                break;
                            case "west":
                                heading(2 * Math.PI / 2);
                                break;
                            default:
                                error = true;
                        }
                    }

                    if (error) {
                        //...
                    }

                    return null;
                }
            });

            nar.input(
                    "see(#objects)! \n 10\n see(front, #objects)!\n 20 \n move(front)! \n" +
                            " 20 \n" +
                            " move(back)! \n 20 \n move(left)! \n 20 \n 20 \n turn(left)! \n 20 \n turn(right)!\n" +
                            "20\n turn(north)! \n 20 \n turn(south)! \n"
            );
        }



    }

    @Override
    public void init() throws Exception {


        UINetwork networkUI = (UINetwork) addNodeModel(newAgentNodeDemo());
        networkUI.doubleClicked();


        new Timer(100, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    float dt = getSimulationDT();
                    networkUI.node().run(time, time + dt);
                    time += dt;

                } catch (SimulationException e1) {
                    e1.printStackTrace();
                }
                //cycle();
            }
        }).start();

    }

}
