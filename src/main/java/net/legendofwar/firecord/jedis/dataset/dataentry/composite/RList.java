package net.legendofwar.firecord.jedis.dataset.dataentry.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.communication.ByteMessage;
import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.SimpleData;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.args.ListPosition;

@SuppressWarnings("unchecked")
public class RList<T extends AbstractData<?>> extends CompositeData<T, List<T>> implements List<T> {

    static HashMap<Bytes, RList<AbstractData<?>>> loaded = new HashMap<Bytes, RList<AbstractData<?>>>();

    static {

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_ADD, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                // [4 Byte: key-length: n][n Byte: key][length-4-n Byte: added key]
                Pair<Bytes, Bytes> m = ByteMessage.readIn(message, Bytes.class, Bytes.class);
                Bytes key = m.getValue0();
                Bytes addedKey = m.getValue1();
                /*
                 * ByteBuffer bytebuffer = ByteBuffer.wrap(message.getData());
                 * bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
                 * int keyLength = bytebuffer.getInt();
                 * byte[] key = new byte[keyLength];
                 * for (int i = 0; i < keyLength; i++) {
                 * key[i] = bytebuffer.get();
                 * }
                 * byte[] addedKey = bytebuffer.array();
                 */
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    AbstractData<?> entry = AbstractData.create(addedKey);
                    if (entry != null) {
                        synchronized (l.data) {
                            l.data.add(entry);
                        }
                    }
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_ADD_ALL, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                // [4 Byte: key length: n][n Byte: key][4 Byte: amount of addedKeys]
                // [4 Byte: addedKey_i length: m_i][m_i Byte: addedKey_i]...
                Pair<Bytes, Bytes[]> m = ByteMessage.readIn(message, Bytes.class, Bytes[].class);
                Bytes key = m.getValue0();
                Bytes[] addedKeys = m.getValue1();
                /*
                 * ByteBuffer bytebuffer = ByteBuffer.wrap(message);
                 * int keyLength = bytebuffer.getInt();
                 * byte[] key = new byte[keyLength];
                 * for (int i = 0; i < keyLength; i++) {
                 * key[i] = bytebuffer.get();
                 * }
                 * int addedKeysAmount = bytebuffer.getInt();
                 * byte[][] addedKeys = new byte[addedKeysAmount][];
                 * for (int i = 0; i < addedKeysAmount; i++) {
                 * int addedKeyLength = bytebuffer.getInt();
                 * addedKeys[i] = new byte[addedKeyLength];
                 * for (int j = 0; j < addedKeyLength; j++) {
                 * addedKeys[i][j] = bytebuffer.get();
                 * }
                 * }
                 */
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    for (Bytes added_key : addedKeys) {
                        AbstractData<?> entry = AbstractData.create(added_key);
                        if (entry != null) {
                            synchronized (l.data) {
                                l.data.add(entry);
                            }
                        }
                    }
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_ADD_INDEX, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                // [4 Byte: key-length: n][n Byte: key][4 Byte: index][length-4-4-n Byte: added
                // key]
                Triplet<Bytes, Integer, Bytes> m = ByteMessage.readIn(message, Bytes.class, Integer.class, Bytes.class);
                Bytes key = m.getValue0();
                int index = m.getValue1();
                Bytes addedKey = m.getValue2();
                /*
                 * ByteBuffer bytebuffer = ByteBuffer.wrap(message);
                 * bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
                 * int keyLength = bytebuffer.getInt();
                 * byte[] key = new byte[keyLength];
                 * for (int i = 0; i < keyLength; i++) {
                 * key[i] = bytebuffer.get();
                 * }
                 * int index = bytebuffer.getInt();
                 * byte[] addedKey = bytebuffer.array();
                 */
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    AbstractData<?> entry = AbstractData.create(addedKey);
                    if (entry != null) {
                        synchronized (l.data) {
                            l.data.add(index, entry);
                        }
                    }
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_ADD_ALL_INDEX, new MessageReceiver() { // RList_add_all_index

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                // [4 Byte: key length: n][n Byte: key][4 Byte: index]
                // [4 Byte: amount of addedKeys][4 Byte: addedKey_i length: m_i][m_i Byte:
                // addedKey_i]...
                Triplet<Bytes, Integer, Bytes[]> m = ByteMessage.readIn(message, Bytes.class, Integer.class,
                        Bytes[].class);
                Bytes key = m.getValue0();
                int index = m.getValue1();
                Bytes[] addedKeys = m.getValue2();
                /*
                 * ByteBuffer bytebuffer = ByteBuffer.wrap(message);
                 * int keyLength = bytebuffer.getInt();
                 * byte[] key = new byte[keyLength];
                 * for (int i = 0; i < keyLength; i++) {
                 * key[i] = bytebuffer.get();
                 * }
                 * int index = bytebuffer.getInt();
                 * int addedKeysAmount = bytebuffer.getInt();
                 * byte[][] addedKeys = new byte[addedKeysAmount][];
                 * for (int i = 0; i < addedKeysAmount; i++) {
                 * int addedKeyLength = bytebuffer.getInt();
                 * addedKeys[i] = new byte[addedKeyLength];
                 * for (int j = 0; j < addedKeyLength; j++) {
                 * addedKeys[i][j] = bytebuffer.get();
                 * }
                 * }
                 */
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    int i = 0;
                    for (Bytes addedKey : addedKeys) {
                        AbstractData<?> entry = AbstractData.create(addedKey);
                        if (entry != null) {
                            synchronized (l.data) {
                                l.data.add(index + (i++), entry);
                            }
                        }
                    }
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_REMOVE, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                // [4 Byte: key-length: n][n Byte: key][length-4-n Byte: removed key]
                Pair<Bytes, Bytes> m = ByteMessage.readIn(message, Bytes.class, Bytes.class);
                Bytes key = m.getValue0();
                Bytes removedKey = m.getValue1();
                /*
                 * ByteBuffer bytebuffer = ByteBuffer.wrap(message);
                 * bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
                 * int keyLength = bytebuffer.getInt();
                 * byte[] key = new byte[keyLength];
                 * for (int i = 0; i < keyLength; i++) {
                 * key[i] = bytebuffer.get();
                 * }
                 * byte[] removedKey = bytebuffer.array();
                 */
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    l._removeKey(removedKey);
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_REMOVE_INDEX, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                // [4 Byte: key-length: n][n Byte: key][4 Byte: index]
                Pair<Bytes, Integer> m = ByteMessage.readIn(message, Bytes.class, Integer.class);
                Bytes key = m.getValue0();
                int index = m.getValue1();
                /*
                 * ByteBuffer bytebuffer = ByteBuffer.wrap(message);
                 * bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
                 * int keyLength = bytebuffer.getInt();
                 * byte[] key = new byte[keyLength];
                 * for (int i = 0; i < keyLength; i++) {
                 * key[i] = bytebuffer.get();
                 * }
                 * int index = bytebuffer.getInt();
                 */
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    synchronized (l.data) {
                        l.data.remove(index);
                    }
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_REMOVE_ALL, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                // [4 Byte: key length: n][n Byte: key][4 Byte: amount of removedKeys
                // [4 Byte: removedKey_i length: m_i][m_i Byte: removedKey_i]...
                Pair<Bytes, Bytes[]> m = ByteMessage.readIn(message, Bytes.class, Bytes[].class);
                Bytes key = m.getValue0();
                Bytes[] removedKeys = m.getValue1();
                /*
                 * ByteBuffer bytebuffer = ByteBuffer.wrap(message);
                 * int keyLength = bytebuffer.getInt();
                 * byte[] key = new byte[keyLength];
                 * for (int i = 0; i < keyLength; i++) {
                 * key[i] = bytebuffer.get();
                 * }
                 * int removedKeysAmount = bytebuffer.getInt();
                 * byte[][] removedKeys = new byte[removedKeysAmount][];
                 * for (int i = 0; i < removedKeysAmount; i++) {
                 * int removedKeyLength = bytebuffer.getInt();
                 * removedKeys[i] = new byte[removedKeyLength];
                 * for (int j = 0; j < removedKeyLength; j++) {
                 * removedKeys[i][j] = bytebuffer.get();
                 * }
                 * }
                 */
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    for (Bytes removedKey : removedKeys) {
                        l._removeKey(removedKey);
                    }
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_RETAIN_ALL, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                // [4 Byte: key length: n][n Byte: key][4 Byte: amount of retainedKeys
                // [4 Byte: retainedKey_i length: m_i][m_i Byte: retainedKey_i]...
                Pair<Bytes, Bytes[]> m = ByteMessage.readIn(message, Bytes.class, Bytes[].class);
                Bytes key = m.getValue0();
                Bytes[] retainedKeys = m.getValue1();
                /*
                 * ByteBuffer bytebuffer = ByteBuffer.wrap(message);
                 * int keyLength = bytebuffer.getInt();
                 * byte[] key = new byte[keyLength];
                 * for (int i = 0; i < keyLength; i++) {
                 * key[i] = bytebuffer.get();
                 * }
                 * int retainedKeysAmount = bytebuffer.getInt();
                 * byte[][] retainedKeys = new byte[retainedKeysAmount][];
                 * for (int i = 0; i < retainedKeysAmount; i++) {
                 * int removedKeyLength = bytebuffer.getInt();
                 * retainedKeys[i] = new byte[removedKeyLength];
                 * for (int j = 0; j < removedKeyLength; j++) {
                 * retainedKeys[i][j] = bytebuffer.get();
                 * }
                 * }
                 */
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    List<AbstractData<?>> toRetain = new ArrayList<AbstractData<?>>(retainedKeys.length);
                    for (Bytes retained_key : retainedKeys) {
                        toRetain.add(AbstractData.create(retained_key));
                    }
                    synchronized (l.data) {
                        l.data.retainAll(toRetain);
                    }
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_SET, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                // [4 Byte: key-length: n][n Byte: key][4 Byte: index][length-4-4-n Byte:
                // replacing
                // key]
                Triplet<Bytes, Integer, Bytes> m = ByteMessage.readIn(message, Bytes.class, Integer.class, Bytes.class);
                Bytes key = m.getValue0();
                int index = m.getValue1();
                Bytes newKey = m.getValue2();
                /*
                 * ByteBuffer bytebuffer = ByteBuffer.wrap(message);
                 * bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
                 * int keyLength = bytebuffer.getInt();
                 * byte[] key = new byte[keyLength];
                 * for (int i = 0; i < keyLength; i++) {
                 * key[i] = bytebuffer.get();
                 * }
                 * int index = bytebuffer.getInt();
                 * byte[] newKey = bytebuffer.array();
                 */
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    AbstractData<?> entry = AbstractData.create(newKey);
                    if (entry != null) {
                        synchronized (l.data) {
                            l.data.set(index, entry);
                        }
                    }
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_LOG, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Bytes key = message;
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    System.out.println(key + ":");
                    System.out.println("  cache: " + String.join(",",
                            l.stream().map(bytearray -> bytearray.getKey().toString()).toList()));
                    try (Jedis j = ClassicJedisPool.getJedis()) {
                        System.out.println("  redis: " + String.join(",", j.lrange(key.getData(), 0, -1).stream()
                                .map(bytearray -> new Bytes(bytearray).toString()).toList()));
                    }
                }
            }

        });

    }

    private RList(@NotNull Bytes key, ArrayList<T> data) {
        super(key, data, DataType.LIST);
        synchronized (loaded) {
            loaded.put(key, (RList<AbstractData<?>>) this);
        }
    }

    public RList(@NotNull Bytes key, int initialCapacity) {
        this(key, new ArrayList<T>(initialCapacity));
    }

    public RList(@NotNull Bytes key) {
        this(key, new ArrayList<T>());
    }

    @Override
    void _load() {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            List<byte[]> keys = j.lrange(this.key.getData(), 0, -1);
            synchronized (this.data) {
                this.data.clear();
                for (byte[] k : keys) {
                    AbstractData<?> ad = AbstractData.create(new Bytes(k));
                    if (ad != null) {
                        // since load is called AFTER we put this object into the AbstractData loaded
                        // map cyclical dependencies are handled correctly
                        this.data.add((T) ad);
                    }
                }
            }
        }
    }

    @Override
    void _store() {
        try (AbstractData<T> ad = this.lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.del(this.key.getData());
                j.rpush(this.key.getData(),
                        this.data.stream().map(element -> element.getKey().getData()).toArray(byte[][]::new));
            }
        }
    }

    public boolean add(T arg0) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        synchronized (this.data) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.rpush(this.key.getData(), arg0.getKey().getData());
            }
            JedisCommunication.broadcast(JedisCommunicationChannel.LIST_ADD,
                    ByteMessage.write(this.key, arg0.getKey()));
            /*
             * ByteBuffer bytebuffer = ByteBuffer.allocate(4 + this.key.length +
             * arg0.getKey().length);
             * bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
             * bytebuffer.putInt(this.key.length);
             * bytebuffer.put(this.key);
             * bytebuffer.put(arg0.getKey());
             * bytebuffer.position(0);
             * JedisCommunication.broadcast(JedisCommunicationChannel.LIST_ADD,
             * bytebuffer.array());
             */
            return this.data.add(arg0);
        }
    }

    public void add(int arg0, T arg1) {
        if (arg1 == null) {
            throw new NullPointerException();
        }
        if (arg0 < 0 || arg0 > this.data.size()) {
            throw new IndexOutOfBoundsException(arg0);
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            if (arg0 == this.data.size()) {
                j.rpush(this.key.getData(), arg1.getKey().getData());
            } else {
                j.linsert(key.getData(), ListPosition.BEFORE, this.data.get(arg0).getKey().getData(),
                        arg1.getKey().getData());
            }
        }
        JedisCommunication.broadcast(JedisCommunicationChannel.LIST_ADD_INDEX,
                ByteMessage.write(this.key, arg0, arg1.getKey()));
        /*
         * ByteBuffer bytebuffer = ByteBuffer.allocate(4 + this.key.length + 4 +
         * arg1.getKey().length);
         * bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
         * bytebuffer.putInt(this.key.length);
         * bytebuffer.put(this.key);
         * bytebuffer.putInt(arg0);
         * bytebuffer.put(arg1.getKey());
         * bytebuffer.position(0);
         * JedisCommunication.broadcast(JedisCommunicationChannel.LIST_ADD_INDEX,
         * bytebuffer.array());
         */
        synchronized (this.data) {
            this.data.add(arg0, arg1);
        }
    }

    public boolean addAll(Collection<? extends T> arg0) {
        if (arg0 == null || arg0.isEmpty()) { // no change
            return false;
        }
        synchronized (this.data) {
            byte[][] keys = new byte[arg0.size()][];
            int index = 0;
            // int totalKeyLength = 0;
            for (T entry : arg0) {
                keys[index++] = entry.getKey().getData();
                // totalKeyLength += entry.getKey().length;
            }
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.rpush(key.getData(), keys);
            }
            JedisCommunication.broadcast(JedisCommunicationChannel.LIST_ADD_ALL,
                    ByteMessage.write(this.key, arg0.stream().map(entry -> entry.getKey()).toArray(Bytes[]::new)));
            /*
             * ByteBuffer bytebuffer = ByteBuffer.allocate(4 + this.key.length + 4 + 4 *
             * keys.length + totalKeyLength);
             * bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
             * bytebuffer.putInt(this.key.length);
             * bytebuffer.put(this.key);
             * bytebuffer.putInt(keys.length);
             * for (int i = 0; i < keys.length; i++) {
             * bytebuffer.putInt(keys[i].length);
             * bytebuffer.put(keys[i]);
             * }
             * bytebuffer.position(0);
             * JedisCommunication.broadcast(JedisCommunicationChannel.LIST_ADD_ALL,
             * bytebuffer.array());
             */
            synchronized (this.data) {
                return this.data.addAll(arg0);
            }
        }
    }

    /**
     * This function should always be used with a lock!
     */
    public boolean addAll(int arg0, Collection<? extends T> arg1) {

        if (arg0 < 0 || arg0 > this.data.size()) {
            throw new IndexOutOfBoundsException(arg0);
        }
        if (arg1 == null || arg1.isEmpty()) { // no change
            return false;
        }
        synchronized (this.data) {
            byte[][] keys = new byte[arg1.size()][];
            // int totalKeyLength = 0;
            int index = 0;
            try (Jedis j = ClassicJedisPool.getJedis()) {
                // get suffix
                List<byte[]> suffix = j.lrange(key.getData(), arg0, -1);
                // remove suffix from list
                if (arg0 == 0) {
                    j.del(key.getData());
                } else {
                    j.ltrim(key.getData(), 0, arg0 - 1);
                }
                for (T entry : arg1) {
                    keys[index++] = entry.getKey().getData();
                    // totalKeyLength += entry.getKey().length;
                }
                // add new entries
                j.rpush(key.getData(), keys);
                // readd suffix
                j.rpush(key.getData(), suffix.toArray(byte[][]::new));
            }
            JedisCommunication.broadcast(JedisCommunicationChannel.LIST_ADD_ALL_INDEX, ByteMessage.write(this.key, arg0,
                    arg1.stream().map(entry -> entry.getKey()).toArray(Bytes[]::new)));
            /*
             * ByteBuffer bytebuffer = ByteBuffer.allocate(4 + this.key.length + 4 + 4 + 4 *
             * keys.length + totalKeyLength);
             * bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
             * bytebuffer.putInt(this.key.length);
             * bytebuffer.put(this.key);
             * bytebuffer.putInt(arg0);
             * bytebuffer.putInt(keys.length);
             * for (int i = 0; i < keys.length; i++) {
             * bytebuffer.putInt(keys[i].length);
             * bytebuffer.put(keys[i]);
             * }
             * bytebuffer.position(0);
             * JedisCommunication.broadcast(JedisCommunicationChannel.LIST_ADD_ALL_INDEX,
             * bytebuffer.array());
             */
            synchronized (this.data) {
                return this.data.addAll(arg0, arg1);
            }
        }
    }

    public T get(int arg0) {
        synchronized (this.data) {
            return this.data.get(arg0);
        }
    }

    public int indexOf(Object arg0) {
        synchronized (this.data) {
            return this.data.indexOf(arg0);
        }
    }

    public int lastIndexOf(Object arg0) {
        synchronized (this.data) {
            return this.data.lastIndexOf(arg0);
        }
    }

    public ListIterator<T> listIterator() {
        synchronized (this.data) {
            return this.data.listIterator();
        }
    }

    public ListIterator<T> listIterator(int arg0) {
        synchronized (this.data) {
            return this.data.listIterator(arg0);
        }
    }

    public boolean remove(Object arg0) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        if (!(arg0 instanceof AbstractData)) {
            throw new ClassCastException(
                    arg0.getClass().getName() + " is not an instance of " + AbstractData.class.getName());
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.lrem(key.getData(), 1, ((AbstractData<?>) arg0).getKey().getData());
        }
        JedisCommunication.broadcast(JedisCommunicationChannel.LIST_REMOVE,
                ByteMessage.write(this.key, ((T) (arg0)).getKey()));
        /*
         * ByteBuffer bytebuffer = ByteBuffer.allocate(4 + this.key.length + ((T)
         * (arg0)).getKey().length);
         * bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
         * bytebuffer.putInt(this.key.length);
         * bytebuffer.put(this.key);
         * bytebuffer.put(((T) (arg0)).getKey());
         * bytebuffer.position(0);
         * JedisCommunication.broadcast(JedisCommunicationChannel.LIST_REMOVE,
         * bytebuffer.array());
         */
        synchronized (this.data) {
            return this.data.remove(arg0);
        }
    }

    public List<Integer> indicesOf(T arg0) {
        List<Integer> l = new ArrayList<Integer>();
        synchronized (this.data) {
            for (int i = 0; i < this.data.size(); i++) {
                T element = this.data.get(i);
                if (arg0.equals(element)) {
                    l.add(i);
                }
            }
        }
        return l;
    }

    // Sadly redis has no native function for this. It is paramount to use a lock
    // whenever this function is called.
    public T remove(int arg0) {
        if (arg0 < 0 || arg0 > this.data.size()) {
            throw new IndexOutOfBoundsException(arg0);
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            // get suffix
            List<byte[]> suffix = j.lrange(key.getData(), arg0 + 1, -1);
            // remove suffix from list
            if (arg0 == 0) {
                j.del(key.getData());
            } else {
                j.ltrim(key.getData(), 0, arg0 - 1);
            }
            if (suffix.size() > 0) {
                // readd suffix
                j.rpush(key.getData(), suffix.toArray(byte[][]::new));
            }
        }
        JedisCommunication.broadcast(JedisCommunicationChannel.LIST_REMOVE_INDEX,
                ByteMessage.write(this.key, arg0));
        /*
         * ByteBuffer bytebuffer = ByteBuffer.allocate(4 + this.key.length + 4);
         * bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
         * bytebuffer.putInt(this.key.length);
         * bytebuffer.put(this.key);
         * bytebuffer.putInt(arg0);
         * bytebuffer.position(0);
         * JedisCommunication.broadcast(JedisCommunicationChannel.LIST_REMOVE_INDEX,
         * bytebuffer.array());
         */
        synchronized (this.data) {
            return this.data.remove(arg0);
        }
    }

    public boolean removeAll(Collection<?> arg0) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        if (arg0.size() == 0) {
            return false;
        }
        byte[][] keys = new byte[arg0.size()][];
        int index = 0;
        // int totalKeyLength = 0;
        synchronized (arg0) {
            for (Object element : arg0) {
                if (element instanceof AbstractData) {
                    @SuppressWarnings("all")
                    AbstractData<?> entry = (AbstractData<?>) element;
                    keys[index++] = entry.getKey().getData();
                    // totalKeyLength += entry.getKey().length;
                }
                try (Jedis j = ClassicJedisPool.getJedis()) {
                    j.lrem(key.getData(), 0, ((T) (element)).getKey().getData());
                }
            }
        }
        synchronized (arg0) {
            JedisCommunication.broadcast(JedisCommunicationChannel.LIST_REMOVE_ALL,
                    ByteMessage.write(this.key,
                            arg0.stream().map(entry -> entry instanceof AbstractData ? ((T) (entry)).getKey() : null)
                                    .toArray(Bytes[]::new)));
        }
        /*
         * ByteBuffer bytebuffer = ByteBuffer.allocate(4 + this.key.length + 4 + 4 *
         * keys.length + totalKeyLength);
         * bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
         * bytebuffer.putInt(this.key.length);
         * bytebuffer.put(this.key);
         * bytebuffer.putInt(keys.length);
         * for (int i = 0; i < keys.length; i++) {
         * bytebuffer.putInt(keys[i].length);
         * bytebuffer.put(keys[i]);
         * }
         * bytebuffer.position(0);
         * JedisCommunication.broadcast(JedisCommunicationChannel.LIST_REMOVE_ALL,
         * bytebuffer.array());
         */
        synchronized (arg0) {
            synchronized (this.data) {
                return this.data.removeAll(arg0);
            }
        }
    }

    public boolean retainAll(Collection<?> arg0) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        List<T> toRem;
        synchronized (this.data) {
            // int totalKeyLength = 0;
            toRem = new ArrayList<>(this.data.size() - arg0.size());
            for (T element : this.data) {
                if (!arg0.contains(element)) {
                    toRem.add(element);
                    // totalKeyLength += element.getKey().length;
                }
            }
            // [4 Byte: key length: n][n Byte: key][4 Byte: amount of retainedKeys
            // [4 Byte: retainedKey_i length: m_i][m_i Byte: retainedKey_i]...
            JedisCommunication.broadcast(JedisCommunicationChannel.LIST_RETAIN_ALL,
                    ByteMessage.write(this.key,
                            arg0.stream().map(entry -> entry instanceof AbstractData ? ((T) (entry)).getKey() : null)
                                    .toArray(Bytes[]::new)));
            /*
             * ByteBuffer bytebuffer = ByteBuffer.allocate(4 + this.key.length + 4 + 4 *
             * keys.length + totalKeyLength);
             * bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
             * bytebuffer.putInt(this.key.length);
             * bytebuffer.put(this.key);
             * bytebuffer.putInt(keys.length);
             * for (int i = 0; i < keys.length; i++) {
             * bytebuffer.putInt(keys[i].length);
             * bytebuffer.put(keys[i]);
             * }
             * bytebuffer.position(0);
             * JedisCommunication.broadcast(JedisCommunicationChannel.LIST_RETAIN_ALL,
             * bytebuffer.array());
             */
            try (Jedis j = ClassicJedisPool.getJedis()) {
                for (T element : toRem) {
                    j.lrem(key.getData(), 0, element.getKey().getData());
                }
            }
        }
        synchronized (this.data) {
            return this.data.retainAll(arg0);
        }
    }

    public T set(int arg0, T arg1) {
        if (arg1 == null) {
            throw new NullPointerException();
        }
        if (arg0 < 0 || arg0 > this.data.size()) {
            throw new IndexOutOfBoundsException(arg0);
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.lset(key.getData(), arg0, arg1.getKey().getData());
        }
        // [4 Byte: key-length: n][n Byte: key][4 Byte: index][length-4-4-n Byte: added
        // key]
        JedisCommunication.broadcast(JedisCommunicationChannel.LIST_SET,
                ByteMessage.write(this.key, arg0, arg1.getKey()));
        /*
         * ByteBuffer bytebuffer = ByteBuffer.allocate(4 + this.key.length + 4 +
         * arg1.getKey().length);
         * bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
         * bytebuffer.putInt(this.key.length);
         * bytebuffer.put(this.key);
         * bytebuffer.putInt(arg0);
         * bytebuffer.put(arg1.getKey());
         * bytebuffer.position(0);
         * JedisCommunication.broadcast(JedisCommunicationChannel.LIST_SET,
         * bytebuffer.array());
         */
        synchronized (this.data) {
            return this.data.set(arg0, arg1);
        }
    }

    public List<T> subList(int fromIndex, int toIndex) {
        synchronized (this.data) {
            return this.data.subList(fromIndex, toIndex);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        synchronized (this.data) {
            for (T entry : this.data) {
                if (entry instanceof SimpleData) {
                    SimpleData<Object> e = (SimpleData<Object>) entry;
                    if (e.get().equals(value)) {
                        return true;
                    }
                } else if (entry instanceof CompositeData) {
                    CompositeData<AbstractData<?>, ?> e = (CompositeData<AbstractData<?>, ?>) entry;
                    if (e.data.equals(value)) {
                        return true;
                    }
                } else if (entry instanceof AbstractObject) {
                    return entry.equals(value);
                }
            }
        }
        return false;
    }

    @Override
    public boolean containsKey(Bytes key) {
        synchronized (this.data) {
            for (T entry : this.data) {
                if (entry.getKey().equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

}
