package com.fnklabs.nast.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class that provide functionality to build {@link ThreadPoolExecutor} and close it
 */
public class Executors {
    private static final Logger log = LoggerFactory.getLogger(Executors.class);

    /**
     * Build named {@link ThreadPoolExecutor} with provided name and poolSize.
     * {@code By default pool.size = 100}
     *
     * @param name     Executor name
     * @param poolSize Executor pool size
     *
     * @return {@link ThreadPoolExecutor} instance
     */
    public static ThreadPoolExecutor fixedPool(String name, int poolSize) {
        return fixedPool(name, poolSize, 100);
    }

    /**
     * Build named {@link ThreadPoolExecutor} with provided name and poolSize and queueSize.
     * {@code By default {@link ThreadPoolExecutor#getRejectedExecutionHandler()} == {@link ThreadPoolExecutor.AbortPolicy}}
     *
     * @param name      Executor name
     * @param poolSize  Executor pool size
     * @param queueSize Executor queue size
     *
     * @return {@link ThreadPoolExecutor} instance
     */
    public static ThreadPoolExecutor fixedPool(String name, int poolSize, int queueSize) {

        if (poolSize < 1) {
            throw new IllegalArgumentException(String.format("pool size must be greater than 1 actual %d", poolSize));
        }

        if (queueSize < 0) {
            throw new IllegalArgumentException(String.format("queue size must be greater than 1 actual %d", queueSize));
        }

        ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(queueSize);

        return new ThreadPoolExecutor(
                poolSize, poolSize,
                0, TimeUnit.MINUTES,
                queue,
                new NamedThreadFactory(name),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * Gracefully shutdown {@link java.util.concurrent.Executor}
     *
     * @param executorService {@link java.util.concurrent.Executor} instance
     *
     * @throws InterruptedException if can't shutdown executor
     */
    public static void shutdown(ExecutorService executorService) throws InterruptedException {
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("can't wait for stop executor  due to timeout 5 seconds");
                }
            }
        } catch (InterruptedException e) {
            log.error("can't wait for stop executor  due to timeout 5 seconds");

            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }


    static class NamedThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(String name) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = name;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + "-" + threadNumber.getAndIncrement());

            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
