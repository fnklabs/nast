package com.fnklabs.nast.network.io.worker;

import com.fnklabs.nast.commons.Executors;
import com.fnklabs.nast.network.io.ChannelWorker;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Function;

public class SimpleWorkerGroup implements WorkerGroup {
    private final ThreadPoolExecutor executor;
    private final List<ChannelWorker> workers = new ArrayList<>();


    public SimpleWorkerGroup(String name, int poolSize) {
        executor = Executors.fixedPool(name, poolSize);
    }

    @Override
    public void close() throws Exception {
        for (ChannelWorker worker : workers) {
            worker.close();
        }
        Executors.shutdown(executor);

    }

    /**
     * Get less loading worker
     *
     * @return {@link ChannelWorker} instance
     */
    @Override
    public Worker selectLessLoadedWorkerFor(int operation) {

        return getLessLoadedWorker();
    }

    @Override
    public void start(Consumer<SelectionKey> onAccept, Consumer<SelectionKey> onWrite, Consumer<SelectionKey> onRead, Consumer<SelectionKey> onConnect) {
        for (int i = 0; i < executor.getMaximumPoolSize(); i++) {
            Consumer<SelectionKey> defaultConsumer = k -> {};

            if (onAccept != null) {
                defaultConsumer = onAccept;
            }

            if (onWrite != null) {
                defaultConsumer = defaultConsumer.andThen(onWrite);
            }

            if (onRead != null) {
                defaultConsumer = defaultConsumer.andThen(onRead);
            }
            if (onConnect != null) {
                defaultConsumer = defaultConsumer.andThen(onConnect);
            }

            ChannelWorker worker = new ChannelWorker(defaultConsumer);

            executor.submit(worker);

            workers.add(worker);
        }
    }

    @Override
    public void attach(int[] operations, Function<Selector, SelectionKey> registrar) {
        for (int operation : operations) {
            selectLessLoadedWorkerFor(operation).attach(registrar);
        }
    }

    public Worker getLessLoadedWorker() {
        ChannelWorker lessLoadedWorker = workers.get(0);

        for (ChannelWorker worker : workers) {
            if (worker.getConnections() < lessLoadedWorker.getConnections()) {
                lessLoadedWorker = worker;
            }
        }

        return lessLoadedWorker;
    }
}
