package net.legendofwar.firecord.jedis;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.datakeys.KeyGenerator;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JedisLock implements Lock, Closeable {

    final static long DEFAULT_TIMEOUT = 5000; // 5s
    final static SetParams params;

    static {
        params = new SetParams();
        params.px(DEFAULT_TIMEOUT);
        params.nx();
    }

    final Bytes id;

    private final Lock slave_lock = new ReentrantLock();

    public JedisLock(@NotNull Bytes id) {
        this.id = KeyGenerator.getLockKey(id);

    }

    public final Bytes getId() {
        return id;
    }

    /*
     * Tries to lock multiple locks, and unlocks them again if we cannot unlock all
     */
    public boolean tryLockMultiple(JedisLock... partners) {
        List<JedisLock> unlocked = new ArrayList<JedisLock>();
        if (tryLock()) {
            unlocked.add(this);
            for (JedisLock l : partners) {
                if (l.tryLock()) {
                    unlocked.add(l);
                } else {
                    // failed to lock
                    for (JedisLock u : unlocked) {
                        u.unlock();
                    }
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void unlockMultiple(JedisLock... partners) {
        this.unlock();
        for (JedisLock p : partners) {
            p.unlock();
        }
    }

    @Override
    public void lock() {
        try (Jedis jedis = ClassicJedisPool.getJedis()) {
            String result = jedis.set(id.getData(), new Bytes((byte) 1).getData(), params);
            while (result == null) {
                try {
                    Thread.sleep(1);
                    result = jedis.set(id.getData(), new Bytes((byte) 1).getData(), params);
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
        try (Jedis jedis = ClassicJedisPool.getJedis()) {
            String result = jedis.set(id.getData(), new Bytes((byte) 1).getData(), params);
            while (result == null) {
                Thread.sleep(1);
                result = jedis.set(id.getData(), new Bytes((byte) 1).getData(), params);
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException iex) {
                throw iex;
            } else {
                e.printStackTrace();
            }
        }
    }

    /**
     * Should NOT be used
     * 
     * @return New Condition object
     */
    @Override
    public Condition newCondition() {
        return slave_lock.newCondition();
    }

    @Override
    public boolean tryLock() {
        try (Jedis jedis = ClassicJedisPool.getJedis()) {
            return jedis.set(id.getData(), new Bytes((byte) 1).getData(), params) != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean tryLock(long lockTime, @NotNull TimeUnit timeUnit) {
        try (Jedis jedis = ClassicJedisPool.getJedis()) {
            long now = System.nanoTime();
            long timeout = now + timeUnit.toNanos(lockTime);
            String result = jedis.set(id.getData(), new Bytes((byte) 1).getData(), params);
            while (result == null && timeout > System.nanoTime()) {
                Thread.sleep(1);
                result = jedis.set(id.getData(), new Bytes((byte) 1).getData(), params);
            }
            return result != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void unlock() {
        try (Jedis jedis = ClassicJedisPool.getJedis()) {
            jedis.del(id.getData());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        this.unlock();
    }

}
