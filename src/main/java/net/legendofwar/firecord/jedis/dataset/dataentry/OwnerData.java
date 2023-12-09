package net.legendofwar.firecord.jedis.dataset.dataentry;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;

public abstract class OwnerData<T> extends AbstractData<T> {

    protected OwnerData(@NotNull Bytes key) {
        super(key);
    }

    /**
     * Informs the parent aka. owner of an AbstractData-Object that it is removed,
     * either automatically or manually
     * 
     * @param ad
     */
    public abstract void deleteChild(AbstractData<?> ad);

}
