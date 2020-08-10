package com.fnklabs.nast.network.io;

import com.fnklabs.nast.network.io.worker.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

public class ChannelWorker implements Worker {
    private final Selector selector;

    private final AtomicInteger connections = new AtomicInteger(0);

    private final Consumer<SelectionKey> funcUnit;

    private static final Logger log = LoggerFactory.getLogger(ChannelWorker.class);

    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    /**
     * Lock that used for locking selector for processing selector keys and attaching new keys
     */
    private final ReentrantLock selectorLock = new ReentrantLock();

    public ChannelWorker(Consumer<SelectionKey> funcUnit) {
        this.funcUnit = funcUnit;

        try {
            this.selector = Selector.open();
            log.debug("open selector");

            while (!selector.isOpen()) {
                log.debug("selector is not open");
            }

        } catch (IOException e) {
            throw new HostNotAvailableException("can't create selector");
        }
    }

    @Override
    public void run() {
        log.debug("run worker...");

        while (isRunning.get()) {
            if (selector.keys().isEmpty()) {
                continue;
            }

            try {
                selectorLock.lock();

                int select = selector.select();

                if (select > 0) {
                    if (selector.isOpen()) {
                        Set<SelectionKey> selectedKeys = selector.selectedKeys();

                        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                        while (keyIterator.hasNext()) {
                            SelectionKey selectionKey = keyIterator.next();

                            funcUnit.accept(selectionKey);

                            selectedKeys.remove(selectionKey);
                        }
                    }
                }
            } catch (SessionClosed e) {
                log.debug("session was closed", e);

                decConnections();
            } catch (ConcurrentModificationException e) {
                // no-op
            } catch (StopWorker e) {
                isRunning.set(false);
                log.warn("stop worker...", e);
                break;
            } catch (Throwable e) {
                log.warn("can't process selector", e);
            } finally {
                selectorLock.unlock();
            }
        }


        log.info("closing worker...");

        close();


        log.info("worker was closed");
    }

    @Override
    public SelectionKey attach(Function<Selector, SelectionKey> registrar) {
        selector.wakeup();

        try {
            selectorLock.lock();

            SelectionKey selectionKey = registrar.apply(getSelector());

            incConnections();

            return selectionKey;
        } finally {
            selectorLock.unlock();
        }
    }


    @Override
    public void close() {
        if (isRunning.compareAndSet(true, false)) {
            selector.wakeup();

            try {
                selectorLock.lock();

                for (SelectionKey key : selector.keys()) {
                    key.channel().close();
                }
                selector.close();

                while (selector.isOpen()) { }
            } catch (IOException ex) {
                log.warn("can't close selector", ex);
            } finally {
                selectorLock.unlock();
            }


            log.debug("worker was closed");
        }

    }

    private Selector getSelector() {
        return selector;
    }

    public int getConnections() {
        return connections.get();
    }

    int decConnections() {
        return connections.decrementAndGet();
    }

    int incConnections() {

        int i = connections.incrementAndGet();

        log.debug("new channel current load count {}", i);
        return i;
    }
}
