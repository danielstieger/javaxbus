package org.modellwerkstatt.javaxbus;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import mjson.Json;

/**
 * Unit test for simple TestApp1.
 */
public class VertxEchoSunnyTest extends TestCase {
    public static final String VERTX_HOSTNAME = "localhost";
    public static final int VERTX_TCPBRIDGEPORT = 8089;

    //public static final String VERTX_HOSTNAME = "modwerk-test";
    //public static final int VERTX_TCPBRIDGEPORT = 2128;


    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public VertxEchoSunnyTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( VertxEchoSunnyTest.class );
    }

    public void dl(String msg){
        // System.err.println(msg);
    }

    public void test_sendreceive()
    {

        EventBus eb = EventBus.create(VERTX_HOSTNAME, VERTX_TCPBRIDGEPORT);
        final TestInfo info = new TestInfo();

        eb.setUnderTestingMode();
        eb.addErrorHandler(new ErrorHandler() {
            @Override
            public void handleMsgFromBus(boolean stillConected, boolean readerRunning, Message payload) {
                // should not happen
                assertTrue(false);
                info.msg1Received.countDown();
            }

            @Override
            public void handleException(boolean stillConected, boolean readerRunning, Exception e) {
                assertTrue(false);
                info.msg1Received.countDown();
            }
        });

        eb.consumer("echo", new ConsumerHandler() {
            @Override
            public void handle(Message msg) {
                dl(msg.toString());

                assertFalse(msg.isErrorMsg());
                info.lastMsgReceived = msg;
                info.msg1Received.countDown();
            }
        });

        eb.send("echo", Json.object().set("content", "hello"));

        try {
            info.msg1Received.await();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        eb.close();

        assertNotNull(info.lastMsgReceived);
        assertEquals(info.lastMsgReceived.getBodyAsMJson().at("content").asString(), "hello");

    }

    public void test_publishreceive()
    {

        EventBus eb = EventBus.create(VERTX_HOSTNAME, VERTX_TCPBRIDGEPORT);
        final TestInfo info = new TestInfo();

        eb.setUnderTestingMode();
        eb.addErrorHandler(new ErrorHandler() {
            @Override
            public void handleMsgFromBus(boolean stillConected, boolean readerRunning, Message payload) {
                // should not happen
                assertTrue(false);
                info.msg1Received.countDown();
            }

            @Override
            public void handleException(boolean stillConected, boolean readerRunning, Exception e) {
                assertTrue(false);
                info.msg1Received.countDown();
            }
        });

        eb.consumer("echo", new ConsumerHandler() {
            @Override
            public void handle(Message msg) {
                dl(msg.toString());

                assertFalse(msg.isErrorMsg());
                info.lastMsgReceived = msg;
                info.msg1Received.countDown();
            }
        });

        eb.publish("echo", Json.object().set("content", "hello"));

        try {
            info.msg1Received.await();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        eb.close();

        assertNotNull(info.lastMsgReceived);

        assertEquals(info.lastMsgReceived.getBodyAsMJson().at("content").asString(), "hello");
        assertEquals(info.lastMsgReceived.isPublishedMsg(), true);
    }


    public void test_internaldispatch()
    {

        EventBus eb = EventBus.create(VERTX_HOSTNAME, VERTX_TCPBRIDGEPORT);
        final TestInfo info = new TestInfo();

        eb.setUnderTestingMode();
        eb.addErrorHandler(new ErrorHandler() {
            @Override
            public void handleMsgFromBus(boolean stillConected, boolean readerRunning, Message payload) {
                // should not happen
                assertTrue(false);
                info.msg1Received.countDown();
                info.msg2Received.countDown();
            }

            @Override
            public void handleException(boolean stillConected, boolean readerRunning, Exception e) {
                assertTrue(false);
                info.msg1Received.countDown();
                info.msg2Received.countDown();
            }
        });

        eb.consumer("echo", new ConsumerHandler() {
            @Override
            public void handle(Message msg) {
                dl(msg.toString());

                assertFalse(msg.isErrorMsg());
                info.lastMsgReceived = msg;
                info.msg1Received.countDown();
            }
        });

        eb.consumer("echo", new ConsumerHandler() {
            @Override
            public void handle(Message msg) {
                dl(msg.toString());

                assertFalse(msg.isErrorMsg());
                info.lastMsgReceived = msg;
                info.msg2Received.countDown();
            }
        });

        eb.send("echo", Json.object().set("content", "hello"));

        try {
            info.msg1Received.await();
            info.msg2Received.await();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        eb.close();

        assertNotNull(info.lastMsgReceived);
        assertEquals(info.lastMsgReceived.getBodyAsMJson().at("content").asString(), "hello");

        // this was a publish msg...
        assertEquals(info.lastMsgReceived.isPublishedMsg(), false);
    }


    public void test_different_dispatch()
    {

        EventBus eb = EventBus.create(VERTX_HOSTNAME, VERTX_TCPBRIDGEPORT);
        final TestInfo info = new TestInfo();

        eb.setUnderTestingMode();
        eb.addErrorHandler(new ErrorHandler() {
            @Override
            public void handleMsgFromBus(boolean stillConected, boolean readerRunning, Message payload) {
                // should not happen
                assertTrue(false);
                info.msg1Received.countDown();
                info.msg2Received.countDown();
            }

            @Override
            public void handleException(boolean stillConected, boolean readerRunning, Exception e) {
                assertTrue(false);
                info.msg1Received.countDown();
                info.msg2Received.countDown();
            }
        });

        eb.consumer("echo", new ConsumerHandler() {
            @Override
            public void handle(Message msg) {
                dl(msg.toString());

                assertFalse(msg.isErrorMsg());
                info.lastMsgReceived = msg;
                info.msg1Received.countDown();
            }
        });

        eb.consumer("echo2", new ConsumerHandler() {
            @Override
            public void handle(Message msg) {
                dl(msg.toString());

                assertFalse(msg.isErrorMsg());
                info.lastMsgReceived = msg;
                info.msg2Received.countDown();
            }
        });

        eb.send("echo2", Json.object().set("content", "hello"));

        try {
            info.msg2Received.await();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        eb.close();

        assertNotNull(info.lastMsgReceived);
        Json msg = info.lastMsgReceived.getBodyAsMJson();
        assertEquals(msg.at("content").asString(), "hello");

        // echo and not echo 2!
        assertEquals(info.lastMsgReceived.getAddress(), "echo2");
        assertEquals(info.msg1Received.getCount(), 1);

        // this was a publish msg...
        assertEquals(!info.lastMsgReceived.isPublishedMsg(), true);
    }
}
