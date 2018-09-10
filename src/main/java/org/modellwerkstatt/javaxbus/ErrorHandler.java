package org.modellwerkstatt.javaxbus;

public interface ErrorHandler<T> {

    void handle(boolean stillConected, boolean readerRunning, T payload);
}
