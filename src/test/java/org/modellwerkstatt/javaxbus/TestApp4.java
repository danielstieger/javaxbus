package org.modellwerkstatt.javaxbus;

import mjson.Json;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;


public class TestApp4 {
    private Json receivedMsg = null;
    private CountDownLatch latch = new CountDownLatch(1);
    private String errorMsg = null;

    private TestApp4() {

    }


    public static void main( String[] args )  {
        EventBus ev = EventBus.create("localhost", 8089);
        final TestApp4 infos = new TestApp4();

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
            ev.send("echo", Json.object().set("from", "dan").set("noreply", "no"), new ConsumerHandler<Json>() {
                @Override
                public void handle(boolean err, Json msg) {
                    System.err.println("fail "+ err + " ? > " + msg.toString());
                    infos.receivedMsg = msg;
                    if (err){
                        infos.errorMsg = "Fail due to " + msg.at("message").asString() + " -> " + msg.at("failureType");
                    }
                    infos.latch.countDown();
                }
            });

            infos.latch.await();
            assertNotNull(infos.receivedMsg);
            assertEquals(infos.receivedMsg.at("type").asString(), "err");
            assertNotNull(infos.errorMsg);
            System.err.println(infos.errorMsg);

            ev.close();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
