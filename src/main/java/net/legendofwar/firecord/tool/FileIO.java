package net.legendofwar.firecord.tool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileIO {

    public static String readFile(File f) throws IOException {
        FileReader reader = new FileReader(f);
        char[] chars = new char[(int) f.length()];
        reader.read(chars);
        String inhalt = new String(chars);
        reader.close();
        return inhalt;
    }

    public static boolean writeInFile(File f, String s) throws IOException {

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(f));
            writer.write(s);
            writer.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            if (writer != null)
                writer.close();
            return false;
        }

    }

}
