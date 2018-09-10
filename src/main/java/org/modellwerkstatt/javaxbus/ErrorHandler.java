package org.modellwerkstatt.javaxbus;

public interface ErrorHandler<T> {

    void handleMsgFromBus(boolean stillConected, boolean readerRunning, T payload);
    void handleException(boolean stillConected, boolean readerRunning, Exception e);
}
