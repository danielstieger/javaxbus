package org.modellwerkstatt.javaxbus;

public interface ConsumerHandler<T> {

    void handle(boolean error, T msg);

}
