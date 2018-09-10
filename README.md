# javaxbus
Simple Java Vertx  Eventbus  TCP-Client without any dependencies

A [Vert.x EventBus](http://vertx.io/docs/vertx-core/java/#event_bus) java client without any dependencies. Using TCP Socket
IO and a spare task to communicate with the vert.X evenbus tcp bridge. 


# Usage
```java

EventBus bus = EventBus.create("localhost", 8089);

        bus.consumer("keyer", new ConsumerHandler<Json>() {
            @Override
            public void handle(Json msg) {
                System.err.println("Received " + msg.at("body").asString());
            }
        });
        
```





# Testing
`TODO - insert here`

# Building

`TODO - insert here` . Find the jar at /lib

# Dependencies
none

# More examples can be found at  
* 
* 
