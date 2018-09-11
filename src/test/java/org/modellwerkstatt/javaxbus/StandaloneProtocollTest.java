package org.modellwerkstatt.javaxbus;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import mjson.Json;

/**
 * Unit test for simple TestApp1.
 */
public class StandaloneProtocollTest extends TestCase {
    VertXProto proto = new VertXProto();

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public StandaloneProtocollTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( StandaloneProtocollTest.class );
    }


    public void test_ping()
    {
        assertEquals("ping", proto.ping().at("type").asString());
    }


    public void test_send()
    {
        Json s = proto.send("to", Json.object().set("content", "here"), "reply");

        assertEquals(s.at("type").asString(), "send");
        assertEquals(s.at("address").asString(), "to");
        assertEquals("here", s.at("body").at("content").asString());
        assertEquals("reply", s.at("replyAddress").asString());


    }

    public void test_publish()
    {
        Json s = proto.publish("to", Json.object().set("content", "here"), "reply");

        assertEquals(s.at("type").asString(), "publish");
        assertEquals(s.at("address").asString(), "to");
        assertEquals("here", s.at("body").at("content").asString());
        assertEquals("reply", s.at("replyAddress").asString());

    }

    public void test_register()
    {
        Json s = proto.register("myadr");

        assertEquals(s.at("type").asString(), "register");
        assertEquals(s.at("address").asString(), "myadr");

    }
    public void test_unregister()
    {
        Json s = proto.unregister("myadr");

        assertEquals(s.at("type").asString(), "unregister");
        assertEquals(s.at("address").asString(), "myadr");

    }
}
