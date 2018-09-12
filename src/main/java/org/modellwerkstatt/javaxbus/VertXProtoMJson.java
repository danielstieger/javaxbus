package org.modellwerkstatt.javaxbus;

import mjson.Json;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class VertXProtoMJson {
    private Charset utf8Charset;


    public VertXProtoMJson(){
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


    public Message prepareMessageToDeliver(String type, Json json) {
        Message msgToDeliver;

        // both might have a replyAddr ?
        String reply = json.has("replyAddress") ? json.at("replyAddress").asString(): null;

        if ("message".equals(type)) {
            // message has always address, body and send flag.
            String address = json.at("address").asString();
            Json body = json.at("body");
            boolean sended = json.at("send").asBoolean();

            msgToDeliver = new Message(address, sended, reply, body);

        } else {
            // might not have a address
            String address = json.has("address") ? json.at("address").asString() : null;

            String failMsg = json.at("message").asString();
            String failCode = json.has("failureCode") ? json.at("failureCode").asString() : "";
            String failType = json.has("failureType") ? json.at("failureType").asString() : "";

            // unsure about that one.
            boolean send = json.has("send") ? json.at("send").asBoolean() : true;

            msgToDeliver = new Message(address, send, reply, failMsg, failCode, failType);

        }

        return msgToDeliver;
    }

}
