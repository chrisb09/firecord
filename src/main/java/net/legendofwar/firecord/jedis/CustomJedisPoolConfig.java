package net.legendofwar.firecord.jedis;

import java.time.Duration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class CustomJedisPoolConfig<T> extends GenericObjectPoolConfig<T> {
    CustomJedisPoolConfig() {
        setMinEvictableIdleTime(Duration.ofMillis(60000));
        setTimeBetweenEvictionRuns(Duration.ofMillis(30000));

        setMaxTotal(128);
        setMaxIdle(128);
        setMinIdle(16);
        setTestOnBorrow(true);
        setTestOnReturn(true);
        setNumTestsPerEvictionRun(3);
        setTestWhileIdle(true);
        setBlockWhenExhausted(true);

        
    }
}