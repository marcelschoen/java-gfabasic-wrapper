package games.play4ever.retrodev.gfabasic;

import games.play4ever.retrodev.hatari.*;
import games.play4ever.retrodev.util.FileUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Allows to test emulator and GFA basic wrapper integration interactively.
 *
 * @author Marcel Schoen
 */
public class Main {

    public static void main(String[] args) throws Exception {

        if(args == null || args.length == 0) {
            printUsage();
            System.exit(-1);
        }

        try {

            List<String> params = Arrays.asList(args);
            if(params.contains("-gui")) {
                showGUI();
            } else if(params.contains("-d") && params.contains("-s")) {
                File buildDirectory = null;
                File sourceFile = null;
                for (int i = 0; i < params.size(); i++) {
                    String param = params.get(i);
                    if (param.equals("-d")) {
                        buildDirectory = new File(params.get(i + 1));
                        GfaBasicWrapper.setBuildDirectory(buildDirectory);
                    }
                    if (param.equals("-s")) {
                        sourceFile = new File(params.get(i + 1));
                    }
                }
                if (buildDirectory != null && sourceFile != null) {
                    if(!sourceFile.exists() || !sourceFile.isFile() && !sourceFile.canRead()) {
                        System.err.println("Cannot read source file " + sourceFile.getAbsolutePath());
                        System.exit(-1);
                    }
                    HatariWrapper.prepare(buildDirectory, TOS.tos206);
                    GfaBasicWrapper.compileGfaProgram(sourceFile);
                }
            } else {
                printUsage();
            }

        } catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage 1: java -jar gfabasic-wrapper-<version>.jar -d <build directory path> -s <source file path>");
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

