package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

public enum SimpleDataType {

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
    LOCATION(null);

    Class<?> c;

    private SimpleDataType(Class<?> c) {
        this.c = c;
    }

    public Class<?> getC() {
        return c;
    }

}
