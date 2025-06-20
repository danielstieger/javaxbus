/*
 * EventBus.java
 * <daniel.stieger@modellwerkstatt.org>
 *
 *
 * VertX 3 EventBus client in plain java. This is the public API of this eventbus client.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package org.modellwerkstatt.javaxbus;


import mjson.Json;



public class EventBus {
    static public final String VERSION = "1.0";
    static public final boolean USE_NIO = true;
    private Thread communicatorThread;
    private EventBusRunnable com;

    public EventBus(){

    }


    public void consumer(String address, ConsumerHandler handler) {
        if (com == null) {
            throw new IllegalStateException("Eventbus not initialized.");
        }
        com.registerHander(address, handler, true);
    }

    public void unregisgterConsumer(String address, ConsumerHandler handler) {
        if (com == null) {
            throw new IllegalStateException("Eventbus not initialized.");
        }
        com.unRegisterHander(address, handler);
    }

    public void addErrorHandler(ErrorHandler handler){
        if (com == null) {
            throw new IllegalStateException("Eventbus not initialized.");
        }
        com.addErrorHandler(handler);
    }

    public void removeErrorHandler(ErrorHandler handler){
        if (com == null) {
            throw new IllegalStateException("Eventbus not initialized.");
        }
        com.addErrorHandler(handler);
    }

    public void send(String adr, Json content){
        if (com == null) {
            throw new IllegalStateException("Eventbus not initialized.");
        }
        com.sendToStream(false, adr, content, null);
    }

    public void send(String adr, Json content, ConsumerHandler replyHandler){
        if (com == null) {
            throw new IllegalStateException("Eventbus not initialized.");
        }
        com.sendToStream(false, adr, content, replyHandler);
    }

    public void publish(String adr, Json content){
        if (com == null) {
            throw new IllegalStateException("Eventbus not initialized.");
        }
        com.sendToStream(true, adr, content, null);
    }

    public boolean isConnected(){
        if (com == null) {
            // was not initialiized or maybe already closed...
            return false;
        }
        return com.isConnected();
    }

    public boolean isUpAndRunning(){
        if (com == null) {
            // was not initialiized or maybe already closed...
            return false;
        }
        return com.isUpNRunning();
    }

    public boolean testReconnect() {
        com.tryReconnect();
        return true;
    }

    public void close() {
        if (com == null) {
            throw new IllegalStateException("Eventbus not initialized.");
        }

        com.shutdown();
        // this will probably not work on socket i/o
        communicatorThread.interrupt();
        // however, close will shutown the thread
        com.closeCon();

        communicatorThread = null;
        com = null;
    }

    public void setUnderTestingMode(){
        if (com == null) {
            throw new IllegalStateException("Eventbus not initialized.");
        }
        com.setUnderTest();
    }
    private void init(String hostname, int port) {

        com = new EventBusRunnable();
        com.init(hostname, port);

        communicatorThread = new Thread(com);
        communicatorThread.setName("VertX EventBus Recv.");
        communicatorThread.setDaemon(true);
        communicatorThread.start();

    }


    public static EventBus create (String hostname, int port) {
        EventBus bus = new EventBus();
        bus.init(hostname, port);
        return bus;
    }
}



