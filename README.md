# Firecord

Firecord is a distributed object synchronization library designed to seamlessly persist and synchronize permanent objects across multiple processes and machines. By leveraging Redis for storage and caching, Firecord allows objects to be shared and updated almost as simply as using local object-oriented programming.

## Purpose

The primary goal of Firecord is to synchronize objects—their contents and references—across nodes with a focus on the atomicity of individual fields or objects. Instead of serializing entire objects every time, Firecord updates only the changed fields using techniques such as bytecode weaving (via AspectJ). This approach minimizes network traffic by supporting lazy updates and ensuring that larger objects are updated efficiently, with only slight latency (typically 1-2ms for lazy updates) when necessary.

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

to broadcast a test message and observe object change propagation.

## Experimental Notice

Firecord is purely experimental. Its current implementation serves as a prototype, focusing on ease-of-use and efficient object synchronization. Future versions may require a full rewrite as the design and performance are refined.

## Use Cases

- **Minecraft Server Networks**: Seamlessly synchronize persistent objects such as player data, game states, or configurations across multiple server instances.
- **Distributed Systems**: Enable real-time propagation of object state in microservices or other distributed architectures.
- **Rapid Prototyping**: Simplify the process of making objects persistent and synchronized across various platforms with minimal coding overhead.

## Contributing

Contributions are welcome! Feel free to submit issues or pull requests to help improve Firecord, keeping in mind its experimental status.

## License

Firecord is licensed under the MIT License. See the `LICENSE` file for details.
