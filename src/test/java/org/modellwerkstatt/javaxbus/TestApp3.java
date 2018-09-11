package org.modellwerkstatt.javaxbus;

import mjson.Json;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.*;


public class TestApp3 {
    private Json receivedMsg = null;
    private CountDownLatch latch = new CountDownLatch(1);
    private String errorMsg = null;

    private TestApp3() {

    }


    public static void main( String[] args )  {
        EventBus ev = EventBus.create("localhost", 8089);
        final TestApp3 infos = new TestApp3();

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


        try {
            ev.send("echo", Json.object().set("from", "dan").set("content", "hello world"), new ConsumerHandler<Json>() {
                @Override
                public void handle(Json msg) {
                    System.err.println("ok? > " + msg.toString());
                    infos.receivedMsg = msg;
                    infos.latch.countDown();
                }
            });

            infos.latch.await();
            assertNotNull(infos.receivedMsg);
            assertEquals(infos.receivedMsg.at("type").asString(), "message");


            infos.latch = new CountDownLatch(1);

            ev.send("echo", Json.object().set("from", "dan").set("content", "hello world").set("fail","yes"), new ConsumerHandler<Json>() {
                @Override
                public void handle(Json msg) {
                    System.err.println("fail? > " + msg.toString());
                    infos.receivedMsg = msg;
                    infos.latch.countDown();
                }
            });


            infos.latch.await();
            assertNotNull(infos.receivedMsg);
            assertEquals(infos.receivedMsg.at("type").asString(), "err");
            assertEquals(infos.receivedMsg.at("sourceAddress").asString(), "echo");


            ev.close();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
