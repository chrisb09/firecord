package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RChar extends SmallData<Character> {

    public RChar(String key, @NotNull Character defaultValue) {
        super(key, defaultValue, DataType.CHAR);
    }

    public RChar(String key) {
        this(key, ' ');
    }

    @Override
    protected void fromString(String value) {
        this.value = value.charAt(0);
    }

    @Override
    public String toString() {
        return ""+this.value;
    }

}
