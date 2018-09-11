package org.modellwerkstatt.javaxbus;

import mjson.Json;


import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;


public class TestApp1 {
    private Json receivedMsg = null;
    private CountDownLatch latch = new CountDownLatch(1);
    private String errorMsg = null;

    private TestApp1() {

    }


    public static void main( String[] args )  {
        EventBus ev = EventBus.create("localhost", 8089);
        final TestApp1 infos = new TestApp1();

        ev.addErrorHandler(new ErrorHandler<Json>() {
            @Override
            public void handleMsgFromBus(boolean stillConected, boolean readerRunning, Json payload) {
                infos.errorMsg = "connected " + stillConected + " reader_ok " + readerRunning + " - " + payload.toString();
                infos.latch.countDown();
            }

            @Override
            public void handleException(boolean stillConected, boolean readerRunning, Exception e) {
                infos.errorMsg = "connected " + stillConected + " reader_ok " + readerRunning + " - " + e.toString();
                infos.latch.countDown();
            }
        });

        ev.consumer("echo", new ConsumerHandler<Json>() {
            @Override
            public void handle(boolean err, Json msg) {
                infos.receivedMsg = msg;
                infos.latch.countDown();
            }
        });

        ev.send("echo", Json.object().set("from", "dan").set("content", "hello world"));








        try {
            infos.latch.await();
            ev.close();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        assertNull(infos.errorMsg);
        assertNotNull(infos.receivedMsg);
        assertEquals("dan", infos.receivedMsg.at("body").at("from").asString());
    }
}
