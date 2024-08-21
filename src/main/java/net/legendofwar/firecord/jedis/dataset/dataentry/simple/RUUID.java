package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import java.util.UUID;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataGenerator;

public final class RUUID extends SmallData<UUID> {

    final static DataGenerator<RUUID> GENERATOR = new DataGenerator<>(new Bytes("ruuid"), RUUID.class);

    final static RUUID create() {
        return GENERATOR.create();
    }

    final static RUUID create(Vector defaultValue) {
        return GENERATOR.create(defaultValue);
    }

    final static void delete(RUUID object) {
        DataGenerator.delete(object);
    }

    final static UUID DEFAULT_VALUE = new UUID(0, 0);

    public RUUID(@NotNull Bytes key) {
        this(key, null);
    }

    public RUUID(@NotNull Bytes key, UUID defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected Bytes toBytes() {
        return new Bytes(this.value.toString());
    }

    @Override
    protected void fromBytes(byte[] value) {
        this.value = UUID.fromString(new String(value));
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    public double getSortScore(){
        return ((double) (this.value.getMostSignificantBits())) * Long.MAX_VALUE + this.value.getLeastSignificantBits();
    }

}
