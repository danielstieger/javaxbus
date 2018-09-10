# javaxbus
Simple Java Vertx  Eventbus  TCP-Client without any dependencies

A [Vert.x EventBus](http://vertx.io/docs/vertx-core/java/#event_bus) java client without any dependencies. Using TCP Socket
IO and a spare task to communicate with the vert.X evenbus tcp bridge. 


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
`mvn test`. Will execute all available tests......  

# Building

`mvn package`. The lib can be found in the target/ folder after maven build and tested the package

# Dependencies
none

# More examples can be found at  
* 
* 
