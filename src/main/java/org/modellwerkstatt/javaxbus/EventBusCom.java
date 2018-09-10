package org.modellwerkstatt.javaxbus;

import mjson.Json;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.Thread.interrupted;

public class EventBusCom implements Runnable {

    private String hostname;
    private int port;
    private Socket socket;
    private DataInputStream reader;
    private DataOutputStream writer;
    private boolean upNRunning;
    private VertXProto proto;
    private HashMap<String, List<ConsumerHandler<Json>>> allHandlers;
    private List<ErrorHandler<Json> > errorHandler;


    public EventBusCom(){
        upNRunning = false;
        proto= new VertXProto ();
        allHandlers = new HashMap<String, List<ConsumerHandler<Json>>>();
        errorHandler = new ArrayList<ErrorHandler<Json> >();
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

    private void dispatchError(Json payload){
        synchronized (this){
            for (ErrorHandler<Json> e: errorHandler) {
                e.handle(socket != null && socket.isConnected(), upNRunning, payload);
            }
        }
    }

    @Override
    public void run() {
        upNRunning = true;

        while (!interrupted() && upNRunning) {
            try {
                Json msg = proto.readFormStream(reader);

                String msgType = msg.at("type").asString();
                if ("pong".equals(msgType)){
                    // nice one

                } else if ("message".equals(msgType)) {
                    dispatchMessage(msg.at("address").asString(), msg);

                } else if ("err".equals(msgType)) {
                    // call error Handler
                    dispatchError(msg);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);

            }


        }
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

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isInitialized() {
        return socket != null;
    }

    private void closeCon(String hostname, int port){
        try {
            socket.close();
            reader.close();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);

        } finally {
            socket = null;
        }
    }
}
