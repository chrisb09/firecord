package net.legendofwar.firecord.jedis.dataset.dataentry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.jedis.dataset.dataentry.composite.RList;
import net.legendofwar.firecord.jedis.dataset.dataentry.composite.RMap;
import net.legendofwar.firecord.jedis.dataset.dataentry.composite.RSet;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RBoolean;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RByte;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RByteArray;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RCharacter;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RDouble;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RFloat;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RInteger;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RItemStack;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RJson;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RLong;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RShort;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RString;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RUUID;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RVector;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RWrapper;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.SimpleData;
import net.legendofwar.firecord.tool.NodeType;

public class DataType {

    // small
    public final static DataType BOOLEAN = new DataType(RBoolean.class);
    public final static DataType CHARACTER = new DataType(RCharacter.class);
    public final static DataType STRING = new DataType(RString.class);
    public final static DataType UUID = new DataType(RUUID.class);

    // numeric
    public final static DataType BYTE = new DataType(RByte.class);
    public final static DataType SHORT = new DataType(RShort.class);
    public final static DataType INTEGER = new DataType(RInteger.class);
    public final static DataType LONG = new DataType(RLong.class);
    public final static DataType FLOAT = new DataType(RFloat.class);
    public final static DataType DOUBLE = new DataType(RDouble.class);

    // mc
    public final static DataType LOCATION = new DataType(null, NodeType.SPIGOT);
    public final static DataType VECTOR = new DataType(RVector.class, NodeType.SPIGOT);

    // replaces mc values on other nodes,
    // although sometimes null is used too
    public final static DataType INVALID = new DataType(Invalid.class);

    // large
    public final static DataType ITEMSTACK = new DataType(RItemStack.class, NodeType.SPIGOT);
    public final static DataType INVENTORY = new DataType(null, NodeType.SPIGOT);

    public final static DataType BYTEARRAY = new DataType(RByteArray.class); // generalized QoL
    public final static DataType JSON = new DataType(RJson.class);

    // composite types

    // corresponds to a native redis type
    public final static DataType LIST = new DataType(RList.class);
    public final static DataType SET = new DataType(RSet.class);
    public final static DataType SORTEDSET = new DataType(null);
    public final static DataType MAP = new DataType(RMap.class);

    // java types we emulate on redis
    public final static DataType QUEUE = new DataType(null);
    public final static DataType STACK = new DataType(null);

    // for later use in custom objects
    public final static DataType WRAPPER = new DataType(RWrapper.class);
    public final static DataType OBJECT = new DataType("Object", null);
    public final static DataType CLASS = new DataType("Class", null);

    // not used so far i guess
    public final static DataType NULL = new DataType("Null", null);


    private static DataType[] values = {
        BOOLEAN, CHARACTER, STRING, UUID, BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE,
        LOCATION, VECTOR, INVALID, ITEMSTACK, INVENTORY, BYTEARRAY, JSON,
        LIST, SET, SORTEDSET, MAP, QUEUE, STACK, WRAPPER, OBJECT, CLASS
    };

    private static HashMap<String, DataType> valueOf = new HashMap<>();

    static {
        for (DataType dt : values){
            if (dt.name() != null) {
                valueOf.put(dt.name(), dt);
            }
        }
    }

    public static DataType[] values(){
        return values;
    }

    public static DataType valueOf(String name) {
        if (valueOf.containsKey(name)){
            return valueOf.get(name);
        }
        return null;
    }



    final Class<?> c;
    // set if this entry type should not be loaded on all nodes that are not this
    // type
    final NodeType exclusive;

    final private String name;
    boolean initialized = false;
    Object defaultValue;

    DataType(Class<?> c){
        this(null, c);
    }

    DataType(String name, Class<?> c) {
        this(name, c, NodeType.ANY);
    }

    DataType(Class<?> c, NodeType exclusive){
        this(null, c, exclusive);
    }

    DataType(String name, Class<?> c, NodeType exclusive) {
        if (name == null) {
            if (c != null) {
                this.name = c.getSimpleName();
            } else {
                this.name = null;
            }
        } else {
            this.name = name;
        }
        this.c = c;
        this.exclusive = exclusive;
    }

    public String name(){
        return this.name;
    }

    @Override
    public String toString(){
        return this.name();
    }

    public boolean canBeLoaded() {
        return this.exclusive.includes(Firecord.getNodeType());
    }

    public final Class<?> getC() {
        return c;
    }

    public Object getDefaultValue() {
        if (!initialized) {
            this.initialized = true;
            if (canBeLoaded()) {
                Object dv = null;
                try {
                    if (c != null && SimpleData.class.isAssignableFrom(c)) {
                        Field f = c.getDeclaredField("DEFAULT_VALUE");
                        f.setAccessible(true);
                        dv = f.get(null); // static field access
                    }
                } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                    e.printStackTrace();
                    System.out.println(this.name());
                    System.out.println("Declared Fields:");
                    Arrays.asList(c.getDeclaredFields()).forEach(f -> System.out
                            .print(f.getName() + (Modifier.isStatic(f.getModifiers()) ? "[static]" : "") + ", "));
                    System.out.println();
                    dv = null;
                }
                this.defaultValue = dv;
            } else {
                
                this.defaultValue = null;
            }
        }
        return this.defaultValue;
    }

    public static DataType getByC(Class<?> c) {
        if (c == null) {
            return null;
        }
        for (DataType dt : DataType.values()) {
            if (dt.getC() != null && dt.getC().equals(c)) {
                return dt;
            }
        }
        return null;
    }

}
