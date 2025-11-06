# Firecord

Firecord is a virtual distributed object memory system that provides (somewhat) transparent object synchronization across multiple independent processes and machines. It implements a virtual shared object space using Redis as the backing store, combined with message-passing for cache coherence, allowing distributed applications to interact with shared objects as if they were local references.

## Comparison to Redisson

Redisson is a popular Java client for Redis that provides distributed data structures and services. Redisson is an actual production-ready library with a wide range of features, including distributed locks, maps, sets, lists, and more. Firecord is completely reference-based, trying to mimic local object access as closely as possible, while Redisson provides distributed data structures that require explicit API calls to interact with.

For example, if you put an object into a Redisson map, it is serialized at that moment and stored in Redis. If you later modify the object locally, those changes are not automatically reflected in the map until you explicitly put the updated object back into the map. In contrast, firecord only remebers that we put the reference to the object into the map, and when we change the object later, both the map and all other references to that object across the distributed system see the updated value automatically since it's the same object in the distributed memory. Also, firecord distinguishes between value changes and reference changes, and well it uses AspectJ to detect reference changes automatically.
A major downside of firecord's approach is the reliance on AspectJ bytecode weaving, which can introduce complexity and potential performance overhead. Redisson, on the other hand, uses standard Java APIs and does not require bytecode manipulation. Also, it's much more mature and feature-rich compared to firecord. So for the vast majority of use cases, Redisson is likely the better choice.

## Purpose

For a lot of real-time applications that need to share state across multiple nodes, using an in-memory database such as Redis is a common approach. Often, the access times of 1-2ms (if node and server are on the same machine) are sufficient for many use cases, if the database is on a different machine the latency can be higher depending on network conditions.

However, certain applications require even lower latencies. For example, when simulating logic for a shared world, like in multiplayer games, repeatedly querying a database for every little change can introduce delays that far exceed the time allotted for a single tick or frame update. As firecord is designed to be used in Minecraft server environments, where tick times of 50ms (20 ticks per second) are the target, accumulated latencies from database queries can lead to noticeable lag and a poor user experience.

Therefore, firecord caches objects locally in each node and only propagates changes to other nodes when necessary. This way, applications can achieve near-instantaneous access to the shared state while still maintaining consistency across the distributed system.
Thus, the developer can work with distributed objects almost as if they were local, without worrying about the underlying network communication, dramatically simplifying the development of distributed applications.
It also keeps network traffic low by only sending incremental updates rather than full object serializations.
This is achieved by fine-grained synchronization at the field level rather than object level—when a field changes, only that specific change is propagated. Reference updates are detected through AspectJ bytecode weaving, enabling the system to distinguish between value changes and "pointer" reassignments.

## Downsides

- **Inefficient Memory Usage**: Each node maintains its own cache of shared objects, which can lead to increased memory consumption compared to a centralized database approach.
    - Four nodes will roughly quadruple the memory usage for the shared objects compared to a single node setup - and that's excluding the memory used by Redis itself which also stores all objects in RAM.
- **Complexity**: The system introduces additional complexity in terms of setup, configuration, and understanding the underlying mechanisms.
    - Unfortunately, distributed shared memory systems are inherently complex, and we can't get around the issue that locks and other synchronization mechanisms are required to ensure consistency in some scenarios, which is a burden on the developer.
- **Overhead**: While it reduces latency for many operations, there is still overhead associated with maintaining cache coherence and handling network communication.
- **Limited Scalability**: The current implementation may not scale well to very large numbers of nodes or extremely high update rates without further optimization.
    - Sharding and load balancing strategies would be required for larger deployments, but are not implemented (or planned to be supported) yet.
- **Experimental**: As a research prototype, it may lack certain features or robustness required for production use.
    - It really would require a complete rewrite to be production ready, as the architecture is likely to change significantly when adding missing features such as garbage collection, sorted set support, parent references, etc.

## Disclaimer

This README is mostly AI generated because I really do not believe a prototype such as this truly requires an elaborate documentation yet, but some documentation is still better than none, and I have proofread and edited the generated text to ensure some level of accuracy. The code itself however does not have the same problem. Letting generative AI write this code would just not work out well anyway.

## Features

