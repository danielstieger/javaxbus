package org.modellwerkstatt.javaxbus;

import mjson.Json;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class NonBlockingIO implements IOSocketService {
    final static public int DEFAULT_READ_BUFFER_SIZE = 16000;
    private SocketAddress address;
    private SocketChannel socketChannel;
    private Selector selector;
    private SelectionKey selectionKey;


    private Charset utf8Charset = Charset.forName("UTF-8");
    private ByteBuffer readBuffer;

    @Override
    public void init(String hostname, int port) throws IOException {
        readBuffer = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);

        address = new InetSocketAddress(hostname, port);

        socketChannel = SocketChannel.open(address);

        // not necessary, write takes 0ms
        socketChannel.configureBlocking(false);

        selector = Selector.open();
        selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);

    }

    @Override
    public void writeToStream(Json msg) throws IOException {
        String jsonPayLoad = msg.toString();
        byte[] asBytes = jsonPayLoad.getBytes(utf8Charset);

        ByteBuffer buffer = ByteBuffer.allocate(asBytes.length + 4);
        buffer.putInt(asBytes.length);
        buffer.put(asBytes);
        buffer.flip();
        socketChannel.write(buffer);

        // System.err.println("-->" + jsonPayLoad);
    }

    @Override
    public Json readFormStream() throws IOException {
        // block n read ...
        int channelsRead = selector.select();

        readBuffer.clear();
        readBuffer.limit(4);
        while (readBuffer.hasRemaining()) {
            if(socketChannel.read(readBuffer) == -1) throw new EOFException("Socket closed, read returned -1");
        }
        readBuffer.rewind();
        int length = readBuffer.getInt();

        readBuffer.clear();
        readBuffer.limit(length);
        while (readBuffer.hasRemaining()) {
            if(socketChannel.read(readBuffer) == -1) throw new EOFException("Socket closed, read returned -1");
        }

        readBuffer.flip();

        byte[] bytesForString = new byte[length];
        readBuffer.get(bytesForString, 0, length);
        String jsonMsg = new String(bytesForString, "UTF-8");

        // System.err.println("<-- " + jsonMsg);
        return Json.read(jsonMsg);
    }

    @Override
    public void close() throws IOException {
        selector.close();
        socketChannel.close();

    }
}
