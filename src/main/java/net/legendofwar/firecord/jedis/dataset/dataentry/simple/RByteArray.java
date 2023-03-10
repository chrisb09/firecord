package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;

public class RByteArray extends LargeData<byte[]> {

    public RByteArray(String key, @NotNull byte[] defaultValue) {
        super(key, defaultValue, DataType.BYTEARRAY);
    }

    @Override
    protected void fromString(@NotNull String value) {
        this.value = value.getBytes();
    }

    @Override
    public String toString(){
       return new String(this.value);
    }
    
}
