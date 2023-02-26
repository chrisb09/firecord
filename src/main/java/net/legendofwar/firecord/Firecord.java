package net.legendofwar.firecord;

import java.io.File;
import java.io.IOException;

import net.legendofwar.firecord.tool.FileIO;
import net.legendofwar.firecord.tool.NodeType;

public class Firecord {

    private static String id = null;
    private static NodeType nodeType = NodeType.STANDALONE;

    public static String getId() {
        return id;
    }

    public static NodeType getNodeType() {
        return nodeType;
    }

    public static boolean init(String id, NodeType nodeType) {
        Firecord.id = id;
        Firecord.nodeType = nodeType;
        return id != null && id.length() != 0;
    }

    public static boolean init(NodeType nodeType) {
        return init(loadId(new File("id")), nodeType);
    }

    private static String loadId(File file) {
        String newId = null;
        try {
            newId = FileIO.readFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newId;
    }

}
