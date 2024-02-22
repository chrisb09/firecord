package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataGenerator;

public final class RByteArray extends LargeData<byte[]> {

    final static DataGenerator<RByteArray> GENERATOR = new DataGenerator<>(new Bytes("rbytearray"), RByteArray.class);

    final static RByteArray create() {
        return GENERATOR.create();
    }

    final static RByteArray create(Vector defaultValue) {
        return GENERATOR.create(defaultValue);
    }

    final static void delete(RByteArray object) {
        DataGenerator.delete(object);
    }

    final static byte[] DEFAULT_VALUE = new byte[0];

    public RByteArray(@NotNull Bytes key) {
        this(key, null);
    }

    public RByteArray(@NotNull Bytes key, byte[] defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected Bytes toBytes() {
        return new Bytes(this.value);
    }

    @Override
    protected void fromBytes(@NotNull byte[] value) {
        this.value = value;
    }

    @Override
    public String toString() {
        get();
        return new Bytes(this.value).toString();
    }

}
