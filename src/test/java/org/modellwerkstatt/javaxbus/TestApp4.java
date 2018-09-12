package org.modellwerkstatt.javaxbus;

import mjson.Json;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;


public class TestApp4 {
    private Message receivedMsg = null;
    private CountDownLatch latch = new CountDownLatch(1);
    private String errorMsg = null;

    private TestApp4() {

    }


    public static void main( String[] args )  {
        EventBus ev = EventBus.create("localhost", 8089);
        final TestApp4 infos = new TestApp4();

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
            ev.send("echo", Json.object().set("from", "dan").set("noreply", "no"), new ConsumerHandler() {
                @Override
                public void handle(Message msg) {
                    System.err.println("fail "+ msg.isErrorMsg() + " ? > " + msg.toString());
                    infos.receivedMsg = msg;
                    if (msg.isErrorMsg()){
                        infos.errorMsg = "Fail due to " + msg.getErrMessage() + " -> " + msg.getErrFailureType();
                    }
                    infos.latch.countDown();
                }
            });

            infos.latch.await();
            assertNotNull(infos.receivedMsg);
            assertTrue(infos.receivedMsg.isErrorMsg());
            assertNotNull(infos.errorMsg);
            System.err.println(infos.errorMsg);

            ev.close();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
