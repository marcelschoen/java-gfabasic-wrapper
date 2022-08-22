package games.play4ever.retrodev.gfabasic;

import com.sun.jna.platform.DesktopWindow;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import games.play4ever.retrodev.hatari.*;
import games.play4ever.retrodev.util.FileUtil;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;

/**
 * Java wrapper which allows to run the GFA BASIC editor to convert ascii source files into proper ".GFA"
 * sources, and then run the GFA compiler in order to compile and link them. This all happens in a Hatari
 * emulator instance, using a local host directory mounted as a GEMDOS drive in the emulator, so that the
 * source file can be edited with any given editor on the host.
 * <p>
 * Essentially, this library is meant to facilitate GFA-BASIC programming for the Atari ST computers on
 * a modern PC, using any modern editor or IDE of your choice (and possibly even building your very own
 * Atari ST IDE).
 *
 * @author Marcel Schoen
 */
public class GfaBasicWrapper {

    /**
     * Used to send key press events to the emulator window.
     */
    private static Robot robot;
    /**
     * The directory where the GFA files are compiled.
     */
    private static File buildDirectory = new File("./build");

    static {
        try {
            robot = new Robot();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize robot API");
        }
    }

    /**
     * Allows to set the directory where the virtual harddisc drive with the GFA BASIC
     * editor and compiler will be created (and then mounted in the emulator). Ultimately,
     * the compiled program will end up in this directory too.
     *
     * @param buildDirectory The build directory.
     */
    public static void setBuildDirectory(File buildDirectory) {
        if (buildDirectory == null) {
            throw new IllegalArgumentException("'null' buildDirectory parameter not allowed!");
        }
        if (!buildDirectory.exists()) {
            System.out.println("GFA build directory does not exist yet, create: " + buildDirectory.getAbsolutePath());
            buildDirectory.mkdirs();
        }
        if (!buildDirectory.isDirectory() || !buildDirectory.canRead()) {
            throw new IllegalArgumentException("Build directory either not a directory or not accessible: " + buildDirectory.getAbsolutePath());
        }
        GfaBasicWrapper.buildDirectory = buildDirectory;
    }

    /**
     * Starts the Hatari emulator, opens the GFA BASIC editor and then executes the
     * given GFA BASIC program directly in the GFA editor (interpreted).
     *
     * @param lstSourceToRun The ASCII source file.
     * @param instance The instance configuration to use for the run.
     */
    public static void runGfaProgram(File lstSourceToRun, HatariInstance instance) {


        try {
            System.out.println(">> Start emulator with LST file to execute in GFA editor: " + lstSourceToRun.getAbsolutePath());
            File runtimeBuildFolder = getOrCreateRuntimeBuildFolder();
            System.out.println(">> Runtime build folder: " + runtimeBuildFolder.getAbsolutePath() + " / exists: " + runtimeBuildFolder.exists());
            DesktopWindow emulatorWindow = HatariWrapper.startEmulator(instance,
                    null,
                    runtimeBuildFolder);

            Thread.sleep(500);
            // Press SPACE to speed up start
            pressKeys(robot, emulatorWindow.getHWND(), KeyEvent.VK_SPACE);
            // Wait a little to give TOS desktop time to become responsive
            Thread.sleep(10000);

            if(emulatorWindow == null) {
                emulatorWindow = new DummyDesktopWindow();
            }

//            User32.INSTANCE.MoveWindow(emulatorWindow.getHWND(), 50, 50,
//                    emulatorWindow.getLocAndSize().width, emulatorWindow.getLocAndSize().height, true);

            System.out.println(">> Clean up build folder...");
            new File(runtimeBuildFolder, "SOURCE.LST").delete();

            System.out.println(">> Copy runtime build folder...");
            File targetFile = new File(runtimeBuildFolder, "SOURCE.LST");
            FileUtil.copyFileTo(lstSourceToRun, targetFile);
            System.out.println(">> Fix CRLF in source file...");
            SourceUtil.fixCrlfBytes(targetFile);

            // *****************************************************************************************
            // Step 1: Start GFA BASIC Editor to load text file and save as ".GFA" file
            // *****************************************************************************************

            // Type "O" to open a file
            pressKeys(robot, emulatorWindow.getHWND(), KeyEvent.VK_O);
            clearInputFieldWithBackspaces(emulatorWindow);

            // Type "GFABASIC.PRG" "ENTER" to open the GFA BASIC editor
            pressKeys(robot, emulatorWindow.getHWND(),
                    KeyEvent.VK_G, KeyEvent.VK_F, KeyEvent.VK_A, KeyEvent.VK_B, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_I, KeyEvent.VK_C,
                    KeyEvent.VK_PERIOD, KeyEvent.VK_P, KeyEvent.VK_R, KeyEvent.VK_G, KeyEvent.VK_ENTER);

            // Wait a little to give the emulator time to finish loading the GFA BASIC editor
            Thread.sleep(500);

            // Type F2 to open "Merge" screen
            pressKeys(robot, emulatorWindow.getHWND(), KeyEvent.VK_F2);
            clearInputFieldWithBackspaces(emulatorWindow);

            // Type "SOURCE.LST"
            pressKeys(robot, emulatorWindow.getHWND(),
                    KeyEvent.VK_S, KeyEvent.VK_O, KeyEvent.VK_U, KeyEvent.VK_R, KeyEvent.VK_C, KeyEvent.VK_E,
                    KeyEvent.VK_PERIOD, KeyEvent.VK_L, KeyEvent.VK_S, KeyEvent.VK_T,
                    KeyEvent.VK_ENTER);

            // Wait a little to give the emulator time to finish loading the LST file
            Thread.sleep(500);

            // Type Shift + F10 to run the program
            pressKeysTogether(robot, emulatorWindow.getHWND(), KeyEvent.VK_SHIFT, KeyEvent.VK_F10);

            System.out.println(">> Program started...");

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to send command to emulator: " + ex, ex);
        }

    }

