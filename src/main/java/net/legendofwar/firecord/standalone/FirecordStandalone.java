package net.legendofwar.firecord.standalone;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.tool.NodeType;

public class FirecordStandalone {

    public static void main(String[] args) {

        System.out.println("Firecord - Standalone-Start registred.");

        Firecord.init(NodeType.STANDALONE);

    }

}
