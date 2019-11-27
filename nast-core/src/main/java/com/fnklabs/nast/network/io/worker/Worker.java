package com.fnklabs.nast.network.io.worker;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.function.Function;

public interface Worker extends AutoCloseable, Runnable {
    SelectionKey attach(Function<Selector, SelectionKey> registrar);
}
