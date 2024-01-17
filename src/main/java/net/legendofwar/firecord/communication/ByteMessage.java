package net.legendofwar.firecord.communication;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.javatuples.Quintet;
import org.javatuples.Sextet;
import org.javatuples.Triplet;
import org.javatuples.Unit;

import net.legendofwar.firecord.jedis.dataset.ByteDataInterface;
import net.legendofwar.firecord.jedis.dataset.Bytes;

public enum ByteMessage {

    BYTE(Byte.class),
    SHORT(Short.class),
    INTEGER(Integer.class),
    LONG(Long.class),
    DOUBLE(Double.class),
    STRING(String.class),
    BYTES(Bytes.class),
    ARRAY(null);

    final Class<?> c;

    private ByteMessage(Class<?> c) {
        this.c = c;
    }

    public Object read(ByteBuffer buffer, Class<?> c) {
        switch (this) {
            case BYTE:
                return buffer.get();
            case INTEGER:
                return buffer.getInt();
            case LONG:
                return buffer.getLong();
            case BYTES:
                //System.out.println("pos: "+buffer.position());
                int size = buffer.getInt();
                //System.out.println("pos: "+buffer.position());
                //System.out.println("Read in Bytes of size:"+size);
                byte[] bytes = new byte[size];
                for (int i = 0; i < size; i++) {
                    bytes[i] = buffer.get();
                }
                return new Bytes(bytes);
            case ARRAY:
                int arraySize = buffer.getInt();
                //System.out.println("Read in Array of size:"+arraySize);
                //Object[] array = new Object[arraySize];
                Object array = Array.newInstance(c, arraySize);
                for (int i = 0; i < arraySize; i++) {
                    Array.set(array, i, byClass(c).read(buffer, (c != null && c.isArray()) ? c.getComponentType() : null));
                }
                return array;
            case DOUBLE:
                return buffer.getDouble();
            case SHORT:
                return buffer.getShort();
            case STRING:
                return ((Bytes) (ByteMessage.BYTES.read(buffer, null))).asString();
            default:
                break;
        }
        return null;
    }

    private Bytes write(Object object) {
        switch (this) {
            case BYTE:
                return new Bytes((Byte) object);
            case INTEGER:
                return new Bytes((Integer) object);
            case LONG:
                return new Bytes((Long) object);
            case BYTES:
                Bytes bytes = new Bytes();
                if (byte[].class.isAssignableFrom(object.getClass())) {
                    bytes = new Bytes((byte[]) object);
                } else if (Bytes.class.isAssignableFrom(object.getClass())) {
                    bytes = (Bytes) object;
                } else if (ByteDataInterface.class.isAssignableFrom(object.getClass())) {
                    bytes = new Bytes((ByteDataInterface) object);
                }
                bytes = new Bytes(bytes.length).append(bytes);
                return bytes;
            case ARRAY:
                int arraySize = Array.getLength(object);
                Bytes data = new Bytes(arraySize);
                for (int i = 0; i < arraySize; i++) {
                    Object ob = Array.get(object, i);
                    data = data.append(byClass(ob.getClass()).write(ob));
                }
                return data;
            case DOUBLE:
                return new Bytes((Double) object);
            case SHORT:
                return new Bytes((Short) object, Short.BYTES);
            case STRING:
                return ByteMessage.BYTES.write(new Bytes((String) object));
            default:
                break;
        }
        return null;
    }

    protected static ByteMessage byClass(Class<?> c) {
        if (c.isArray()) {
            return ByteMessage.ARRAY;
        }
        for (ByteMessage bm : ByteMessage.values()) {
            if (bm.c.equals(c)) {
                return bm;
            }
        }
        System.out.println("The class '" + c + "' is not known.");
        System.out.println("Known classes: "
                + String.join(",", Arrays.stream(ByteMessage.values()).map(bm -> bm.c.toString()).toList()));
        return null;
    }

    private static Object[] _readIn(byte[] message, ByteField... fields) {
        ByteBuffer bytebuffer = ByteBuffer.wrap(message);
        bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
        Object[] objects = new Object[fields.length];
        for (int i = 0; i < fields.length; i++) {
            //System.out.println("#"+i+". "+fields[i].getByteMessage().name()+" pos: "+bytebuffer.position());
            objects[i] = fields[i].read(bytebuffer);
        }
        return objects;
    }

    private static Object[] _readIn(byte[] message, Class<?>... classes) {
        ByteField[] fields = new ByteField[classes.length];
        for (int i = 0; i < classes.length; i++) {
            fields[i] = byClass(classes[i]).new ByteField(classes[i]);
        }
        return _readIn(message, fields);
    }

    @SuppressWarnings("unchecked")
    public static <A> Unit<A> readIn(Bytes message, Class<A> a) {
        Object[] results = _readIn(message.getData(), a);
        return new Unit<A>((A) results[0]);
    }

    @SuppressWarnings("unchecked")
    public static <A, B> Pair<A, B> readIn(Bytes message, Class<A> a, Class<B> b) {
        Object[] results = _readIn(message.getData(), a, b);
        return new Pair<A, B>((A) results[0], (B) results[1]);
    }

    @SuppressWarnings("unchecked")
    public static <A, B, C> Triplet<A, B, C> readIn(Bytes message, Class<A> a, Class<B> b, Class<C> c) {
        Object[] results = _readIn(message.getData(), a, b, c);
        return new Triplet<A, B, C>((A) results[0], (B) results[1], (C) results[2]);
    }

    @SuppressWarnings("unchecked")
    public static <A, B, C, D> Quartet<A, B, C, D> readIn(Bytes message, Class<A> a, Class<B> b, Class<C> c,
            Class<D> d) {
        Object[] results = _readIn(message.getData(), a, b, c, d);
        return new Quartet<A, B, C, D>((A) results[0], (B) results[1], (C) results[2], (D) results[3]);
    }

    @SuppressWarnings("unchecked")
    public static <A, B, C, D, E> Quintet<A, B, C, D, E> readIn(Bytes message, Class<A> a, Class<B> b, Class<C> c,
            Class<D> d, Class<E> e) {
        Object[] results = _readIn(message.getData(), a, b, c, d, e);
        return new Quintet<A, B, C, D, E>((A) results[0], (B) results[1], (C) results[2], (D) results[3], (E) results[4]);
    }
    @SuppressWarnings("unchecked")
    public static <A, B, C, D, E, F> Sextet<A, B, C, D, E, F> readIn(Bytes message, Class<A> a, Class<B> b, Class<C> c,
            Class<D> d, Class<E> e, Class<F> f) {
        Object[] results = _readIn(message.getData(), a, b, c, d, e, f);
        return new Sextet<A, B, C, D, E, F>((A) results[0], (B) results[1], (C) results[2], (D) results[3], (E) results[4], (F) results[4]);
    }

    public static Bytes write(Object... objects) {
        Bytes[] data = new Bytes[objects.length];
        for (int i = 0; i < objects.length; i++) {
            data[i] = byClass(objects[i].getClass()).write(objects[i]);
        }
        return new Bytes().append(data);
    }

    class ByteField {

        final private Class<?> c;

        private ByteField(Class<?> c) {
            if (ByteMessage.this == ByteMessage.ARRAY) {
                this.c = c.getComponentType();
            } else {
                this.c = c;
            }
        }

        public Object read(ByteBuffer bytebuffer) {
            return ByteMessage.this.read(bytebuffer, c);
        }

        ByteMessage getByteMessage(){
            return ByteMessage.this;
        }

    }

}
