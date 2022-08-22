package games.play4ever.retrodev.gfabasic;

import games.play4ever.retrodev.hatari.*;
import games.play4ever.retrodev.util.FileUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Allows to test emulator and GFA basic wrapper integration interactively.
 *
 * @author Marcel Schoen
 */
public class Main {

    private static String ARG_BUILDDIR_PATH = "-d";
    private static String ARG_SOURCEFILE_PATH = "-s";
    private static String ARG_GUI = "-gui";
    private static String ARG_TASK = "-task";

    private static String TASK_COMPILE = "compile";
    private static String TASK_RUN = "run";

    public static void main(String[] args) throws Exception {

        if(args == null || args.length == 0) {
            printUsage();
            System.exit(-1);
        }

        try {

            Map<String, String> params = getArgumentsAsMap(args);
            if(params.containsKey(ARG_GUI)) {
                showGUI();
            } else if(params.containsKey(ARG_BUILDDIR_PATH) && params.containsKey(ARG_SOURCEFILE_PATH)) {
                if(!params.containsKey(ARG_TASK)) {
                    System.err.println("ERROR: Need to specify '-task' argument, either 'run' or 'compile'.");
                    printUsage();
                    System.exit(-1);
                }
                File buildDirectory = null;
                File sourceFile = null;
                String task = "";

                if(params.containsKey(ARG_BUILDDIR_PATH)) {
                    buildDirectory = new File(params.get(ARG_BUILDDIR_PATH));
                    GfaBasicWrapper.setBuildDirectory(buildDirectory);
                }
                if(params.containsKey(ARG_SOURCEFILE_PATH)) {
                    sourceFile = new File(params.get(ARG_SOURCEFILE_PATH));
                }
                if(params.containsKey(ARG_TASK)) {
                    task = params.get(ARG_TASK);
                }

                if(!(task.equals(TASK_RUN) || task.equals(TASK_COMPILE))) {
                    System.err.println("ERROR: '-task' argument '" + task + "' must be either 'run' or 'compile'.");
                    printUsage();
                    System.exit(-1);
                }
                if (buildDirectory != null && sourceFile != null) {
                    if(!sourceFile.exists() || !sourceFile.isFile() && !sourceFile.canRead()) {
                        System.err.println("Cannot read source file " + sourceFile.getAbsolutePath());
                        System.exit(-1);
                    }
                    HatariWrapper.prepare(buildDirectory, TOS.tos206);
                    if(task.equals(TASK_COMPILE)) {
                        doCompileProgram(params);
                    } else {
                        doRunProgram(params);
                    }
                }
            } else {
                printUsage();
            }

        } catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Runs the GFA BASIC program directly in the GFA BASIC editor.
     *
     * @param argsAsMap
     * @throws Exception
     */
    private static void doRunProgram(Map<String, String> argsAsMap) throws Exception {
        File buildDirectory = new File(argsAsMap.get(ARG_BUILDDIR_PATH));
        GfaBasicWrapper.setBuildDirectory(buildDirectory);
        HatariWrapper.prepare(buildDirectory, TOS.tos206);

        // TODO Configure instance based on commandline arguments
        HatariInstance running = new HatariInstance("building",
                true,
                false,
                true,
                true,
                true,
                true,
                MachineType.ste,
                TOS.tos206,
                ScreenMode.low,
                Memory.mb1);

        File sourceFile = new File(argsAsMap.get(ARG_SOURCEFILE_PATH));
        GfaBasicWrapper.runGfaProgram(sourceFile, running);
    }

    /**
     * Compile and link the given GFA BASIC program.
     *
     * @param argsAsMap
     * @throws Exception
     */
    private static void doCompileProgram(Map<String, String> argsAsMap) throws Exception {
        File buildDirectory = new File(argsAsMap.get(ARG_BUILDDIR_PATH));
        GfaBasicWrapper.setBuildDirectory(buildDirectory);
        HatariWrapper.prepare(buildDirectory, TOS.tos206);

        File sourceFile = new File(argsAsMap.get(ARG_SOURCEFILE_PATH));
        GfaBasicWrapper.compileGfaProgram(sourceFile);
    }

    private static Map<String, String> getArgumentsAsMap(String ... args) {
        Map<String, String> argsMap = new HashMap<>();
        try {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.trim().startsWith("-")) {
                    // it's a key
                    argsMap.put(arg, args[i + 1]);
                    i++;
                }
            }
        } catch(Exception e) {
            printUsage();
        }
        return argsMap;
    }

    private static void printUsage() {
        System.out.println("Usage 1: java -jar gfabasic-wrapper-<version>.jar -task [compile|run] -d <build directory path> -s <source file path>");
        System.out.println("");
        System.out.println("Usage 2: java -jar gfabasic-wrapper-<version>.jar -gui");
        System.out.println("");
    }

    private static void showGUI() throws Exception {
        JFrame Alert_Frame = new JFrame("Test Window");
        Alert_Frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Create_Popup(Alert_Frame);
        Alert_Frame.setSize(400, 200);
        Alert_Frame.setLocationRelativeTo(null);
        Alert_Frame.setVisible(true);
    }

    private static void Create_Popup(final JFrame Alert_Frame) {
        JPanel Alert_Panel = new JPanel();
        LayoutManager Alert_Layout = new FlowLayout();
        Alert_Panel.setLayout(Alert_Layout);
        File buildDirectory = new File("build");

        JButton Alert_Button = new JButton("Build!");
        Alert_Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GfaBasicWrapper.setBuildDirectory(buildDirectory);
                HatariWrapper.prepare(buildDirectory, TOS.tos206);
                GfaBasicWrapper.compileGfaProgram(new File("src/test/gfabasic", "SOURCE.LST"));
            }
        });

        JButton Test_Button = new JButton("Test!");
        Test_Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GfaBasicWrapper.setBuildDirectory(buildDirectory);
                HatariWrapper.prepare(buildDirectory, TOS.tos206);
                GfaBasicWrapper.testProgram(MachineType.ste, Memory.mb1, ScreenMode.low, true);
            }
        });

        JButton Exit_Button = new JButton("Exit");
        Exit_Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println(">> Stop, shut down emulator.");
                HatariWrapper.stopEmulators();
                System.exit(0);
            }
        });

        Alert_Panel.add(Alert_Button);
        Alert_Panel.add(Test_Button);
        Alert_Panel.add(Exit_Button);
        Alert_Frame.getContentPane().add(Alert_Panel, BorderLayout.CENTER);
    }
}

