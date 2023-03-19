package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RUUID extends SmallData<UUID> {

    public RUUID(String key, @NotNull UUID defaultValue) {
        super(key, defaultValue, DataType.UUID);
    }

    public RUUID(String key) {
        this(key, UUID.randomUUID());
    }

    @Override
    protected void fromString(String value) {
        this.value = UUID.fromString(value);
    }

    @Override
    public String toString() {
        return ""+this.value;
    }

}
