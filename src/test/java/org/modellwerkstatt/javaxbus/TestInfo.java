package org.modellwerkstatt.javaxbus;

import java.util.concurrent.CountDownLatch;

public class TestInfo {
    public CountDownLatch msg1Received = new CountDownLatch(1);
    public CountDownLatch msg2Received = new CountDownLatch(1);
    public CountDownLatch msg3Received = new CountDownLatch(1);
    public CountDownLatch errorReceived = new CountDownLatch(1);

    public Message lastMsgReceived;
    public String lastError;

}
