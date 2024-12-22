package net.legendofwar.firecord.jedis.dataset.datakeys;

import java.util.Map;
import java.util.stream.Collectors;

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

    public static void migrate(String newClassName, String oldClassName){
        lookupTable.migrate(new Bytes(newClassName), new Bytes(oldClassName));
    }

    public static Map<Long, String> getCache(){
        return lookupTable.cache.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().decodeNumber(),
                entry -> entry.getValue().asString()
            ));
    }

}
