package net.legendofwar.firecord.jedis;

import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JedisLock implements Lock {

    static long DEFAULT_TIMEOUT = 30000;

    String id;
    String key;
    SetParams params;

    private final Lock slave_lock = new ReentrantLock();

    public JedisLock(String id, long timeoutMilliseconds) {
        this.id = id;
        this.key = "redis_lock:"+id;
        params = new SetParams();
        params.px(timeoutMilliseconds);
        params.nx();
    }

    public JedisLock(String id) {
        this(id, DEFAULT_TIMEOUT);
    }

    public String getId() {
        return id;
    }

    @Override
    public void lock() {
        try(Jedis jedis = ClassicJedisPool.getJedis()) {
            String result = jedis.set(key, "1", params);
            while (result == null) {
                try {
                    Thread.sleep(1);
                    result = jedis.set(key, "1", params);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        try(Jedis jedis = ClassicJedisPool.getJedis()) {
            String result = jedis.set(key, "1", params);
            while (result == null) {
                Thread.sleep(1);
                result = jedis.set(key, "1", params);
            }
        } catch (Exception e) {
            if(e instanceof InterruptedException iex) {
                throw iex;
            } else {
                e.printStackTrace();
            }
        }
    }

    /**
     * Should NOT be used
     * @return New Condition object
     */
    @Override
    public Condition newCondition() {
        return slave_lock.newCondition();
    }

    @Override
    public boolean tryLock() {
        try(Jedis jedis = ClassicJedisPool.getJedis()) {
            return jedis.set(key, "1", params) != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean tryLock(long lockTime, @NotNull TimeUnit timeUnit) {
        try(Jedis jedis = ClassicJedisPool.getJedis()) {
            long now = System.nanoTime();
            long timeout = now + timeUnit.toNanos(lockTime);
            String result = jedis.set(key, "1", params);
            while (result == null && timeout > System.nanoTime()) {
                Thread.sleep(1);
                result = jedis.set(key, "1", params);
            }
            return result != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void unlock() {
        try(Jedis jedis = ClassicJedisPool.getJedis()) {
            jedis.del(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
