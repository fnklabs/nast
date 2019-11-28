package com.fnklabs.nast.network.io.worker;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.function.Consumer;
import java.util.function.Function;

public interface WorkerGroup extends AutoCloseable {


    Worker selectLessLoadedWorkerFor(int operation);

    void start(Consumer<SelectionKey> onAccept, Consumer<SelectionKey> onWrite, Consumer<SelectionKey> onRead, Consumer<SelectionKey> onConnect);

    void attach(int[] operations, Function<Selector, SelectionKey> registrar);
}
