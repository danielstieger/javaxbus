package org.modellwerkstatt.javaxbus;

import mjson.Json;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;

public class TraditionalSocketIO implements IOCapabilities {

    private Socket socket;
    private DataInputStream reader;
    private DataOutputStream writer;
    private Charset utf8Charset = Charset.forName("UTF-8");;


    public TraditionalSocketIO(){

    }

    public void init(String hostname, int port) throws IOException {
        socket = new Socket(hostname, port);
        // from server
        reader = new DataInputStream(socket.getInputStream());
        // to server
        writer = new DataOutputStream(socket.getOutputStream());
    }

    public void close() throws IOException {
        reader.close();
        writer.close();
        socket.close();
    }

    synchronized public void writeToStream(Json msg) throws IOException {

        String jsonPayLoad = msg.toString();
        byte[] asBytes = jsonPayLoad.getBytes(utf8Charset);

        // long millis = System.currentTimeMillis();


        // big endian
        writer.writeInt(asBytes.length);
        writer.write(asBytes);
        // writer.flush();

        // long result = System.currentTimeMillis() - millis;
        // System.err.println("T " + result  +" ms <-- " + jsonPayLoad);

    }

    public Json readFormStream() throws IOException {
        // read complete msg
        int len = reader.readInt();
        byte[] message = new byte[len];
        reader.readFully(message, 0, len);
        // System.err.println("<<-- " + new String(message));

        String jsonMsg = new String(message, "UTF-8");
        return Json.read(jsonMsg);
    }

}
