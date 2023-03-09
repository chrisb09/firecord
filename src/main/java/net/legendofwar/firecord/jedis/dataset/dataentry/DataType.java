package net.legendofwar.firecord.jedis.dataset.dataentry;

import net.legendofwar.firecord.jedis.dataset.dataentry.composite.RList;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RInteger;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RItemStack;

public enum DataType {
    

    BOOLEAN(null),
    BYTE(null),
    CHAR(null),
    SHORT(null),
    INTEGER(RInteger.class),
    LONG(null),
    FLOAT(null),
    DOUBLE(null),
    STRING(null),
    UUID(null),
    ITEMSTACK(RItemStack.class),
    INVENTORY(null),
    LOCATION(null),

    LIST(RList.class),
    SET(null),
    SORTEDSET(null),
    MAP(null),
    OBJECT(null); //for later use

    Class<?> c;

    private DataType(Class<?> c) {
        this.c = c;
    }

    public Class<?> getC() {
        return c;
    }
}
