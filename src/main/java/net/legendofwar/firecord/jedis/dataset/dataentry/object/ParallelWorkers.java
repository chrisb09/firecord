package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject.SetField;

public class ParallelWorkers {

    static ExecutorService executorService = Executors.newFixedThreadPool(100);

    public static Future<?> submit(Runnable runnable) {
        return executorService.submit(runnable);
    }

    public static Future<?> submit(Class<?> clazz, Runnable runnable) {
        if (AnnotationChecker.isParallelLoadAllowed(clazz)) {
            return executorService.submit(runnable);
        }
        runnable.run();
        return new DummyFuture<Object>();
    }

    public static Future<?> submit(String className, String fieldName, Runnable runnable) {
        try {
            Class<?> clazz = Class.forName(className);
            Field field = clazz.getDeclaredField(fieldName);
        if (clazz != null && AnnotationChecker.isParallelLoadAllowed(clazz)) {
            System.out.println("Run parallel.. "+executorService.isShutdown()+" "+executorService.isTerminated());
            return executorService.submit(runnable);
        } else if (field != null && AnnotationChecker.isParallelLoadAllowed(field)) {
            System.out.println("Run parallel...");
            return executorService.submit(runnable);
        }
        runnable.run();
        return new DummyFuture<Object>();
    } catch (ClassNotFoundException e) {
        e.printStackTrace();
    } catch (NoSuchFieldException e) {
        e.printStackTrace();
    } catch (SecurityException e) {
        e.printStackTrace();
    }
        return null;
    }

    public static Future<?> submit(Field field, Runnable runnable) {
        if (AnnotationChecker.isParallelLoadAllowed(field)) {
            return executorService.submit(runnable);
        }
        runnable.run();
        return new DummyFuture<Object>();
    }

    public static Future<Object> submit(String className, String fieldName, Callable<Object> callable) {

        try {
            Class<?> clazz = Class.forName(className);
            Field field = clazz.getField(fieldName);
            if (clazz != null && AnnotationChecker.isParallelLoadAllowed(clazz)) {
                System.out.println("Run parallel.. "+executorService.isShutdown()+" "+executorService.isTerminated());
                return executorService.submit(callable);
            } else if (field != null && AnnotationChecker.isParallelLoadAllowed(field)) {
                System.out.println("Run parallel...");
                return executorService.submit(callable);
            }
            Object result = null;
            result = callable.call();
            return new FieldFuture(result);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DummyFuture<>();
    }

    public static void disable() {
        executorService.shutdown();
    }

}
