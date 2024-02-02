package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FieldFuture implements Future<Object> {

    Object entry = null;
    boolean set = false;

    public FieldFuture() {

    }

    public FieldFuture(Object entry) {
        this.entry = entry;
        set = true;
    }

    @Override
    public boolean cancel(boolean arg0) {
        return false;
    }

    public void set(Object entry) {
        this.entry = entry;
        this.set = true;
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        while (!this.set) {
            Thread.sleep(1);
        }
        return this.entry;
    }

    @Override
    public Object get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
        long count = 0;
        while (!this.set) {
            if (count++ >= arg1.toMillis(arg0)) {
                throw new TimeoutException();
            }
            Thread.sleep(1);
        }
        return this.entry;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return this.set;
    }

}
