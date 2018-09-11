# javaxbus
Simple Java Vertx  Eventbus  TCP-Client without any dependencies

A [Vert.x EventBus](http://vertx.io/docs/vertx-core/java/#event_bus) java client without any dependencies. Using TCP Socket
IO and a spare thread to communicate with the vert.X evenbus tcp bridge. 

* a daemon thread is used to receive data from the vert.x tcp-event-bus-bridge which in turn calls registered consumer handlers
* the daemon thread will report problems and exceptions via registered error handlers (ensure you register one!)
* the thread will reconnect to the vert.x tcp-event-bus-bidge upon lost connection, reregistering used addresses
* exceptions might be thrown on initial connect
* exceptions are also thrown on send/publish of messages, i.e. messages are not queued until reconnect etc.
* calling close() will unreg. handlers, shutdown the receiver thread and close the connection, cleaning up all ressources neatly 
* the library draws heavily on [mjson](https://bolerio.github.io/mjson/), a json object implementation for java


# Usage
```java

EventBus bus = EventBus.create("localhost", 8089);

bus.consumer("keyer", new ConsumerHandler<Json>() {
	@Override
	public void handleMsgFromBus(Json msg) {
    	System.err.println("Received " + msg.at("body").asString());
    }
});
   
bus.send("keyer", Json.object().set("msg", "Hello World"));
     
```

# Testing
`mvn test`. Will execute all available tests......  Among the tests are unit tests and a small app as an integration test. 
You will need a vert.x instance with a tcp-event-bus-bridge and permissions in/outbound for the 'echo' address (do not reg.
any consumers on this adr.)   

# Building

`mvn package`. The lib can be found in the target/ folder after maven build and tested the package


# Dependencies
as already statet, javaxbus draws heavily on [mjson](https://bolerio.github.io/mjson/)

# More examples can be found at  
*  
* 
