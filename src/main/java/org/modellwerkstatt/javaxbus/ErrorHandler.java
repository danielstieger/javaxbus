package org.modellwerkstatt.javaxbus;

public interface ErrorHandler {

    void handleMsgFromBus(boolean stillConected, boolean readerRunning, Message payload);
    void handleException(boolean stillConected, boolean readerRunning, Exception e);
}