    /**
     * Starts the Hatari emulator and then does the following:
     * <ul>
     *     <li>Copy the given source file into the build folder as "SOURCE.LST" (also fixing CRLF line endings)</li>
     *     <li>Open the GFA BASIC editor</li>
     *     <li>Read the text source file "SOURCE.LST" using the "Merge" function</li>
     *     <li>Save the file as "SOURCE.GFA" and quit the GFA BASIC editor</li>
     *     <li>Open the GFA BASIC compiler ("MENU.PRG")</li>
     *     <li>Select the "SOURCE.GFA" file</li>
     *     <li>Run the compiler followed by the linker</li>
     * </ul>
     * If everything goes well, the result is a new "TEST.PRG" file in the build folder.
     *
     * @param lstSourceToConvert The ASCII source file.
     */
    public static void compileGfaProgram(File lstSourceToConvert) {

        HatariInstance building = new HatariInstance("building",
                true,
                true,
                true,
                true,
                false,
                true,
                MachineType.ste,
                TOS.tos206,
                ScreenMode.high,
                Memory.mb4);

        try {
            System.out.println(">> Start emulator with LST file to convert in GFA editor: " + lstSourceToConvert.getAbsolutePath());
            File runtimeBuildFolder = getOrCreateRuntimeBuildFolder();
            System.out.println(">> Runtime build folder: " + runtimeBuildFolder.getAbsolutePath() + " / exists: " + runtimeBuildFolder.exists());
            DesktopWindow emulatorWindow = HatariWrapper.startEmulator(building,
                    null,
                    runtimeBuildFolder);
            // Wait a little to give TOS desktop time to become responsive
            Thread.sleep(1500);
            if(emulatorWindow == null) {
                emulatorWindow = new DummyDesktopWindow();
            }

//            User32.INSTANCE.MoveWindow(emulatorWindow.getHWND(), 50, 50,
//                    emulatorWindow.getLocAndSize().width, emulatorWindow.getLocAndSize().height, true);

            System.out.println(">> Clean up build folder...");
            new File(runtimeBuildFolder, "SOURCE.LST").delete();
            new File(runtimeBuildFolder, "SOURCE.BAK").delete();
            File sourceGfa = new File(runtimeBuildFolder, "SOURCE.GFA");
            sourceGfa.delete();
            File testPrg = new File(runtimeBuildFolder, "TEST.PRG");
            testPrg.delete();

            System.out.println(">> Copy runtime build folder...");
            File targetFile = new File(runtimeBuildFolder, "SOURCE.LST");
            FileUtil.copyFileTo(lstSourceToConvert, targetFile);
            System.out.println(">> Fix CRLF in source file...");
            SourceUtil.fixCrlfBytes(targetFile);

            // *****************************************************************************************
            // Step 1: Start GFA BASIC Editor to load text file and save as ".GFA" file
            // *****************************************************************************************

            // Type "O" to open a file
            pressKeys(robot, emulatorWindow.getHWND(), KeyEvent.VK_O);
            clearInputFieldWithBackspaces(emulatorWindow);

            // Type "GFABASIC.PRG" "ENTER" to open the GFA BASIC editor
            pressKeys(robot, emulatorWindow.getHWND(),
                    KeyEvent.VK_G, KeyEvent.VK_F, KeyEvent.VK_A, KeyEvent.VK_B, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_I, KeyEvent.VK_C,
                    KeyEvent.VK_PERIOD, KeyEvent.VK_P, KeyEvent.VK_R, KeyEvent.VK_G, KeyEvent.VK_ENTER);

            // Wait a little to give the emulator time to finish loading the GFA BASIC editor
            Thread.sleep(500);

            // Type F2 to open "Merge" screen
            pressKeys(robot, emulatorWindow.getHWND(), KeyEvent.VK_F2);
            clearInputFieldWithBackspaces(emulatorWindow);

            // Type "SOURCE.LST"
            pressKeys(robot, emulatorWindow.getHWND(),
                    KeyEvent.VK_S, KeyEvent.VK_O, KeyEvent.VK_U, KeyEvent.VK_R, KeyEvent.VK_C, KeyEvent.VK_E,
                    KeyEvent.VK_PERIOD, KeyEvent.VK_L, KeyEvent.VK_S, KeyEvent.VK_T,
                    KeyEvent.VK_ENTER);

            // Wait a little to give the emulator time to finish loading the LST file
            waitForFileToBeWritten(sourceGfa);
//            Thread.sleep(500);

            // Type Shift + F1 to open "Save" screen
            pressKeysTogether(robot, emulatorWindow.getHWND(), KeyEvent.VK_SHIFT, KeyEvent.VK_F1);

            // Type "Enter" to confirm and save as GFA file
            pressKeys(robot, emulatorWindow.getHWND(), KeyEvent.VK_ENTER);

            // Wait a second before quitting the GFA editor, in order for the GFA file to be saved
            Thread.sleep(1000);

            // Quit the GFA BASIC editor
            pressKeysTogether(robot, emulatorWindow.getHWND(), KeyEvent.VK_SHIFT, KeyEvent.VK_F3);
            pressKeys(robot, emulatorWindow.getHWND(), KeyEvent.VK_ENTER);

            // *****************************************************************************************
            // Step 2: Start GFA Compiler Menu and run compiler and linker
            // *****************************************************************************************

            // Type "O" to open a file again
            Thread.sleep(1000);
            pressKeys(robot, emulatorWindow.getHWND(), KeyEvent.VK_O);
            clearInputFieldWithBackspaces(emulatorWindow);

            // Type "MENU.PRG" "ENTER" to open the GFA BASIC compiler
            pressKeys(robot, emulatorWindow.getHWND(),
                    KeyEvent.VK_M, KeyEvent.VK_E, KeyEvent.VK_N, KeyEvent.VK_U,
                    KeyEvent.VK_PERIOD, KeyEvent.VK_P, KeyEvent.VK_R, KeyEvent.VK_G, KeyEvent.VK_ENTER);

            // Wait a little to give the emulator time to finish loading the GFA BASIC compiler
            Thread.sleep(500);

            // Open the file selection dialog
            pressKeysTogether(robot, emulatorWindow.getHWND(), KeyEvent.VK_CONTROL, KeyEvent.VK_S);
            clearInputFieldWithBackspaces(emulatorWindow);

            // Select the GFA source file to compile
            pressKeys(robot, emulatorWindow.getHWND(),
                    KeyEvent.VK_S, KeyEvent.VK_O, KeyEvent.VK_U, KeyEvent.VK_R, KeyEvent.VK_C, KeyEvent.VK_E,
                    KeyEvent.VK_PERIOD, KeyEvent.VK_G, KeyEvent.VK_F, KeyEvent.VK_A,
                    KeyEvent.VK_ENTER);
            Thread.sleep(500);

            // Compile
            pressKeysTogether(robot, emulatorWindow.getHWND(), KeyEvent.VK_CONTROL, KeyEvent.VK_C);

            // Wait a little to make sure the compiler has completed its work.
            waitForFileToBeWritten(testPrg);
//            Thread.sleep(2000);

            // Link
            pressKeysTogether(robot, emulatorWindow.getHWND(), KeyEvent.VK_CONTROL, KeyEvent.VK_L);

            // Wait a little to make sure the linker has completed its work.
            Thread.sleep(2000);

            System.out.println(">> Build successful, compiled PRG file created: " + testPrg.getAbsolutePath());

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to send command to emulator: " + ex, ex);
        } finally {
            HatariWrapper.stopEmulator(building);
        }
    }

