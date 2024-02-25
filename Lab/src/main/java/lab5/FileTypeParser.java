package lab5;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class FileTypeParser {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java FileTypeParser <filename>");
            System.exit(1);
        }
        String filename = args[0];
        try {
            getFileType(filename);
        } catch (IOException e) {
            System.out.println("Failed or interrupted when doing the I/O operations");
        }


    }

    static void getFileType(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new IOException("File does not exist.");
        }
        String type = "Unknown";
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[4];
            fis.read(header);
            if (header[0] == (byte) 0x89 && header[1] == (byte) 0x50 && header[2] == (byte) 0x4E && header[3] == (byte) 0x47) {
                type = "PNG";
            }
            if (header[0] == (byte) 0x50 && header[1] == (byte) 0x4B && header[2] == (byte) 0x03 && header[3] == (byte) 0x04) {
                type = "ZIP or jar";
            }
            if (header[0] == (byte) 0xca && header[1] == (byte) 0xfe && header[2] == (byte) 0xba && header[3] == (byte) 0xbe) {
                type = "Class";
            }
            System.out.println("Filename: " + filename);
            System.out.println("File header(hex): " + Arrays.toString(header));
            System.out.println("File type: " + type);
        }
    }
}
