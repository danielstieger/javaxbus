package org.modellwerkstatt.javaxbus;

import mjson.Json;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.Thread.interrupted;

public class EventBusCom implements Runnable {
    final static public int RECON_TIMEOUT = 10000;
    final static public int FAST_RECON_TIMEOUT = 500;

    private String hostname;
    private int port;
    private Socket socket;
    private DataInputStream reader;
    private DataOutputStream writer;
    private volatile boolean upNRunning;
    private volatile boolean stillConnected;
    private VertXProto proto;
    private HashMap<String, List<ConsumerHandler<Json>>> allHandlers;
    private List<ErrorHandler<Json> > errorHandler;
    private boolean underTest;

    public EventBusCom(){
        upNRunning = false;
        stillConnected = false;
        proto= new VertXProto ();
        allHandlers = new HashMap<String, List<ConsumerHandler<Json>>>();
        errorHandler = new ArrayList<ErrorHandler<Json> >();
        underTest = false;
    }


    public void sendToStream(boolean publish, String adr, Json obj, String reply){
        try {
            if (publish){
                proto.writeToStream(writer, proto.publish(adr, obj, reply));
            } else {
                proto.writeToStream(writer, proto.send(adr, obj, reply));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerHander(String adr, ConsumerHandler<Json> handler){
        synchronized (this) {
            try {
                if (!allHandlers.containsKey(adr)){
                    allHandlers.put(adr, new ArrayList<ConsumerHandler<Json>>());
                }

                List<ConsumerHandler<Json>> listOfHandlers = allHandlers.get(adr);
                listOfHandlers.add(handler);

                if (listOfHandlers.size() == 1){
                    proto.writeToStream(writer, proto.register(adr));
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void unRegisterHander(String adr, ConsumerHandler<Json> handler){
        synchronized (this) {
            try {
                if (!allHandlers.containsKey(adr)) {
                    throw new IllegalStateException("No handlers registered for adr " + adr);
                }

                List<ConsumerHandler<Json>> existingHandlers = allHandlers.get(adr);

                if (!existingHandlers.contains(handler)) {
                    throw new IllegalStateException("Handler not registered for adr " + adr);
                }

                existingHandlers.remove(handler);

                if (existingHandlers.size() == 0){
                    proto.writeToStream(writer, proto.unregister(adr));
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addErrorHandler(ErrorHandler<Json> handler){
        synchronized (this) {
            if (errorHandler.contains(handler)){
                throw new IllegalStateException("You should not register this handler twice.");
            }

            errorHandler.add(handler);
        }
    }

    public void removeErrorHandler(ErrorHandler<Json> handler){
        synchronized (this) {
            if (!errorHandler.contains(handler)){
                throw new IllegalStateException("The given handler was never registered.....");
            }

            errorHandler.remove(handler);
        }
    }


    private void dispatchMessage(String adr, Json msg){
        synchronized (this){
            if (!allHandlers.containsKey(adr)){
                throw new IllegalStateException("No handlers registered for " + adr + " but msg " + msg.toString() + " received.");
            }

            for (ConsumerHandler<Json> h : allHandlers.get(adr)) {
                h.handle(msg);
            }
        }
    }

    private void dispatchErrorFromBus(Json payload){
        synchronized (this){
            for (ErrorHandler<Json> e: errorHandler) {
                e.handleMsgFromBus(stillConnected, upNRunning, payload);
            }
        }
    }
    private void dispatchException(Exception exception){
        synchronized (this){
            for (ErrorHandler<Json> e: errorHandler) {
                e.handleException(stillConnected, upNRunning, exception);
            }
        }
    }

    @Override
    public void run() {
        upNRunning = true;

        while (!interrupted() && upNRunning) {
            try {
                if (stillConnected) {
                    Json msg = proto.readFormStream(reader);

                    String msgType = msg.at("type").asString();
                    if ("pong".equals(msgType)) {
                        // nice one

                    } else if ("message".equals(msgType)) {
                        if (upNRunning) {
                            // check upNRunning before dealing out messages ...
                            dispatchMessage(msg.at("address").asString(), msg);
                        }

                    } else if ("err".equals(msgType)) {
                        // call error Handler
                        dispatchErrorFromBus(msg);
                    }


                } else {
                    tryReconnect();

                }

            } catch (SocketException e) {
              stillConnected = false;
                if (upNRunning) {
                  dispatchException(e);
              }
              // else, ignore this one ...

            } catch (EOFException e) {
              stillConnected = false;
              // lost connection to vertx ..
              dispatchException(e);

            } catch (IOException e) {
                throw new RuntimeException(e);

            }
        }
    }

    private void tryReconnect() {
        try {
            closeCon();
        } catch (Exception e) {
            // ignore any ex
        }

        try {
            Thread.sleep(underTest ? FAST_RECON_TIMEOUT : RECON_TIMEOUT);

            if (!upNRunning) {
                throw new IllegalStateException("This can not happen");
            }
            initCon();

            // if that is successfull, we have to register handlers ...
            synchronized (this){
                for (String adr: allHandlers.keySet()) {
                    proto.writeToStream(writer, proto.register(adr));
                }
            }


        } catch (IOException e) {
          throw new RuntimeException(e);

        } catch (RuntimeException e) {
          if (e.getCause() != null && e.getCause().getClass().equals(ConnectException.class)){
              // ignore connect ex ...
          }else {
              throw e;
          }

        } catch (InterruptedException e) {
            // mh .. sleep interrupted ...

        }
    }

    public boolean isUpNRunning() {
        return upNRunning;
    }

    public void init(String hostname, int port){
        this.hostname = hostname;
        this.port = port;
        initCon();
    }


    private void initCon(){
        try {
            socket = new Socket(hostname, port);

            // from server
            reader = new DataInputStream(socket.getInputStream());
            // to server
            writer = new DataOutputStream(socket.getOutputStream());

            proto.writeToStream(writer, proto.ping());
            stillConnected = true;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isInitialized() {
        return socket != null;
    }
    public void setUnderTest(){
        underTest = true;
    }

    public void shutdown() {
        synchronized (this) {
            try {
                upNRunning = false;

                for (String adr: allHandlers.keySet()) {
                    proto.writeToStream(writer, proto.unregister(adr));
                    allHandlers.get(adr).clear();
                }

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
                stillConnected=false;
                reader.close();
                writer.close();
                socket.close();

            } catch (IOException e) {
                throw new RuntimeException(e);

            } finally {
                socket = null;

            }

        }
    }

}
