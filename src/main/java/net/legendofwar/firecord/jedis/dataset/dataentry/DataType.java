package net.legendofwar.firecord.jedis.dataset.dataentry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.jedis.dataset.dataentry.composite.RList;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.Invalid;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RBoolean;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RByte;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RByteArray;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RChar;
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

public enum DataType {

    // small
    BOOLEAN(RBoolean.class),
    CHAR(RChar.class),
    STRING(RString.class),
    UUID(RUUID.class),
    // numeric
    BYTE(RByte.class),
    SHORT(RShort.class),
    INTEGER(RInteger.class),
    LONG(RLong.class),
    FLOAT(RFloat.class),
    DOUBLE(RDouble.class),

    // mc
    LOCATION(null, NodeType.SPIGOT),
    VECTOR(RVector.class, NodeType.SPIGOT),

    // replaces mc values on other nodes,
    // although sometimes null is used too
    INVALID(Invalid.class),

    // large
    ITEMSTACK(RItemStack.class, NodeType.SPIGOT),
    INVENTORY(null, NodeType.SPIGOT),

    BYTEARRAY(RByteArray.class), // generalized QoL
    JSON(RJson.class),

    // composite types

    // corresponds to a native redis type
    LIST(RList.class),
    SET(null),
    SORTEDSET(null),
    MAP(null),

    // java types we emulate on redis
    QUEUE(null),
    STACK(null),

    // for later use in custom objects
    WRAPPER(RWrapper.class),
    OBJECT(null);

    final Class<?> c;
    // set if this entry type should not be loaded on all nodes that are not this
    // type
    final NodeType exclusive;

    boolean initialized = false;
    Object defaultValue;

    private DataType(Class<?> c) {
        this(c, null);
    }

    private DataType(Class<?> c, NodeType exclusive) {
        this.c = c;
        this.exclusive = exclusive;
    }

    public boolean canBeLoaded() {
        return this.exclusive == null || this.exclusive == Firecord.getNodeType();
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
        for (DataType dt : DataType.values()) {
            if (dt.getC() != null && dt.getC().equals(c)) {
                return dt;
            }
        }
        return null;
    }

}
