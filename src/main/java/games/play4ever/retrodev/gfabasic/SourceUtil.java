package games.play4ever.retrodev.gfabasic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Source-file related utility code.
 *
 * @author Marcel Schoen
 */
public class SourceUtil {

    /**
     * Make sure the line endings in the given file are all 2-bytes / 0x0D 0x0A / CR LF.
     * Otherwise, the GFA Editor will refuse to read the file.
     *
     * @param file
     */
    public static void fixCrlfBytes(File file) {
        try {
            FileInputStream fin = new FileInputStream(file);
            byte[] fileData = fin.readAllBytes();
            byte[] fixedData = new byte[fileData.length * 2];
            int finalLength = 0;
            for (int i = 0; i < fileData.length; i++) {
                byte value = fileData[i];
                boolean addCR = false;
                if (value == 0x0A) {
                    if (i > 0 && fileData[i - 1] != 0x0D) {
                        addCR = true;
                    }
                }
                if (addCR) {
                    fixedData[finalLength++] = 0x0D;
                }
                fixedData[finalLength++] = value;
            }
            System.out.println(">> Original file length: " + file.length() + ", fixed: " + finalLength);
            if (file.length() != finalLength) {
                System.out.println(">> Replacing CRLF-fixed file...");
                FileOutputStream fout = new FileOutputStream(file);
                fout.write(fixedData, 0, finalLength);
                fout.flush();
            }
        } catch (Exception e) {
            throw new RuntimeException(">> Failed to process file: " + file.getAbsolutePath(), e);
        }
    }
}