- **Distributed Object Memory**: Provides (somewhat) transparent shared object semantics over a distributed system using Redis as the virtual object store
- **Cache Coherence Protocol**: Uses Redis Pub/Sub channels for invalidation-based cache coherence across nodes
- **Fine-Grained Synchronization**: Updates individual fields rather than entire objects, detected through AspectJ bytecode weaving
- **Reference vs. Value Change Detection**: Distinguishes between field value modifications and reference reassignments
- **Selective Synchronization**: Classes can opt-out of synchronization using `@ClassAnnotation(synchronize = false)`
- **Multi-Platform Support**: Works across different machines or multiple processes on the same machine
- **Platform Integration**: Native support for Minecraft servers (Spigot/Paper/Velocity) plus standalone mode
- **Lazy Loading**: Objects and their reference graphs are loaded on-demand from Redis
- **Experimental Prototype**: Research-focused implementation exploring virtual distributed object memory concepts in Java

## How It Works

Firecord implements a **Virtual distributed object memory** architecture that simulates shared object space across multiple nodes:

### Architecture Overview

1. **Virtual Shared Object Space**: Redis serves as the coherent shared storage that all nodes can access, acting as the "object store" in a distributed system
2. **Message Passing for Coherence**: Redis Pub/Sub channels maintain cache coherence through invalidation-based protocols
3. **Transparent Access**: Applications interact with `AbstractData` objects as if they were local, while the system handles distributed synchronization
4. **AspectJ Interception**: Bytecode weaving intercepts field assignments to detect and broadcast changes

### Technical Components

- **`JedisCommunication`**: Core communication layer managing Redis Pub/Sub for change propagation
- **`JedisCommunicationChannel`**: Defines broadcast channels for different update types:
  - `REFERENCE_UPDATE`: Object reference changes (detected by AspectJ)
  - `UPDATE_SMALL_KEY`/`UPDATE_LARGE_KEY`: Value modifications
  - Collection operations: `LIST_ADD`, `MAP_PUT`, `SET_REMOVE`, etc.
- **`AbstractData`**: Base class for all synchronized objects, handles local caching and remote updates
- **`MessageReceiver`**: Processes incoming synchronization messages and maintains local cache coherence
- **`FieldListener`**: AspectJ aspect that intercepts field assignments on `AbstractObject` subclasses

### Update Broadcasting Process

1. **Change Detection**: AspectJ intercepts field modifications in `AbstractObject` instances
2. **Classification**: System determines if it's a value change or reference reassignment  
3. **Broadcasting**: Updates sent via `JedisCommunication.broadcast()` to appropriate channels
4. **Cache Invalidation**: Other nodes receive updates and refresh their local caches
5. **Event Notification**: Local listeners are notified of remote changes

This approach provides strong consistency while minimizing network overhead by sending only incremental changes rather than full object serialization.

## Usage

### Initialization

Initialize Firecord with a node type. The system will automatically handle Redis connection and channel subscriptions:

```java
// Standalone mode (non-Minecraft applications)
Firecord.init(NodeType.STANDALONE);

// Minecraft server modes
Firecord.init(NodeType.SPIGOT);   // For Spigot/Paper servers
Firecord.init(NodeType.VELOCITY); // For Velocity proxy servers
```

While firecord has excplicit support for Minecraft server environments, it can also be used in any Java application as a standalone virtual distributed object memory system. It also features an incomplete CLI for testing purposes.

### Synchronizing Objects

Objects extending `AbstractData` are automatically synchronized across nodes. The system handles both value updates and reference changes (with certain limitations). Everything that is not extended from AbstractData is ignored by the synchronization system.


Generally speaking, each AbstractData based object must have a constructor taking a Bytes key as parameter, which is used to identify the object in the distributed memory (Redis). This constructor is called when loading the object from the distributed memory automatically. Additionally, one can provide extra constructors for easier initialization of fields when creating a new object manually.

However, most of the time we do not want to call the constructor manually, as if we do so, the object is created new instead of being loaded from the distributed memory. That could lead to duplicate objects if the object is already present in the distributed memory, causing all kinds of issues.
```java
// basic way to create a new object (not recommended for objects that are part of the distributed memory already)
RInteger myInt = new RInteger(new Bytes("myIntKey"), 5); // creates a new RInteger with initial value 5

// recommended way to get an object from the distributed memory (creates it if it doesn't exist yet)
RInteger myInt = (RInteger) AbstractData.create(new Bytes("myIntKey")); // loads existing RInteger

if(myInt == null){
    // due to some reason (the object potentially not existing in the database yet) the object could not be loaded
    myInt = new RInteger(new Bytes("myIntKey"), 5); // create it manually
}
```

