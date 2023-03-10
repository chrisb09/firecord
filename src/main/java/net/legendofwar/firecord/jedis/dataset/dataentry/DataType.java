package net.legendofwar.firecord.jedis.dataset.dataentry;

import net.legendofwar.firecord.jedis.dataset.dataentry.composite.RList;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RBoolean;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RByteArray;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RDouble;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RInteger;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RItemStack;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RString;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RVector;

public enum DataType {

    // small
    BOOLEAN(RBoolean.class),
    CHAR(null),
    STRING(RString.class),
    UUID(null),
    // numeric
    BYTE(null),
    SHORT(null),
    INTEGER(RInteger.class),
    LONG(null),
    FLOAT(null),
    DOUBLE(RDouble.class),

    // mc
    LOCATION(null),
    VECTOR(RVector.class),
    
    //large
    ITEMSTACK(RItemStack.class),
    INVENTORY(null),
    BYTEARRAY(RByteArray.class), // generalized QoL

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
