package org.modellwerkstatt.javaxbus;

import mjson.Json;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class VertXProto {
    private Charset utf8Charset;


    public VertXProto(){
        utf8Charset = Charset.forName("UTF-8");
    }


    public Json send(String adr, Json msg, String replyAddr){
        Json json = Json.object();
        json.set("type", "send");
        json.set("address", adr);
        json.set("body", msg);
        if (replyAddr != null){
            json.set("replyAddress", replyAddr);
        }
        return json;
    }

    public Json publish(String adr, Json msg, String replyAddr){
        Json sendJson = send(adr, msg, replyAddr);
        sendJson.set("type", "publish");
        return sendJson;
    }

    public Json register(String adr){
        Json json = Json.object();
        json.set("type", "register");
        json.set("address", adr);
        return json;
    }

    public Json unregister(String adr){
        Json json = Json.object();
        json.set("type", "unregister");
        json.set("address", adr);
        return json;
    }

    public Json ping(){
        return Json.object().set("type", "ping");
    }

    synchronized public void writeToStream(DataOutputStream stream, Json msg) throws IOException {
        String jsonPayLoad = msg.toString();
        byte[] asBytes = jsonPayLoad.getBytes(utf8Charset);

        // big endian
        stream.writeInt(asBytes.length);
        stream.write(asBytes);
        stream.flush();
        // System.err.println("-->> " + new String(asBytes));
    }

    public Json readFormStream(DataInputStream stream) throws IOException {
        // read complete msg
        int len = stream.readInt();
        byte[] message = new byte[len];
        stream.readFully(message, 0, len);
        // System.err.println("<<-- " + new String(message));

        String jsonMsg = new String(message, "UTF-8");
        return Json.read(jsonMsg);
    }

}
