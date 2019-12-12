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
import java.util.function.Consumer;
import java.util.function.Function;

public class ChannelWorker implements Worker {
    private final Selector selector;

    private final AtomicInteger connections = new AtomicInteger(0);

    private final Consumer<SelectionKey> funcUnit;

    private static final Logger log = LoggerFactory.getLogger(ChannelWorker.class);

    private final AtomicBoolean isRunning = new AtomicBoolean(true);

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
                int select = selector.select();

                if (select > 0) {
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();

                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                    while (keyIterator.hasNext()) {
                        SelectionKey selectionKey = keyIterator.next();

                        funcUnit.accept(selectionKey);

                        selectedKeys.remove(selectionKey);
                    }
                }
            } catch (ConcurrentModificationException e) {
                // no-op
            } catch (StopWorker | IOException e) {
                isRunning.set(false);
                log.warn("stop worker...", e);
                break;
            }
        }


        log.info("closing worker...");

        close();


        log.info("worker was closed");
    }

    @Override
    public SelectionKey attach(Function<Selector, SelectionKey> registrar) {
        selector.wakeup();
        selector.wakeup();

        SelectionKey selectionKey = registrar.apply(getSelector());

        incConnections();

        return selectionKey;
    }


    @Override
    public void close() {
        isRunning.set(false);

        selector.wakeup();

        try {
            selector.close();
        } catch (IOException ex) {
            log.warn("can't close selector", ex);
        }


        while (selector.isOpen()) {

        }

        log.debug("worker was closed");
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
