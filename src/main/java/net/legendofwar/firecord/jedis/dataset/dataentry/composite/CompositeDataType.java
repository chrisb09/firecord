package net.legendofwar.firecord.jedis.dataset.dataentry.composite;

public enum CompositeDataType {

    LIST("list", RList.class),
    SET("set", null),
    SORTEDSET("zset", null),
    MAP("hash", null),;

    final Class<?> c;
    final String redisName;

    private CompositeDataType(String redisName, Class<?> c) {
        this.c = c;
        this.redisName = redisName;
    }

    public Class<?> getC() {
        return c;
    }

    public String getRedisName() {
        return redisName;
    }

}
