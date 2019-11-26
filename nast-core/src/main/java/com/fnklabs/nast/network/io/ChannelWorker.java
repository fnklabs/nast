package com.fnklabs.nast.network.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class ChannelWorker implements Runnable, Closeable {
    private final Selector selector;

    private final AtomicInteger connections = new AtomicInteger(0);

    private final Function<Selector, Boolean> funcUnit;

    private static final Logger log = LoggerFactory.getLogger(ChannelWorker.class);

    ChannelWorker(Function<Selector, Boolean> funcUnit) {
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

        try {
            for (; ; ) {
                if (selector.keys().isEmpty()) {
                    continue;
                }

                try {
                    if (!funcUnit.apply(getSelector())) {
                        break;
                    }
                } catch (Exception e) {
                    log.warn("can't process select", e);
                }
            }


            log.debug("closing worker...");

            close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        log.debug("worker was closed");
    }

    SelectionKey add(Function<Selector, SelectionKey> unit) {
        SelectionKey selectionKey = unit.apply(getSelector());

        incConnections();

        return selectionKey;
    }

    @Override
    public void close() throws IOException {
        selector.wakeup();

        selector.close();

        while (selector.isOpen()) {

        }

        log.debug("worker was closed");
    }

    private Selector getSelector() {
        return selector;
    }

    int getConnections() {
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
