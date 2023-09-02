package net.legendofwar.firecord.jedis.dataset.datakeys;

import net.legendofwar.firecord.jedis.dataset.Bytes;

public class ClassNameLookup {

    // 64kibi classes should be sufficient
    private static KeyLookupTable lookupTable = new KeyLookupTable(KeyGenerator.getLookUpTableKey(new Bytes("classes")), 2);
    
    public static String getClassName(Bytes id){
        return lookupTable.lookUpName(id).asString();
    }

    public static Bytes getId(String className){
        return lookupTable.lookUpId(className);
    }

}
