package org.modellwerkstatt.javaxbus;

import mjson.Json;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Hello world!
 *
 */
public class App 
{

    public static void main( String[] args )  {
        EventBus ev = EventBus.create("localhost", 8089);

        ev.addErrorHandler(new ErrorHandler<Json>() {
            @Override
            public void handleMsgFromBus(boolean stillConected, boolean readerRunning, Json payload) {
                System.err.println("Connected " + stillConected + ", reader is running "+ readerRunning + "  some payload? "+ payload.toString());
            }

            @Override
            public void handleException(boolean stillConected, boolean readerRunning, Exception e) {

            }
        });

        final CountDownLatch latch = new CountDownLatch(1);
        ev.consumer("echo", new ConsumerHandler<Json>() {
            @Override
            public void handle(Json msg) {
                System.err.println("Received msg " + msg.toString());
                latch.countDown();
            }
        });

        ev.send("echo", Json.object().set("from", "dan").set("content", "hello world"));

        try {
            latch.await();
            ev.close();

            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
