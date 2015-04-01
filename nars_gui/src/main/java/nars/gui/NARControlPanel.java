/*
 * NARControls.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARSwing.
 *
 * Open-NARSwing is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARSwing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARSwing.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.gui;

import automenta.vivisect.swing.AwesomeButton;
import automenta.vivisect.swing.NSlider;
import automenta.vivisect.swing.NWindow;
import automenta.vivisect.swing.TimeControl;
import ca.nengo.ui.NengrowPanel;
import nars.Events;
import nars.Events.FrameEnd;
import nars.Memory;
import nars.NAR;
import nars.event.Reaction;
import nars.gui.input.KeyboardInputPanel;
import nars.gui.input.TextInputPanel;
import nars.gui.output.*;
import nars.gui.output.chart.MeterNode;
import nars.gui.output.graph.nengo.GraphPanelNengo;
import nars.io.TextOutput;
import nars.nal.meta.NARMetrics;
import reactor.jarjar.jsr166e.extra.AtomicDouble;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static nars.Memory.Timing.RealMS;
import static nars.Memory.Timing.Simulation;


public class NARControlPanel extends TimeControl implements Reaction {

    //final int TICKS_PER_TIMER_LABEL_UPDATE = 4 * 1024; //set to zero for max speed, or a large number to reduce GUI updates

    /**
     * Reference to the reasoner
     */
    public final NAR nar;

    /**
     * Reference to the memory
     */
    private final Memory memory;
    
    /**
     * Reference to the experience writer
     */
    private final TextOutput experienceWriter;
    private final MeterNode meters;


    /**
     * Whether the experience is saving into a file
     */
    private boolean savingExp = false;


    private NSlider volumeSlider;


    private final NARMetrics metrics;

    public NARControlPanel(final NAR nar) {
        this(nar, null, true);
    }
    
    public NARControlPanel(final NAR nar, final NARMetrics metrics, boolean addCharts) {
        super(new BorderLayout());
        
        this.nar = nar;
        this.memory = nar.memory;
        this.metrics = (metrics == null) ? new NARMetrics(nar, 128) : metrics;


        experienceWriter = new TextOutput(nar);


        JMenuBar menuBar = new JMenuBar();

        JMenu m = new JMenu("Memory");
        addJMenuItem(m, "Reset");
        m.addSeparator();
        addJMenuItem(m, "Load Experience");
        addJMenuItem(m, "Save Experience");        
        
        /*internalExperienceItem = addJMenuItem(m, "Enable Internal Experience (NAL9)");
        fullInternalExp = addJMenuItem(m, "Enable Full Internal Experience");
        narsPlusItem = addJMenuItem(m, "Enable NARS+ Ideas");*/
        
        m.addActionListener(this);
        menuBar.add(m);

        m = new JMenu("Windows");
        {
            
            JMenuItem mv3 = new JMenuItem("+ Input");
            mv3.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    TextInputPanel inputPanel = new TextInputPanel(nar);
                    NWindow inputWindow = new NWindow("Input", inputPanel);                    
                    inputWindow.setSize(800, 200);
                    inputWindow.setVisible(true);        
                }
            });
            m.add(mv3);

            JMenuItem mu3 = new JMenuItem("+ Keyboard");
            mu3.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    newKeyboardInput();
                }
            });
            m.add(mu3);



            //not really relevant for NARS, Im working on a active approach to detecting such patterns
            //which will work when conditioning works good
           /* JMenuItem cct4 = new JMenuItem("+ Input Drawing");
            cct4.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NWindow w = new NWindow("Sketch", new SketchPointCloudPanel(nar));
                    w.setSize(500,500);
                    w.setVisible(true);
                }                
            });
            m.add(cct4); */
            
            JMenuItem ml = new JMenuItem("+ Output");
            ml.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                    
                    new NWindow("Output", new SwingLogPanel(nar)).show(500, 300);
                }
            });
            m.add(ml);
            
            m.addSeparator();


            JMenuItem mv = new JMenuItem("+ Concept Network");
            mv.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new NWindow("Concepts",
                            new GraphPanelNengo(nar) ).show(800, 800, false);
                }
            });
            m.add(mv);
            
