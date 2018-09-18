package org.modellwerkstatt.javaxbus;

import mjson.Json;

public class HeavyWriter implements Runnable {

    private volatile boolean runLoop;
    private long cnt;
    private EventBus bus;
    private String writerName;

    public HeavyWriter(EventBus e, String name) {
        runLoop = true;
        cnt = 0;
        bus = e;
        writerName = name;
    }

    public void run() {

        while (runLoop) {
            Json json = Json.object().set("millis", System.currentTimeMillis());
            json.set("userName", writerName);
            json.set("userId", 0);
            json.set("message", "Hello from " + writerName + " / " + cnt);

            // long mil = System.currentTimeMillis();
            bus.send("globallog", json);
            // long diff = System.currentTimeMillis() - mil;
            // System.err.println(" " + diff + "ms");

            cnt++;
        }

    }


    public void stopLoop() {
        runLoop = false;
    }

    public long getCount() {
        return cnt;
    }

}