Once we have an object, we can modify its fields and the changes will be propagated automatically. Small simple objects like RInteger, RString, etc., will directly broadcast the value immediately when changed.

```java
myInt.set(10); // Broadcasts UPDATE_SMALL_KEY message
```

Large simple objects like RByteArray or RJson will only notify that the value changed, but will not send the new value immediately. Instead, other nodes will load the new value from Redis when they need it - or after a delay to prevent flooding the network with large updates.

```java
RByteArray byteArray = AbstractData.create(new Bytes("byteArrayKey"));
if(byteArray == null){
    byteArray = new RByteArray(new Bytes("byteArrayKey"), new byte[1024 * 1024]); // 1MB byte array
} 
byteArray.set(new byte[512 * 1024]); // Broadcasts UPDATE_LARGE_KEY message, other nodes will load the new value from Redis when needed
```
### Bytes Class

Bytes is a simple wrapper around byte[] that provides efficient serialization and comparison methods (hashcode, equals, etc.). Besides them using the `new Bytes(byte[])` or `new Bytes(String)` constructors primarily, it's relevant to note that Bytes objects are immutable. Once created, their content cannot be changed. This immutability ensures that Bytes instances can be safely used as keys in maps or sets without the risk of their hash codes changing over time. Lastly, the `toString()` method provides a convenient way to represent the byte array as hexadecimal strings for debugging or logging purposes. If one used `new Bytes("example")` and wants to retrieve the original string, one can use the `asString()` method - which can throw an exception if the byte array does not represent a valid UTF-8 string.


### Static Fields

Static fields in classes extending `AbstractObject` can also be synchronized across nodes. Their keys in the distributed memory are derived from the class name and variable name, allowing easy access without manual key management.

Ideally, you start the reference tree from a static field in a class, as this way you can easily access the root object from anywhere in your code without needing to pass references around.

```java
// static field's key (adress in the distributed memory) are derived from the variable name and class name
// they're also loaded (or initialized) automatically on first access (when the class is loaded)
// downside: if the class is renamed or obfuscated, the key changes and a new object is created in redis
static RMap<RInteger> testMap;
// Create/access a synchronized map

public static void exampleUsage(){
    RInteger value = testMap.get(new Bytes("key1")); // Loads existing value or null if not present
    if(value == null){
        value = new RInteger(new Bytes("key1"), new Bytes("value1"));
    }
    testMap.put(new Bytes("key1"), value); // Broadcasts MAP_PUT message
}
```

### Reference Changes

When changing references to other `AbstractObject` instances, the system detects these changes via AspectJ and broadcasts a `REFERENCE_UPDATE` message. This allows the system to maintain consistency across nodes when object references are reassigned.

```java
    // Reference changes are detected by AspectJ
    MyObject obj = new MyObject(new Bytes("obj"));
    obj.field = new RString(new Bytes("newField")); // Broadcasts REFERENCE_UPDATE message
```

assuming that `MyObject` extends `AbstractObject` and `field` is a field of type `RString`. See the Advanced Example section below for a complete example.

Besides object references, collections such as `RList`, `RMap`, and `RSet` also support reference changes for their elements. Adding or removing elements from these collections will broadcast the appropriate messages to keep other nodes in sync.

```java
RList<RString> list = AbstractData.create(new Bytes("myList"));
if(list == null){
    list = new RList<>(new Bytes("myList"));
}
list.add(new RString(new Bytes("element1"))); // Broadcasts LIST_ADD message
list.remove(new RString(new Bytes("element1"))); // Broadcasts LIST_REMOVE message
```

### Selective Synchronization

Classes can opt-out of distributed synchronization while still maintaining local functionality:

```java
@ClassAnnotation(synchronize = false)
public class LocalOnlyObject extends AbstractObject {
    public RInteger value; // Won't be synchronized across nodes
}
```

to broadcast a test message and observe object change propagation. The commands are only available in spigot/velocity, not in the standalone mode.

## Advanced Example: Custom Class Synchronization

Consider a custom class that extends AbstractObject. In this example, we define a class MyData with two fields: one primitive-like field and one reference field to another AbstractObject subclass. When an object of MyData is loaded as part of the reference tree, its fields are automatically populated if the corresponding entries exist in Redis, otherwise they are created and written to the database.