    private static void waitForFileToBeWritten(File file) throws Exception {
        int remainsSameCounter = 0;
        while(remainsSameCounter == 5) {
            long length = file.length();
            Thread.sleep(20);
            if(file.length() == length) {
                remainsSameCounter ++;
            } else {
                remainsSameCounter = 0;
            }
        }
    }

    private static void clearInputFieldWithBackspaces(DesktopWindow emulatorWindow) {
        // In case there were unwanted "o" key presses, clear text field
        for (int i = 0; i < 10; i++) {
            pressKeys(robot, emulatorWindow.getHWND(), KeyEvent.VK_BACK_SPACE);
        }
    }

    public static void testProgram(MachineType machine,
                                   Memory memory,
                                   ScreenMode mode,
                                   boolean blitter) {

        HatariInstance testing = new HatariInstance("testing",
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

        System.out.println(">> Start emulator to test compiled program....");
        DesktopWindow emulatorWindow = HatariWrapper.startEmulator(testing,
                null,
                null);

        // NOTE: Using Robot keyboard input to open the freshly compiled "TEST.PRG" may
        // not work well here, because this emulator instance is running at original hardware
        // speed; this makes it difficult to make the keyboard input work reliably.
    }

    private static File getOrCreateRuntimeBuildFolder() {
        File runtimeFolder = new File("build", "drivec");
        runtimeFolder.mkdirs();
        File gfaEditor = new File(runtimeFolder, "GFABASIC.PRG");
        System.out.println(">> GFABASIC.PRG: " + gfaEditor.getAbsolutePath() + " / exists: " + gfaEditor.exists());
        if (!gfaEditor.exists()) {
            FileUtil.copyDirectory(new File("src/main/resources/gfabasic/hatari_hdd"), runtimeFolder);
        }
        return runtimeFolder;
    }

    /**
     * Performs some key presses in the given window, using the Java Robot API. This method
     * will first invoke "keyPress()" and then "keyRelease()" on each key, one by one. This
     * method should be used to simulate the user typing a command or a file name.
     *
     * @param robot  The Robot instance.
     * @param window The window (will be brought to the foreground).
     * @param keys   The list of key codes to send to the window.
     */
    private static void pressKeys(Robot robot, WinDef.HWND window, int... keys) {
        try {
            if(window != null) {
                User32.INSTANCE.SetFocus(window);
                User32.INSTANCE.SetForegroundWindow(window);
            }
            for (int key : keys) {
                robot.keyPress(key);
                robot.keyRelease(key);
                Thread.sleep(50);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("** Failed to enter keyboard presses **");
        } finally {
            Arrays.stream(keys).forEach(k -> robot.keyRelease(k));
        }
    }

    /**
     * Performs some key presses in the given window, using the Java Robot API. This method
     * will first invoke "keyPress()" on all given keys (effectively pressing the all at the same time),
     * and then "keyRelease()" on all of them. This should be used to simulate shortcut keypressed,
     * like pressing "ALT_GR" together with "l" to load a memory shortcut.
     *
     * @param robot  The Robot instance.
     * @param window The window (will be brought to the foreground).
     * @param keys   The list of key codes to send to the window.
     */
    private static void pressKeysTogether(Robot robot, WinDef.HWND window, int... keys) {
        try {
            if(window != null) {
                User32.INSTANCE.SetFocus(window);
                User32.INSTANCE.SetForegroundWindow(window);
            }
            for (int key : keys) {
                robot.keyPress(key);
                //Thread.sleep(50);
            }
            Thread.sleep(50);
        } catch (Exception e) {
            throw new RuntimeException("** Failed to enter keyboard presses **");
        } finally {
            Arrays.stream(keys).forEach(k -> robot.keyRelease(k));
        }
    }
}
