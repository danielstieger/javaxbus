package org.modellwerkstatt.javaxbus;

import mjson.Json;

import java.awt.*;
import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{

    public static void main( String[] args )  {

        EventBus bus = EventBus.create("localhost", 8089);

        bus.consumer("keyer", new ConsumerHandler<Json>() {
            @Override
            public void handle(Json msg) {
                System.err.println("Received " + msg);
            }
        });


        try {
            System.in.read();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
