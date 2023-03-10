package net.legendofwar.firecord.jedis.dataset.dataentry;

import net.legendofwar.firecord.jedis.dataset.dataentry.composite.RList;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RInteger;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RItemStack;

public enum DataType {

    // small
    BOOLEAN(null),
    // numeric
    BYTE(null),
    SHORT(null),
    INTEGER(RInteger.class),
    LONG(null),
    FLOAT(null),
    DOUBLE(null),

    CHAR(null),
    STRING(null),
    UUID(null),
    // mc
    LOCATION(null),
    VECTOR(null),
    
    //large
    ITEMSTACK(RItemStack.class),
    INVENTORY(null),
    BYTEARRAY(null), // generalized QoL

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
    WRAPPER(null),
    OBJECT(null); 

    Class<?> c;

    private DataType(Class<?> c) {
        this.c = c;
    }

    public Class<?> getC() {
        return c;
    }
}
