package net.legendofwar.firecord.tool;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class ReadProperties {

    public static String[] readProperties(String file, String[] properties) throws FileNotFoundException, IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream(file));
        String[] data = new String[properties.length];
        for (int i = 0; i < properties.length; i++) {
            data[i] = prop.getProperty(properties[i]);
        }
        return data;
    }

}
