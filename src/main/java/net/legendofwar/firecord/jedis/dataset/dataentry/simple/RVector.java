package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import java.nio.ByteBuffer;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataGenerator;

public final class RVector extends SmallData<Vector> {

    final static DataGenerator<RVector> GENERATOR = new DataGenerator<>(new Bytes("rvector"), RVector.class);

    final static RVector create() {
        return GENERATOR.create();
    }

    final static RVector create(Vector defaultValue) {
        return GENERATOR.create(defaultValue);
    }

    final static void delete(RVector object) {
        DataGenerator.delete(object);
    }

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

    public double getSortScore(){
        return this.value.getX();
    }

}
