package com.fnklabs.nast.network.io.worker;

import com.fnklabs.nast.commons.Executors;
import com.fnklabs.nast.network.io.ChannelWorker;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.String.format;

public class WorkerByOperation implements WorkerGroup {


    private final Map<Integer, Group> threadGroups = new ConcurrentHashMap<>();

    public WorkerByOperation(String name, int opReadPoolSize, int opWritePoolSize, int opAcceptPoolSize, int opConnectPoolSize) {
        threadGroups.put(SelectionKey.OP_ACCEPT, new Group(format("%s[accept]", name), SelectionKey.OP_ACCEPT, opAcceptPoolSize));
        threadGroups.put(SelectionKey.OP_READ, new Group(format("%s[read]", name), SelectionKey.OP_READ, opReadPoolSize));
        threadGroups.put(SelectionKey.OP_WRITE, new Group(format("%s[write]", name), SelectionKey.OP_WRITE, opWritePoolSize));
        threadGroups.put(SelectionKey.OP_CONNECT, new Group(format("%s[connect]", name), SelectionKey.OP_CONNECT, opConnectPoolSize));
    }


    public WorkerByOperation(String name, int opReadPoolSize, int opWritePoolSize) {
        this(name, opReadPoolSize, opWritePoolSize, 1, 1);
    }

    public void start(Consumer<SelectionKey> onAccept,
                      Consumer<SelectionKey> onWrite,
                      Consumer<SelectionKey> onRead,
                      Consumer<SelectionKey> onConnect) {
        threadGroups.get(SelectionKey.OP_ACCEPT).start(onAccept);
        threadGroups.get(SelectionKey.OP_WRITE).start(onWrite);
        threadGroups.get(SelectionKey.OP_READ).start(onRead);
        threadGroups.get(SelectionKey.OP_CONNECT).start(onConnect);
    }

    @Override
    public void attach(int[] operations, Function<Selector, SelectionKey> registrar) {
        for (int operation : operations) {
            selectLessLoadedWorkerFor(operation).attach(registrar);
        }
    }

    @Override
    public void close() throws Exception {
        threadGroups.forEach((op, group) -> {
            for (ChannelWorker worker : group.workers) {
                worker.close();
            }

            try {
                Executors.shutdown(group.executor);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });


    }

    /**
     * Get less loading worker
     *
     * @return {@link ChannelWorker} instance
     */
    @Override
    public Worker selectLessLoadedWorkerFor(int operation) {
        Group group = threadGroups.get(operation);

        return group.getLessLoadedWorker();
    }

    private static class Group {
        private final int operation;
        private final ThreadPoolExecutor executor;
        private final List<ChannelWorker> workers = new ArrayList<>();

        private Group(String name, int operation, int poolSize) {
            this.operation = operation;
            this.executor = Executors.fixedPool(name, poolSize);
        }

        public void start(Consumer<SelectionKey> keyConsumer) {
            for (int i = 0; i < executor.getMaximumPoolSize(); i++) {
                ChannelWorker worker = createWorker(keyConsumer);

                executor.submit(worker);

                workers.add(worker);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(operation);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Group)) return false;
            Group group = (Group) o;
            return operation == group.operation;
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

        ChannelWorker createWorker(Consumer<SelectionKey> onSelect) {
            return new ChannelWorker(onSelect);
        }

    }
}
