package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import java.nio.ByteBuffer;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;

public final class RVector extends SmallData<Vector> {

    final static Vector DEFAULT_VALUE = new Vector();

    public RVector(@NotNull Bytes key) {
        this(key, null);
    }

    public RVector(@NotNull Bytes key, Vector defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected Bytes toBytes() {
        ByteBuffer bytebuffer = ByteBuffer.allocate(3 * Double.BYTES);
        bytebuffer.putDouble(this.value.getX());
        bytebuffer.putDouble(this.value.getY());
        bytebuffer.putDouble(this.value.getZ());
        bytebuffer.position(0);
        return new Bytes(bytebuffer);
    }

    @Override
    protected void fromBytes(byte[] value) {
        ByteBuffer bytebuffer = ByteBuffer.wrap(value);
        this.value = new Vector(bytebuffer.getDouble(), bytebuffer.getDouble(), bytebuffer.getDouble());
    }

    @Override
    public String toString() {
        return "(" + value.getX() + "," + value.getY() + "," + value.getZ() + ")";
    }

}