//            JMenuItem tlp = new JMenuItem("+ Timeline");
//            tlp.addActionListener(new ActionListener() {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    NWindow outputWindow = new NWindow("Timeline", new TimelinePanel(nar, trace));
//                    outputWindow.show(900, 700);        
//                }
//            });
//            m.add(tlp);

            m.addSeparator();

            /* JMenuItem pml = new JMenuItem("+ Planning Log");
            pml.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                    
                    new NWindow("Planning", new SwingLogPanel(NARControls.this, 
                            MultipleExecutionManager.class, Execution.class, 
                            GraphExecutive.ParticlePath.class, 
                            GraphExecutive.ParticlePlan.class))
                    .show(500, 300);
                }
            });
            m.add(pml); */
            
            JMenuItem gml = new JMenuItem("+ Forgetting Log");
            gml.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                    
                    new NWindow("Forgot", new SwingLogPanel(nar,
                            Events.ConceptForget.class
                            //, Events.TaskRemove.class, Events.TermLinkRemove.class, Events.TaskLinkRemove.class)
                    ))
                    .show(500, 300);
                }
            });
            m.add(gml);
            
            /* JMenuItem al = new JMenuItem("+ Activity");
            al.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new NWindow("Activity", new MultiOutputPanel(NARControls.this)).show(500, 300);                }
            });
            m.add(al); */


//            JMenuItem imv = new JMenuItem("+ Eternalized Implications Graph");
//            imv.addActionListener(new ActionListener() {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    //new Window("Implication Graph", new SentenceGraphPanel(nar, nar.memory.executive.graph.implication)).show(500, 500);
//                    new NWindow("Implication Graph",
//                            new PCanvas(
//                                    new AnimatingGraphVis(
//                                            nar.memory.executive.graph.implication,
//                                            new NARGraphDisplay(nar),
//                                            new FastOrganicLayout()
//                                    ))).show(500, 500);
//                }
//            });
//            m.add(imv);




//
//            JMenuItem sg = new JMenuItem("+ Inheritance / Similarity Graph");
//            sg.addActionListener(new ActionListener() {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    new NWindow("Inheritance Graph", 
//                            new ProcessingGraphPanel(nar, 
//                                    new SentenceGraphCanvas(
//                                            new InheritanceGraph(nar)))).show(500, 500);
//                }
//            });
//            m.add(sg);
            
            m.addSeparator();
            
            JMenuItem tt = new JMenuItem("+ Task Tree");
            tt.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                    
                    new NWindow("Task Tree", new TaskTree(nar)).show(300, 650, false);
                }
            });
            m.add(tt);
            
            
            JMenuItem it = new JMenuItem("+ Idea Panel");
            it.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                    
                    new NWindow("Ideas", new IdeaPanel(nar)).show(400, 700, false);
                }
            });
            m.add(it);
            
            m.addSeparator();
            
            JMenuItem st = new JMenuItem("+ Sentence Table");
            st.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SentenceTablePanel p = new SentenceTablePanel(nar);
                    NWindow w = new NWindow("Sentence Table", p);
                    w.setSize(500, 300);
                    w.setVisible(true);                    
                }
            });
            m.add(st);

         /* not working yet anyway   JMenuItem fc = new JMenuItem("+ Freq. vs Confidence");
            fc.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    BubbleChart bc = new BubbleChart(nar);
                    NWindow wbc = new NWindow("Freq vs. Conf", bc);
                    wbc.setSize(250,250);
                    wbc.setVisible(true);
                }
            });
            m.add(fc); */
            
            JMenuItem hf = new JMenuItem("+ Humanoid Face");
            hf.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NARFacePanel f = new NARFacePanel(nar);
                    NWindow w = new NWindow("Face", f);
                    w.setSize(250,400);
                    w.setVisible(true);
                }
            });
            m.add(hf);
            
            
            /*JMenuItem ct = new JMenuItem("+ Concepts");
            ct.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // see design for Bag and {@tlink BagWindow} in {@tlink Bag#startPlay(String)}
                    memory.conceptsStartPlay(new BagWindow<Concept>(), "Active Concepts");                    
                }
            });
            m.add(ct);
            
            JMenuItem bt = new JMenuItem("+ Buffered Tasks");
            bt.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    memory.taskBuffersStartPlay(new BagWindow<Task>(), "Buffered Tasks");
                }
            });
            m.add(bt);*/
            
            /*JMenuItem cct = new JMenuItem("+ Concept Content");
            cct.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    conceptWin.setVisible(true);                
                }                
            });
            m.add(cct);*/
            
            
            
            
            /*
            JMenuItem it = new JMenuItem("+ Inference Log");
            it.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    record.show();
                    record.play();
                }                
            });
            m.add(it);            
            */
        }
        menuBar.add(m);
        
