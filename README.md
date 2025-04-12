# Firecord

Firecord is a distributed object synchronization library designed to seamlessly persist and synchronize permanent objects across multiple processes and machines. By leveraging Redis for storage and caching, Firecord allows objects to be shared and updated almost as simply as using local object-oriented programming.

## Purpose

The primary goal of Firecord is to synchronize objects—their contents and references—across nodes with a focus on the independence of individual fields or objects. Instead of serializing entire objects every time, Firecord updates only the changed fields if the values are changed, or updates the reference the field points to if a reference change was detected using bytecode-weaving (via AspectJ). This approach minimizes network traffic by supporting lazy updates and ensuring that larger objects are updated efficiently, with only slight latency (typically 1-2ms for lazy updates) when necessary.

## Features

- **Object Synchronization**: Automatically populate and load objects stored in the database and synchronize changes across nodes.
- **Delta and Lazy Updates**: Update only the modified fields or referenced objects rather than serializing complete objects.
- **Efficient Propagation**: Minimize excessive network traffic by caching data locally and propagating incremental changes.
- **Reference Change Detection**: Detect updates in object references using bytecode weaving, allowing fine-grained update propagation.
- **Platform Focus**: Primarily tailored for SpigotMC, Paper, and Velocity servers, while also supporting standalone mode for non-Minecraft applications.
- **Experimental Nature**: This software is an experimental prototype and may require a complete rewrite in the future.

## How It Works

Firecord uses Redis both as a high-speed cache and as a persistent data store for permanent objects. By leveraging Redis Pub/Sub channels and Redis data structures, Firecord can synchronize object state between nodes. The innovative use of AspectJ-based bytecode weaving allows Firecord to detect exactly which fields or object references are updated and propagate only those changes, making it a lean alternative to libraries like Redisson.

### Key Components

- **JedisCommunication**: Manages communication over Redis Pub/Sub channels (used primarily for change propagation).
- **JedisCommunicationChannel**: Defines the channels used for broadcasting update events.
- **AbstractData**: Represents the permanent, synchronized objects and their structures (lists, sets, maps, etc.).
- **MessageReceiver**: Handles incoming synchronization messages on subscribed channels.

## Usage

### Initialization

To initialize Firecord, call the `init` method with the node's unique ID and type. For example, in a standalone mode:

```java
Firecord.init(new Bytes("Node1"), NodeType.STANDALONE);
```

For Minecraft server modes (SpigotMC/Paper/Velocity), use the relevant integration.

### Synchronizing Objects

Create and interact with permanent objects almost like local objects. For example, to manipulate a synchronized map:

```java
RMap<RInteger> testMap = new RMap<>(new Bytes("testMap"));
testMap.put(new Bytes("key1"), new RInteger(new Bytes("value1")));
```

Only the modified field or object is updated on the network, reducing latency and network overhead.

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

Firecord is purely experimental. Its current implementation serves as a prototype, focusing on ease-of-use and efficient object synchronization. Future versions may require a full rewrite as the design and performance are refined.

It is also still missing key features, and has not be properly tested in all scenarios.

What's missing:
- Sorted Sets
- Parent-references
- Garbage Collector
- Deletion of Objects

## Use Cases

- **Minecraft Server Networks**: Seamlessly synchronize persistent objects such as player data, game states, or configurations across multiple server instances.
- **Distributed Systems**: Enable real-time propagation of object state in microservices or other distributed architectures.
- **Rapid Prototyping**: Simplify the process of making objects persistent and synchronized across various platforms with minimal coding overhead.

## Contributing

Contributions are welcome! Feel free to submit issues or pull requests to help improve Firecord, keeping in mind its experimental status.

## License

Firecord is licensed under the MIT License. See the `LICENSE` file for details.
