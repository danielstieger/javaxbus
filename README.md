# javaxbus
Simple Java Vert.x Eventbus TCP-Client (without any dependencies)

A [Vert.x EventBus](http://vertx.io/docs/vertx-core/java/#event_bus) client for java environments, using traditional Socket-IO or NIO.
IO and a spare thread to communicate with the [vert.x event-bus-tcp-bridge](https://github.com/vert-x3/vertx-tcp-eventbus-bridge). 

* a daemon thread is used to receive data from the vert.x tcp-event-bus-bridge which in turn calls registered consumer handlers
* you can register multiple handlers to the same address, all handlers will be called.
* the daemon thread will report problems and exceptions via registered error handlers (ensure you register one!)
* the thread will reconnect to the vert.x tcp-event-bus-bidge upon lost connection, reregistering used addresses
* exceptions might be thrown on initial connect (e.g. can not reach event-bus-tcp-bridge)
* exceptions are also thrown when sending messages using send() or publish(), i.e. messages are not queued until reconnect etc.
* calling close() will unreg. handlers, shutdown the receiver thread and close the connection. All ressources are cleaned up neatly. 
* the library draws heavily on [mjson](https://bolerio.github.io/mjson/), a json object implementation for java
* right now, only json payload as mjson object is supported (extension possible)
* you can use send() with a handler to respond to an expected reply by the server 
* if send() is used with a reply handler, server side fails are dispatched to that handler too (and not to the error handler)
* if send() with reply handler does not receive a reply within 30sec, an error will be received by the reply handler. See the vertx documentation for further information
* there is a traditional socket io and a java nio implementation available


# Usage
```java

EventBus bus = EventBus.create("localhost", 8089);

bus.consumer("hello", new ConsumerHandler<Json>() {
	@Override
	public void handleMsgFromBus(Message msg) {
		if (!msg.isErrorMsg()) {
    		System.err.println("Received " + msg.getBodyAsMJson().toString());
    	} else {
    		System.err.println("ERROR received " + msg.getErrMessage());
    	}
    	
    }
});
   
// send a message to myself :)
Json jsonPayLoad = Json.object().set("msg", "Hello World");
bus.send("hello", new Message(jsonPayLoad));


```



# Testing
`mvn test`. Will execute all available tests......  Among the tests are unit tests and some small test-apps. 
You will need a vert.x instance with a tcp-event-bus-bridge and permissions in/outbound for the 'echo' and the 'echo2' address 
(do not reg. any consumers on this adr.) in order to run the test-suit.   



# Building

`mvn package`. The lib can be found in the target/ folder after maven build and tested the package



# Dependencies
as already statet, javaxbus draws heavily on [mjson](https://bolerio.github.io/mjson/)



# More examples   
* There are 4 test-apps inclulded 
* The [centrix verticel](https://github.com/danielstieger/centrix) which comes with an echo handler 

