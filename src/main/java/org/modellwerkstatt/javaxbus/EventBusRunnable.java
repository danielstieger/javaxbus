package org.modellwerkstatt.javaxbus;

import mjson.Json;

import java.io.*;
import java.net.ConnectException;
import java.net.SocketException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.Thread.interrupted;

public class EventBusRunnable implements Runnable {

    final static public int     RECON_TIMEOUT = 10000;
    final static public int FAST_RECON_TIMEOUT = 500;
    final static public String TEMP_HANDLER_SIGNATURE = "__MODWERK_HC__";


    private String hostname;
    private int port;
    private IOSocketService io;

    private volatile boolean upNRunning;
    private volatile boolean stillConnected;
    private VertXProtoMJson proto;
    private HashMap<String, List<ConsumerHandler>> consumerHandlers;

    private List<ErrorHandler> errorHandler;
    private boolean underTest;

    public EventBusRunnable(){
        upNRunning = false;
        stillConnected = false;
        proto= new VertXProtoMJson();
        io = null;
        consumerHandlers = new HashMap<String, List<ConsumerHandler>>();
        errorHandler = new ArrayList<ErrorHandler >();
        underTest = false;
    }

    public void sendToStream(boolean publish, String adr, Json msg, ConsumerHandler replyHandler){
        try {
            String replyAdr = null;
            if (replyHandler != null) {
                replyAdr = adr + TEMP_HANDLER_SIGNATURE + replyHandler.hashCode();
                registerHander(replyAdr, replyHandler, false);
            }

            if (publish){
                io.writeToStream(proto.publish(adr, msg, replyAdr));
            } else {
                io.writeToStream(proto.send(adr, msg, replyAdr));
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerHander(String adr, ConsumerHandler handler, boolean registerWithServer){
        synchronized (this) {
            try {
                if (!consumerHandlers.containsKey(adr)){
                    consumerHandlers.put(adr, new ArrayList<ConsumerHandler>());
                }

                List<ConsumerHandler> listOfHandlers = consumerHandlers.get(adr);
                listOfHandlers.add(handler);

                if (listOfHandlers.size() == 1 && registerWithServer){
                    io.writeToStream(proto.register(adr));
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void unRegisterHander(String adr, ConsumerHandler handler){
        synchronized (this) {
            try {
                if (!consumerHandlers.containsKey(adr)) {
                    throw new IllegalStateException("No handlers registered for adr " + adr);
                }

                List<ConsumerHandler> existingHandlers = consumerHandlers.get(adr);

                if (!existingHandlers.contains(handler)) {
                    throw new IllegalStateException("Handler not registered for adr " + adr);
                }

                existingHandlers.remove(handler);

                if (existingHandlers.size() == 0){
                    io.writeToStream(proto.unregister(adr));
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addErrorHandler(ErrorHandler handler){
        synchronized (this) {
            if (errorHandler.contains(handler)){
                throw new IllegalStateException("You should not register this handler twice.");
            }

            errorHandler.add(handler);
        }
    }

    public void removeErrorHandler(ErrorHandler handler){
        synchronized (this) {
            if (!errorHandler.contains(handler)){
                throw new IllegalStateException("The given handler was never registered.....");
            }

            errorHandler.remove(handler);
        }
    }


    private void dispatchMessage(String adr, Message msg){
        synchronized (this){
            if (!consumerHandlers.containsKey(adr)){
                throw new IllegalStateException("No handlers registered for " + adr + " but msg " + msg.toString() + " received.");
            }

            List<ConsumerHandler> handlers = consumerHandlers.get(adr);
            for (ConsumerHandler h : handlers) {
                h.handle(msg);
            }

            if (adr.contains(TEMP_HANDLER_SIGNATURE)) {
                consumerHandlers.get(adr).clear();
            }
        }
    }

    private void dispatchErrorFromBus(Message msg){
        synchronized (this){
            if (errorHandler.size() == 0) {
                if ("unknown_address".equals(msg.getErrMessage()) && "".equals(msg.getErrFailureCode()) && "".equals(msg.getErrFailureType())) {
                    // ignore, nown return on our platform
                } else {
                    System.err.println("EventBusRunnable.dispatchErrorFromBus(): no errorHandlers registered, but received msg:" +  msg.toString());
                }

            } else {
                for (ErrorHandler e: errorHandler) {
                    e.handleMsgFromBus(stillConnected, upNRunning, msg);
                }
            }
        }
    }
    private void dispatchException(Exception exception){
        synchronized (this){
            if (errorHandler.size() == 0) {
                exception.printStackTrace();

            } else {
                for (ErrorHandler e: errorHandler) {
                    e.handleException(stillConnected, upNRunning, exception);
                }
            }

        }
    }

    @Override
    public void run() {
        upNRunning = true;

        while (!interrupted() && upNRunning) {
            try {
                if (stillConnected) {
                    Json msg = io.readFormStream();

                    String msgType = msg.at("type").asString();

                    if ("pong".equals(msgType)) {
                        // nice one

                    } else if ("message".equals(msgType)) {
                        if (upNRunning) {
                            Message msgToSend = proto.prepareMessageToDeliver(msgType, msg);
                            dispatchMessage(msgToSend.getAddress(), msgToSend);
                        }

                    } else if ("err".equals(msgType)) {
                        // is there an address set?
                        if (upNRunning && msg.has("address")) {
                            Message msgToSend = proto.prepareMessageToDeliver(msgType, msg);
                            dispatchMessage(msgToSend.getAddress(), msgToSend);

                        } else {
                            Message msgToSend = proto.prepareMessageToDeliver(msgType, msg);
                            // call error Handler
                            dispatchErrorFromBus(msgToSend);
                        }
                    }


                } else {
                    tryReconnect();

                }

            } catch (SocketException e) {
              stillConnected = false; // this issues a reconnect ..
              if (upNRunning) {
                dispatchException(e);
              }
              // else, ignore this one, might be a shutdown for traditional

            } catch (ClosedByInterruptException e) {
                stillConnected = false; // this issues a reconnect ..
                if (upNRunning) {
                    dispatchException(e);
                }
                // else, ignore this one, might be a shutdown for NIO

            } catch(ClosedChannelException e) {
                stillConnected = false; // this issues a reconnect ..
                if (upNRunning) {
                    dispatchException(e);
                }
                // else, ignore this one, might be a shutdown for NIO

            } catch (EOFException e) {
              stillConnected = false;
              // lost connection to vertx ..
              dispatchException(e);

            } catch (IOException e) {
              stillConnected = false;
              dispatchException(e);

            } catch (Exception e){
              // just try to reconnect ...
              stillConnected = false;
              dispatchException(e);
            }
        }
    }

    public void tryReconnect() {
        try {
            closeCon();
        } catch (Exception e) {
            // ignore any ex
        }

        try {
            Thread.sleep(underTest ? FAST_RECON_TIMEOUT : RECON_TIMEOUT);

            // shutdown while sleeping ?
            if (upNRunning) {
                initCon();

                // if that is successfull, we have to register handlers ...
                synchronized (this){
                    for (String adr: consumerHandlers.keySet()) {
                        io.writeToStream(proto.register(adr));
                    }
                }
            }

        } catch (IOException e) {
            stillConnected = false;
            dispatchException(e);

        } catch (RuntimeException e) {
          if (e.getCause() != null && e.getCause().getClass().equals(ConnectException.class)){
              // ignore connect ex ... we are reconnecting anyway
          } else {
              stillConnected = false;
              dispatchException(e);
          }

        } catch (InterruptedException e) {
            // mh .. sleep interrupted ...

        } catch (Exception e) {
            stillConnected = false;
            dispatchException(e);

        }
    }

    public boolean isUpNRunning() {
        return upNRunning;
    }
    public boolean isConnected() {
        return stillConnected;
    }


    public void init(String hostname, int port){
        this.hostname = hostname;
        this.port = port;
        initCon();
    }


    private void initCon(){
        try {
            if (EventBus.USE_NIO) {
                io = new NonBlockingIO();
            } else {
                io = new TraditionalSocketIO();
            }

            io.init(hostname, port);
            io.writeToStream(proto.ping());
            stillConnected = true;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setUnderTest(){
        underTest = true;
    }

    public void shutdown() {
        synchronized (this) {
            try {
                upNRunning = false;

                if (stillConnected) {
                    for (String adr: consumerHandlers.keySet()) {
                        if (adr.contains(TEMP_HANDLER_SIGNATURE)) {
                            // do not unregister this one, since this is only a reply handler
                        } else {
                            io.writeToStream(proto.unregister(adr));
                        }
                        consumerHandlers.get(adr).clear();
                    }
                }

                consumerHandlers.clear();

                // remove all error handlers
                errorHandler.clear();

            } catch (IOException e) {
                throw new RuntimeException(e);

            }
        }
    }

    public void closeCon(){
        synchronized (this) {
            try {
                if (stillConnected) {
                    stillConnected = false;
                    io.close();
                }


            } catch (IOException e) {
                throw new RuntimeException(e);

            }

        }
    }

}
