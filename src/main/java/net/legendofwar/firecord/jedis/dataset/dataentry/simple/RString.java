package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataGenerator;
import net.legendofwar.firecord.tool.SortHelper;

public final class RString extends SmallData<String> {

    final static DataGenerator<RString> GENERATOR = new DataGenerator<>(new Bytes("rstring"), RString.class);

    public final static RString create() {
        return GENERATOR.create();
    }

    public final static RString create(String defaultValue) {
        return GENERATOR.create(defaultValue);
    }

    public final static void delete(RString object) {
        DataGenerator.delete(object);
    }

    final static String DEFAULT_VALUE = "";

    public RString(@NotNull Bytes key) {
        this(key, null);
    }

    public RString(@NotNull Bytes key, String defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected Bytes toBytes() {
        return new Bytes(this.value);
    }

    @Override
    protected void fromBytes(byte[] value) {
        this.value = new String(value);
    }

    @Override
    public String toString() {
        return this.value;
    }

    public double getSortScore() {
        return SortHelper.getSortScore(this.value);
    }

}
