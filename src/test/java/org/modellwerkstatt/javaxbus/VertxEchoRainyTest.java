package org.modellwerkstatt.javaxbus;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import mjson.Json;

/**
 * Unit test for simple TestApp1.
 */
public class VertxEchoRainyTest extends TestCase {
    public static final String VERTX_HOSTNAME = "localhost";
    public static final int VERTX_TCPBRIDGEPORT = 8089;



    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public VertxEchoRainyTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( VertxEchoRainyTest.class );
    }

    public void dl(String msg){
        System.err.println(msg);
    }

    public void test_nohandler()
    {

        EventBus eb = EventBus.create(VERTX_HOSTNAME, VERTX_TCPBRIDGEPORT);
        final TestInfo info = new TestInfo();

        eb.setUnderTestingMode();
        eb.addErrorHandler(new ErrorHandler<Json>() {
            @Override
            public void handleMsgFromBus(boolean stillConected, boolean readerRunning, Json payload) {
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

        eb.send("echo", Json.object().set("content", "hello"), new ConsumerHandler<Json>() {
            @Override
            public void handle(boolean error, Json msg) {
                assertTrue(error);
                dl(msg.toString());
                info.lastMsgReceived = msg;
                info.msg1Received.countDown();
            }
        });

        try {
            info.msg1Received.await();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        eb.close();


        // reply called with a fail!
        assertNotNull(info.lastMsgReceived);
        assertEquals(info.lastMsgReceived.at("type").asString(), "err");
        // message should be no handlers ..
        assertEquals(info.lastMsgReceived.at("failureType").asString(), "NO_HANDLERS");

    }



    public void test_nopermission()
    {

        EventBus eb = EventBus.create(VERTX_HOSTNAME, VERTX_TCPBRIDGEPORT);
        final TestInfo info = new TestInfo();

        eb.setUnderTestingMode();
        eb.addErrorHandler(new ErrorHandler<Json>() {
            @Override
            public void handleMsgFromBus(boolean stillConected, boolean readerRunning, Json payload) {
                // should not happen
                dl(payload.toString());
                info.lastMsgReceived = payload;
                info.msg1Received.countDown();
            }

            @Override
            public void handleException(boolean stillConected, boolean readerRunning, Exception e) {
                assertTrue(false);
                info.msg1Received.countDown();
            }
        });

        eb.send("echoXX", Json.object().set("content", "hello"));

        try {
            info.msg1Received.await();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        eb.close();


        // reply called with a fail!
        assertNotNull(info.lastMsgReceived);
        assertEquals(info.lastMsgReceived.at("type").asString(), "err");

        // message should be access denied...
        assertEquals(info.lastMsgReceived.has("message"), true);

    }




}
