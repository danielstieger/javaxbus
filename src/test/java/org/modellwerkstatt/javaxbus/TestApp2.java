package org.modellwerkstatt.javaxbus;

import mjson.Json;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.*;


public class TestApp2 {
    private Message receivedMsg = null;
    private CountDownLatch firstMessage = new CountDownLatch(1);
    private CountDownLatch errorHandlerCalled = new CountDownLatch(1);
    private CountDownLatch secondMessage = new CountDownLatch(1);
    private String errorMsg = null;

    private TestApp2() {

    }




    public static void main( String[] args )  {
        EventBus ev = EventBus.create("localhost", 8089);
        final TestApp2 infos = new TestApp2();


        ev.setUnderTestingMode();
        ev.addErrorHandler(new ErrorHandler() {
            @Override
            public void handleMsgFromBus(boolean stillConected, boolean readerRunning, Message payload) {
                infos.receivedMsg = null;
                infos.errorMsg = "connected " + stillConected + " reader_ok " + readerRunning + " - " + payload.toString();
                infos.errorHandlerCalled.countDown();
            }

            @Override
            public void handleException(boolean stillConected, boolean readerRunning, Exception e) {
                infos.receivedMsg = null;
                infos.errorMsg = "connected " + stillConected + " reader_ok " + readerRunning + " - " + e.toString();
                infos.errorHandlerCalled.countDown();
            }
        });

        ev.consumer("echo", new ConsumerHandler() {
            @Override
            public void handle(Message r) {
                infos.receivedMsg = r;
                Json msg = infos.receivedMsg.getBodyAsMJson();

                if (msg.at("content").asString().equals("msg1")){
                    infos.firstMessage.countDown();

                } else if (msg.at("content").asString().equals("msg2")) {
                    infos.secondMessage.countDown();

                }else {
                    // unknown message?
                    infos.firstMessage.countDown();
                    infos.secondMessage.countDown();
                }
            }
        });



        try {
            ev.send("echo", Json.object().set("from", "dan").set("content", "msg1"));

            infos.firstMessage.await();
            assertNull(infos.errorMsg);
            assertNotNull(infos.receivedMsg);

            Json msg = infos.receivedMsg.getBodyAsMJson();
            assertEquals("msg1", msg.at("content").asString());


            System.err.println("Shut down vert.x and press a key..... ");
            System.in.read();


            infos.errorHandlerCalled.await();
            assertNotNull(infos.errorMsg);
            infos.errorMsg = null;
            assertNull(infos.receivedMsg);


            System.err.println("Start vert.x again and press a key..... ");
            System.in.read();


            ev.send("echo", Json.object().set("from", "dan").set("content", "msg2"));

            infos.secondMessage.await();
            assertNull(infos.errorMsg);
            assertNotNull(infos.receivedMsg);

            msg = infos.receivedMsg.getBodyAsMJson();
            assertEquals("msg2", msg.at("content").asString());


        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ev.close();

        }




    }
}
