package org.modellwerkstatt.javaxbus;

import mjson.Json;

import java.io.IOException;

public interface IOCapabilities {

    void init(String hostname, int port) throws IOException;
    void writeToStream(Json msg) throws IOException;
    Json readFormStream() throws IOException;
    void close() throws IOException;

}
