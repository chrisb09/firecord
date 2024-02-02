package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DummyFuture<V> implements Future<V> {

    

    @Override
    public boolean cancel(boolean arg0) {
        return false;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public V get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

}