```java
// Example custom class
public class MyData extends AbstractObject {
    // A simple value field (e.g., a name)
    private RString name;
    // A reference to another synchronizable object (e.g., settings)
    // The key is automatically derived, imagine it as this.getKey()+":settings" but in binary and efficiently encoded
    private MySettings settings;

    // MyData(Bytes key) constructor must be present in every AbstractData derivate
    public MyData(Bytes key){
        super(key);
    }
    
    // Constructor is called only when creating a new instance manually, otherwise the MyData(Bytes key) constructor is called
    public MyData(Bytes key, String name) {
        this(key);
        // Fields are assigned using helper methods or explicit constructor calls.
        this.name.set(name);
        // The settings field is automatically populated in the reference tree.
        // Do NOT call the constructor for settings explicitly here.
        // It is loaded automatically when MyData is retrieved.
    }
    
    // Getters and setters...
    public String getName() {
        return name.get();
    }

    public void setName(String name){
        this.name.set(name);
    }

    // The reference update will be detected by aspectj and thus written to redis and synchronized accross instances
    public void setName(RString name){
        this.name = name;
    }
    
    public MySettings getSettings() {
        return settings;
    }
}
```

Similarly, the MySettings class might be defined as:

```java
// A settings object extending AbstractObject
public class MySettings extends AbstractObject {
    private RString config;
    
    public MySettings(Bytes key) {
        super(key);
    }

    public MySettings(Bytes key, String config) {
        this(key);
        this.config.set(config);
    }
    
    public String getConfig() {
        return config.get();
    }

    public void setConfig(String config) {
        this.config.set(config);
    }
}
```

If we create the object `new MyData(new Bytes("mydata"))` explicitly in the code, the corresponding `MySettings`-Object will be created. Do only call the constructor for objects that are not automatically loaded when other objects are loaded. Usually, having one List/Map as the root of your "reference-tree" is sufficient.

## Automatic Population and Key Derivation

When an object such as MyData is part of a larger synchronized object graph (the reference tree), its fields are populated automatically. The keys for each field are derived from the parent object's key by appending a field identifier (often via a lookup table). This mechanism ensures that:
- Fields that exist in Redis (or have been pre-loaded as part of the reference tree) are automatically linked without the need for explicit constructor calls.
- Calling the constructor manually for referenced objects can lead to duplicate instances; therefore, only the root object (or objects not retrievable via the reference tree) should be explicitly loaded.
  
For example, if an instance of MyData (with key "test") contains a settings reference, the system automatically uses the parent key ("test") together with the field name (e.g., "settings" or a mapped key from a lookup table) to derive the key for MySettings. As long as the reference is available, the settings field will be loaded automatically, preventing duplicate instantiation.

## Missing Explanations

There are various nuances and details not covered in this README, that would probably require a full wiki to explain properly. But alas, as this is just a prototype, that level of documentation is not merited.

## Experimental Notice

Firecord is a research prototype exploring virtual distributed object memory concepts in Java. While functional, it's designed primarily for experimentation and learning rather than production use. The current implementation demonstrates:

- How virtual distributed object memory can be implemented using Redis as a virtual object store
- AspectJ-based change detection for fine-grained synchronization
- Cache coherence through message-passing protocols
- Transparent distributed object access

**Current Limitations:**
- Missing garbage collection for distributed objects
- No support for sorted sets or parent references  
- Limited testing in high-concurrency scenarios
- May require architectural changes for production scalability

Future versions may involve significant rewrites as the design evolves.
A potential improvement would be to disregard with the get() and set() methods for simple types completely and let aspectj handle detecting those fields getting changed as well.
Another idea would be to disregard the explicit reference handling (the `Bytes key`) and instead use runtime persistent annotations as well as a complete virtual automatically managed address space including a garbage collector that would only rely on "beacons" (objects with manually specified keys that are never* deleted) to keep the distributed object graph alive and let us find the relevant objects through their (indirect) relations to the beacons.

## Using firecord

Firecord relies on AspectJ for bytecode weaving, so it requires proper setup in your build system (Maven, Gradle, etc.) to enable compile-time or load-time weaving. The maven configuration is also present in firecord's pom.xml file, but an example pom will be provided here later for reference.
Gradle can do bytecode weaving as well in theory, but I have not tested it yet and cannot provide an example configuration at this time.

## Contributing

Contributions are welcome! Feel free to submit issues or pull requests to help improve Firecord, keeping in mind its experimental status.

## License

Firecord is licensed under the MIT License. See the `LICENSE` file for details.