//        m = new JMenu("Demos");
//        {
//            JMenuItem cct2 = new JMenuItem("+ Test Chamber");
//            cct2.addActionListener(new ActionListener() {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    
//                    chamber.create(nar);
//                }                
//            });
//            m.add(cct2);
//        }
//        menuBar.add(m);

        m = new JMenu("Help");
        //addJMenuItem(m, "Related Information");
        addJMenuItem(m, "About NARS");
        m.addActionListener(this);
        menuBar.add(m);

        
        JPanel top = new JPanel(new BorderLayout());
        
        top.add(menuBar, BorderLayout.NORTH);


        JComponent jp = newParameterPanel();
        top.add(jp, BorderLayout.CENTER);


        
        
//        CompoundMeter senses = new CompoundMeter(memory.logic, memory.resource) {
//            
//            
//            //@Override
//            public Chart newDefaultChart(String id, TreeMLData data) {                
//                switch (id) {
//                    case "concept.pri.histo":
//                        return new StackedPercentageChart(data).height(2);
//                    case "concept.pri.mean":
//                    case "task.pri.mean":
//                        return new LineChart(data).range(0, 1f);
//                    case "plan.graph":
//                    case "plan.graph.add":
//                    case "plan.task":                    
//                    case "concept.belief.mean":
//                    case "task.process":                        
//                        return new LineChart(data);
//                        
//                }
//                return new BarChart(data);
//            }          
//        };
//        senses.setActive(true);
//        senses.update(memory);        
        
        add(top, NORTH);

        if (addCharts) {
            meters = new MeterNode(nar, this.metrics.getMetrics());
            NengrowPanel np = new NengrowPanel(meters);


            add(np, CENTER);
        }
        else
            meters = null;
        
        init();
        volumeSlider.setValue(nar.param.outputVolume.get());
        
    }

    public NWindow newKeyboardInput() {
        return new NWindow("Keyboard Input", new KeyboardInputPanel(nar)).show(300, 100, false);
    }

    /**
     * @param m
     * @param item
     */
    private JMenuItem addJMenuItem(JMenu m, String item) {
        JMenuItem menuItem = new JMenuItem(item);
        m.add(menuItem);
        menuItem.addActionListener(this);
        return menuItem;
    }

    /**
     * Open an addInput experience file with a FileDialog
     */
    public void openLoadFile() {
        FileDialog dialog = new FileDialog((Dialog) null, "Load experience", FileDialog.LOAD);
        dialog.setVisible(true);
        String directoryName = dialog.getDirectory();
        String fileName = dialog.getFile();
        String filePath = directoryName + fileName;

        try {
            nar.input(new File(filePath));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    /**
     * Initialize the system for a new finish
     */
    public void init() {
        setSpeed(0);
        setSpeed(0);        //call twice to make it start as paused
        setFrameRate(25);
        updateGUI();
        nar.memory.event.on(FrameEnd.class, this);
    }

    final Runnable updateGUIRunnable = new Runnable() {
        @Override public void run() {
            updateGUI();
        }
    };

    protected void updateGUI() {
                
        speedSlider.repaint();

        updateScheduled.set(false);

    }
    

    @Override
    public void event(final Class event, final Object... arguments) {
        if (event == FrameEnd.class) {
            
            long now = System.currentTimeMillis();
            long deltaTime = now - lastUpdateTime;
            
            if ((deltaTime >= GUIUpdatePeriodMS) /*|| (!updateScheduled.get())*/) {
                
                updateScheduled.set(true);                
                                                
                speedSlider.repaint();
                
                SwingUtilities.invokeLater(updateGUIRunnable);

                lastUpdateTime = now;
                
            }
        }
    }

    public void setFrameRate(float fps) {
        this.GUIUpdatePeriodMS = (int)(1000.0/fps);
    }

    /**
     * Handling button click
     *
     * @param e The ActionEvent
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Object obj = e.getSource();
        if (obj instanceof JButton) {
            if (obj == stopButton) {
                setSpeed(0);
                updateGUI();
            } else if (obj == walkButton) {
                nar.stop();
                nar.step(1);
                updateGUI();
            }
        } else if (obj instanceof JMenuItem) {
            String label = e.getActionCommand();
            switch (label) {
                //case "Enable Full Internal Experience":
                    //fullInternalExp.setEnabled(false);
                    //Parameters.INTERNAL_EXPERIENCE_FULL=true;
                    //Parameters.ENABLE_EXPERIMENTAL_NARS_PLUS=!Parameters.ENABLE_EXPERIMENTAL_NARS_PLUS;
                  //  break;
                    
//                case "Enable NARS+ Ideas":
//                    narsPlusItem.setEnabled(false);
//                    nar.memory.param.experimentalNarsPlus.set(true);
//                    break;
//                case "Enable Internal Experience (NAL9)":
//                    internalExperienceItem.setEnabled(false);
//                    nar.memory.param.internalExperience.set(true);
//                    break;
                    
                case "Load Experience":
                    openLoadFile();
                    break;
                case "Save Experience":
                    if (savingExp) {
                        experienceWriter.closeSaveFile();
                    } else {
                        FileDialog dialog = new FileDialog((Dialog) null, "Save experience", FileDialog.SAVE);
                        dialog.setVisible(true);
                        String directoryName = dialog.getDirectory();
                        String fileName = dialog.getFile();
                        String path = directoryName + fileName;
                        experienceWriter.openSaveFile(path);
                    }
                    savingExp = !savingExp;
                    break;
                case "Reset":
                    /// TODO mixture of modifier and reporting
                    //narsPlusItem.setEnabled(true);
                    //internalExperienceItem.setEnabled(true);
                    nar.reset();
                    break;
                case "Related Information":
//                MessageDialog web =
                    new MessageDialog(NAR.WEBSITE); 
                    break;
                case "About NARS":
//                MessageDialog info =
                    new MessageDialog(NAR.VERSION+"\n\n"+NAR.WEBSITE);
                    break;
            }
        }
    }


    private NSlider newVolumeSlider() {
        final NSlider s = this.volumeSlider = new NSlider(100f, 0, 100f) {

            @Override
            public String getText() {
                if (value == null) {
                    return "";
                }

                float v = value();
                String s = "Volume: " + super.getText() + " (";

                if (v == 0) {
                    s += "Silent";
                } else if (v < 25) {
                    s += "Quiet";
                } else if (v < 75) {
                    s += "Normal";
                } else {
                    s += "Loud";
                }

                s += ")";
                return s;
            }

            @Override
            public void setValue(float v) {
                super.setValue(Math.round(v));
                repaint(); //needed to update when called from outside, as the 'focus' button does
            }

            @Override
            public void onChange(float v) {
                int level = (int) v;
                (nar.param).outputVolume.set(level);
            }

        };

        return s;
    }

    @Override
    public void setSpeed(float nextSpeed) {
        final float maxPeriodMS = 1024.0f;

        if (nextSpeed == -1) {
            //disable speed change
            speedSlider.setEnabled(false);
            stopButton.setEnabled(false);
            walkButton.setEnabled(false);
        }

        if (nextSpeed == 0) {
            if (currentSpeed == 0) {
                if (lastSpeed == 0) {
                    lastSpeed = defaultSpeed;
                }
                nextSpeed = lastSpeed;
            } else {
            }

        }
        if (currentSpeed == nextSpeed) return;
        lastSpeed = currentSpeed;
        speedSlider.repaint();
        stopButton.setText(String.valueOf(FA_PlayCharacter));

        /*if (currentSpeed == s)
         return;*/
        speedSlider.setValue(nextSpeed);
        currentSpeed = nextSpeed;

        float logScale = 50f;
        if (nextSpeed > 0) {
            long ms = (long) ((1.0 - Math.log(1+nextSpeed*logScale)/Math.log(1+logScale)) * maxPeriodMS);
            if (ms < 1) {
                if (allowFullSpeed)
                    ms = 0;
                else
                    ms = 1;
            }
            stopButton.setText(String.valueOf(FA_StopCharacter));
            //nar.setThreadYield(true);
            nar.start(ms, nar.getCyclesPerFrame());
        } else {
            stopButton.setText(String.valueOf(FA_PlayCharacter));
            nar.stop();
        }
    }


    
//
//    @Override
//    public void run() {
//        
//
//        updateGUI();
//        
//        lastTime = nar.getTime();
//
//        while (true) {
//            try {
//                Thread.sleep(GUIUpdatePeriodMS);
//            } catch (InterruptedException ex) {
//            }
//
//
//            updateGUI();
//
//        }
//    }


    AtomicDouble tasklinkRate = new AtomicDouble(1),
            termlinkRate = new AtomicDouble(1),
            novelRate = new AtomicDouble(1);

    private JComponent newParameterPanel() {
        JPanel p = new JPanel();

        JPanel pc = new JPanel();

        pc.setLayout(new GridLayout(1, 0));

        stopButton = new AwesomeButton(FA_StopCharacter);
        stopButton.addActionListener(this);
        pc.add(stopButton);

        walkButton = new AwesomeButton('\uf051');
        walkButton.setToolTipText("Walk 1 Cycle");
        walkButton.addActionListener(this);
        pc.add(walkButton);

        JButton focusButton = new AwesomeButton(FA_FocusCharacter);
        focusButton.setToolTipText("Focus");
        focusButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                setSpeed(1.0f);
                volumeSlider.setValue(0.0f);
            }
        });
        pc.add(focusButton);
        
        
        JButton pluginsButton = new AwesomeButton(FA_ControlCharacter);
        pluginsButton.setToolTipText("Plugins");
        pluginsButton.addActionListener(new ActionListener() {

            @Override public void actionPerformed(ActionEvent e) {
                new NWindow("Plugins", new PluginPanel(nar)).show(350, 600);
            }

        });
        pc.add(pluginsButton);
        
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        c.ipady = 8;

        p.add(pc, c);
        
        NSlider vs = newVolumeSlider();
        vs.setFont(vs.getFont().deriveFont(Font.BOLD));
        p.add(vs, c);

        NSlider ss = newSpeedSlider();
        ss.setFont(vs.getFont());
        p.add(ss, c);


        c.ipady = 4;

        JPanel pg = new JPanel(new GridLayout(0, 2));
        {
            pg.add(new NSlider(memory.param.inputActivationFactor, "Input Activation", 0.0f, 1.0f), c);
            pg.add(new NSlider(memory.param.conceptActivationFactor, "Concept Activation", 0.0f, 1.0f), c);

            pg.add(new NSlider(memory.param.conceptFireThreshold, "Concepts >", 0.0f, 1.0f), c);
            pg.add(new NSlider(memory.param.decisionThreshold, "Decisions >", 0.0f, 1.0f), c);

            pg.add(new NSlider(memory.param.conceptForgetDurations, "Concept Memory Durations", 0.5f, 20), c);

            tasklinkRate.set(memory.param.taskLinkForgetDurations.get() / memory.param.conceptForgetDurations.get());
            termlinkRate.set( memory.param.termLinkForgetDurations.get() / memory.param.conceptForgetDurations.get() );
            novelRate.set( memory.param.novelTaskForgetDurations.get() / memory.param.conceptForgetDurations.get() );

            pg.add(new NCSlider(tasklinkRate, "TaskLink Memory x", 0.1f, 5f), c);
            pg.add(new NCSlider(termlinkRate, "TermLink Memory x", 0.1f, 5f), c);
            pg.add(new NCSlider(novelRate, "NovelTask Memory x", 0.1f, 5f), c);
        }
        p.add(pg,c);
        
