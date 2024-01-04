package net.legendofwar.firecord.jedis;

import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class PartialResource {

    public PartialResource() {

    }

    public void registerLoad(Class<?> clazz, AbstractData<?> ad) {
    }

    public void registerUnavailableLoad(String className, Bytes bytes) {
    }

}
