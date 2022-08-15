package games.play4ever.retrodev.gfabasic;

import games.play4ever.retrodev.hatari.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Allows to test emulator and GFA basic wrapper integration interactively.
 *
 * @author Marcel Schoen
 */
public class Main {

    public static void main(String[] args) throws Exception {

        try {
            HatariWrapper.prepare(new File("build"), TOS.tos206);

            JFrame Alert_Frame = new JFrame("Test Window");
            Alert_Frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            Create_Popup(Alert_Frame);
            Alert_Frame.setSize(400, 200);
            Alert_Frame.setLocationRelativeTo(null);
            Alert_Frame.setVisible(true);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void Create_Popup(final JFrame Alert_Frame) {
        JPanel Alert_Panel = new JPanel();
        LayoutManager Alert_Layout = new FlowLayout();
        Alert_Panel.setLayout(Alert_Layout);

        JButton Alert_Button = new JButton("Build!");
        Alert_Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GfaBasicWrapper.compileGfaProgram(new File("src/test/gfabasic", "SOURCE.LST"));
            }
        });

        JButton Test_Button = new JButton("Test!");
        Test_Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GfaBasicWrapper.testProgram(MachineType.ste, Memory.mb1, ScreenMode.low, true);
            }
        });

        JButton Exit_Button = new JButton("Exit");
        Exit_Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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

