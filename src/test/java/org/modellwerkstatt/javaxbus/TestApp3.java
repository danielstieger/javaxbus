package org.modellwerkstatt.javaxbus;

import mjson.Json;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.*;


public class TestApp3 {
    private Message receivedMsg = null;
    private CountDownLatch latch = new CountDownLatch(1);
    private String errorMsg = null;

    private TestApp3() {

    }


    public static void main( String[] args )  {
        EventBus ev = EventBus.create("localhost", 8089);
        final TestApp3 infos = new TestApp3();

        ev.addErrorHandler(new ErrorHandler() {
            @Override
            public void handleMsgFromBus(boolean stillConected, boolean readerRunning, Message payload) {
                infos.errorMsg = "connected " + stillConected + " reader_ok " + readerRunning + " - " + payload.toString();
                infos.latch.countDown();
            }

            @Override
            public void handleException(boolean stillConected, boolean readerRunning, Exception e) {
                infos.errorMsg = "connected " + stillConected + " reader_ok " + readerRunning + " - " + e.toString();
                infos.latch.countDown();
            }
        });


        try {
            ev.send("echo", Json.object().set("from", "dan").set("reply", "yes"), new ConsumerHandler() {
                @Override
                public void handle(Message msg) {
                    System.err.println("ok? > " + msg.toString());
                    infos.receivedMsg = msg;
                    infos.latch.countDown();
                }
            });

            infos.latch.await();
            assertNotNull(infos.receivedMsg);
            assertTrue(!infos.receivedMsg.isErrorMsg());


            infos.latch = new CountDownLatch(1);

            ev.send("echo", Json.object().set("from", "dan").set("content", "hello world").set("fail","yes"), new ConsumerHandler() {
                @Override
                public void handle(Message msg) {
                    System.err.println("fail? > " + msg.toString());
                    infos.receivedMsg = msg;
                    infos.latch.countDown();
                }
            });


            infos.latch.await();
            assertNotNull(infos.receivedMsg);
            assertTrue(infos.receivedMsg.isErrorMsg());
            System.err.println("> "+ infos.receivedMsg.toString());

            ev.close();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
