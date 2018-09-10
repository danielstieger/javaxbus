package org.modellwerkstatt.javaxbus;

public interface ConsumerHandler<T> {

    void handle(T msg);

}
