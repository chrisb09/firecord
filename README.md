# Firecord

Firecord is a distributed shared memory (DSM) system that provides transparent object synchronization across multiple processes and machines. It implements a virtual shared address space using Redis as the backing store, combined with message-passing for cache coherence, allowing distributed applications to interact with shared objects as if they were local references.

## Purpose

Firecord simulates shared memory semantics in a distributed environment by using Redis as a coherent shared storage layer with Pub/Sub-based invalidation protocols. The system provides fine-grained synchronization at the field level rather than object level—when a field changes, only that specific change is propagated. Reference updates are detected through AspectJ bytecode weaving, enabling the system to distinguish between value changes and pointer reassignments. This approach minimizes network traffic and provides low-latency updates (typically 1-2ms) while maintaining strong consistency across nodes.

## Features

- **Distributed Shared Memory**: Provides transparent shared memory semantics over a distributed system using Redis as the virtual address space
- **Cache Coherence Protocol**: Uses Redis Pub/Sub channels for invalidation-based cache coherence across nodes
- **Fine-Grained Synchronization**: Updates individual fields rather than entire objects, detected through AspectJ bytecode weaving
- **Reference vs. Value Change Detection**: Distinguishes between field value modifications and reference reassignments
- **Selective Synchronization**: Classes can opt-out of synchronization using `@ClassAnnotation(synchronize = false)`
- **Multi-Platform Support**: Works across different machines or multiple processes on the same machine
- **Platform Integration**: Native support for Minecraft servers (Spigot/Paper/Velocity) plus standalone mode
- **Lazy Loading**: Objects and their reference graphs are loaded on-demand from Redis
- **Experimental Prototype**: Research-focused implementation exploring DSM concepts in Java

## How It Works

Firecord implements a **Distributed Shared Memory (DSM)** architecture that simulates shared memory across multiple nodes:

### Architecture Overview

1. **Virtual Shared Address Space**: Redis serves as the coherent shared storage that all nodes can access, acting as the "memory" in a distributed system
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

### Synchronizing Objects

Objects extending `AbstractData` are automatically synchronized across nodes. The system handles both value updates and reference changes:

```java
// Create/access a synchronized map
RMap<RInteger> testMap = new RMap<>(new Bytes("testMap"));
testMap.put(new Bytes("key1"), new RInteger(new Bytes("value1")));

// Value changes are automatically broadcast
RInteger counter = new RInteger(new Bytes("counter"));
counter.set(42); // Broadcasts UPDATE_SMALL_KEY message

// Reference changes are detected by AspectJ
MyObject obj = new MyObject(new Bytes("obj"));
obj.field = new RString(new Bytes("newField")); // Broadcasts REFERENCE_UPDATE message
```

### Selective Synchronization

Classes can opt-out of distributed synchronization while still maintaining local functionality:

```java
@ClassAnnotation(synchronize = false)
public class LocalOnlyObject extends AbstractObject {
    public RInteger value; // Won't be synchronized across nodes
}
```

### Example Command

Firecord includes commands for testing synchronization. For example, use the command:

```java
/firecord test
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

If we create the object `new MyData(new Bytes("mydata"))` explicitly in the code, the corresponding `MySettings`-Object will be loaded or created if it doesn't exist yet. Do only call the constructor for objects that are not automatically loaded when other objects are loaded. Usually, having one List/Map as the root of your "reference-tree" is sufficient.

## Automatic Population and Key Derivation

When an object such as MyData is part of a larger synchronized object graph (the reference tree), its fields are populated automatically. The keys for each field are derived from the parent object's key by appending a field identifier (often via a lookup table). This mechanism ensures that:
- Fields that exist in Redis (or have been pre-loaded as part of the reference tree) are automatically linked without the need for explicit constructor calls.
- Calling the constructor manually for referenced objects can lead to duplicate instances; therefore, only the root object (or objects not retrievable via the reference tree) should be explicitly loaded.
  
For example, if an instance of MyData (with key "test") contains a settings reference, the system automatically uses the parent key ("test") together with the field name (e.g., "settings" or a mapped key from a lookup table) to derive the key for MySettings. As long as the reference is available, the settings field will be loaded automatically, preventing duplicate instantiation.

## Experimental Notice

Firecord is a research prototype exploring distributed shared memory concepts in Java. While functional, it's designed primarily for experimentation and learning rather than production use. The current implementation demonstrates:

- How DSM can be implemented using Redis as a virtual address space
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


## Use Cases

- **Distributed Applications**: Enable shared state across microservices or distributed system components
- **Multi-Process Applications**: Share objects between processes on the same machine with automatic synchronization  
- **Minecraft Server Networks**: Synchronize player data, game states, and configurations across server instances
- **Research & Education**: Study distributed shared memory concepts and cache coherence protocols
- **Rapid Prototyping**: Quickly add persistence and cross-process synchronization to Java applications

## Contributing

Contributions are welcome! Feel free to submit issues or pull requests to help improve Firecord, keeping in mind its experimental status.

## License

Firecord is licensed under the MIT License. See the `LICENSE` file for details.