//
//        //JPanel chartPanel = new JPanel(new GridLayout(0,1));
//        {
//            this.chart = new MeterVis(senses, chartHistoryLength);
//            //chartPanel.add(chart);
//                        
//        }
//        
//        c.weighty = 1.0;
//        c.fill = GridBagConstraints.BOTH;        
//        //p.add(new JScrollPane(chartPanel), c);
//        p.add(chart, c);

        /*c.fill = c.BOTH;
        p.add(Box.createVerticalBox(), c);*/
        

        return p;
    }

    class NCSlider extends NSlider {

        public NCSlider(AtomicDouble value, String label, float min, float max) {
            super(value, label, min, max);

        }

        @Override
        public void setValue(float v) {
            super.setValue(v);
            updateFrequencies();
        }
    }


    protected void updateFrequencies() {

        //relative to base freq:
        //  input x
        memory.param.inputsMaxPerCycle.set( 1 );
        //  concept #
        memory.param.conceptsFiredPerCycle.set( 1 );
        //  term #
        memory.param.termLinkMaxReasoned.set(1);

        final float conceptRate = memory.param.conceptForgetDurations.floatValue();

        //  term x
        memory.param.termLinkForgetDurations.set( termlinkRate.get() * conceptRate );
        //  task x
        memory.param.taskLinkForgetDurations.set( tasklinkRate.get() * conceptRate );
        //  novelty x
        memory.param.novelTaskForgetDurations.set( novelRate.get() * conceptRate );
    }

    private NSlider newIntSlider(final AtomicInteger x, final String prefix, int min, int max) {
        final NSlider s = new NSlider(x.intValue(), min, max) {

            @Override
            public String getText() {
                return prefix + ": " + super.getText();
            }

            @Override
            public void setValue(float v) {
                int i = Math.round(v);
                super.setValue(i);
                x.set(i);
            }

            @Override
            public void onChange(float v) {
            }
        };

        return s;
    }

    /** if true, then the speed control allows NAR to run() each iteration with 0 delay.  
     *  otherwise, the minimum delay is 1ms */
    public void setAllowFullSpeed(boolean allowFullSpeed) {
        this.allowFullSpeed = allowFullSpeed;
    }


    @Override
    protected void visibility(boolean appearedOrDisappeared) {

    }

    transient StringBuilder sb = new StringBuilder();

    @Override
    public String getTimeText() {


        Memory.Timing tt = memory.param.getTiming();

        if (sb.length() > 0) sb.setLength(0);

        sb.append('@');

        if ((tt == RealMS) || (tt == Simulation)) {
            sb.append(memory.time() + "|" + memory.timeCycle());
        }
        else {
            sb.append(memory.time());
        }



        if (currentSpeed == 0) {
            sb.append(" - pause");
        } else if (currentSpeed == 1.0) {
            sb.append(" - run max speed");
        } else {
            sb.append(" - run ").append(nar.getMinFramePeriodMS()).append(" ms / frame");
        }
        return sb.toString();

    }
}
