package org.modellwerkstatt.javaxbus;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import mjson.Json;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Unit test for simple App.
 */
public class VertXSunnyTest extends TestCase {
    private Json tempObj;
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public VertXSunnyTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( VertXSunnyTest.class );
    }


    public void test_SendReceive() {
        EventBus ev = EventBus.create("localhost", 8089);

        final CountDownLatch latch = new CountDownLatch(1);
        tempObj = null;

        ev.addErrorHandler(new ErrorHandler<Json>() {
            @Override
            public void handleMsgFromBus(boolean stillConected, boolean readerRunning, Json payload) {
                latch.countDown();
            }

            @Override
            public void handleException(boolean stillConected, boolean readerRunning, Exception e) {
                latch.countDown();
            }
        });

        ev.consumer("echo", new ConsumerHandler<Json>() {
            @Override
            public void handle(Json msg) {
                tempObj = msg;
                latch.countDown();
            }
        });

        ev.send("echo", Json.object().set("from", "dan").set("content", "hello world"));

        try {
            latch.await();
            ev.close();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertNotNull(tempObj);
        assertEquals("dan", tempObj.at("body").at("from").asString());
    }

    public void test_Reconnect() {
        EventBus ev = EventBus.create("localhost", 8089);

        final CountDownLatch firstMessage = new CountDownLatch(1);
        final CountDownLatch errorHandlerCalled = new CountDownLatch(1);
        final CountDownLatch secondMessage = new CountDownLatch(1);

        tempObj = null;

        ev.addErrorHandler(new ErrorHandler<Json>() {
            @Override
            public void handleMsgFromBus(boolean stillConected, boolean readerRunning, Json payload) {
                firstMessage.countDown();
                System.err.println("StillConnected " + stillConected + "  readerRunning "+ readerRunning + " payload " + payload);
            }

            @Override
            public void handleException(boolean stillConected, boolean readerRunning, Exception e) {
                System.err.println("StillConnected " + stillConected + "  readerRunning "+ readerRunning + " exception " + e);
                errorHandlerCalled.countDown();
            }
        });

        ev.consumer("echo", new ConsumerHandler<Json>() {
            @Override
            public void handle(Json msg) {
                tempObj = msg;
                firstMessage.countDown();
                System.err.println("received msg " + msg.toString());
                if (msg.at("body").at("content").asString().equals("two")) {
                    secondMessage.countDown();
                }
            }
        });

        ev.send("echo", Json.object().set("from", "dan").set("content", "hello world"));

        try {
            firstMessage.await();
            System.err.println("Shutdown vertx now and press a key.");

            System.err.println("O k ... waiting for error handler .... ");
            errorHandlerCalled.await();

            System.err.println("Okay, error received .. start vertx again ... ");

            Thread.sleep(15000);
            System.err.println("Waiting done, sending another msg.");

            ev.send("echo", Json.object().set("from", "dan").set("content", "two"));
            secondMessage.await();


        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        assertNotNull(tempObj);
        assertEquals("two", tempObj.at("body").at("content").asString());
    }

}
