package net.legendofwar.firecord.jedis.dataset.dataentry.composite;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;

public class RMap<T extends AbstractData<?>, E extends AbstractData<?>> extends CompositeData<T> implements Map<T, E> {

    RMap(@NotNull Bytes key) {
        super(key, DataType.MAP);
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'clear'");
    }

    @Override
    public boolean containsKey(Object arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'containsKey'");
    }

    @Override
    public boolean containsValue(Object arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'containsValue'");
    }

    @Override
    public Set<Entry<T, E>> entrySet() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'entrySet'");
    }

    @Override
    public E get(Object arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'get'");
    }

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isEmpty'");
    }

    @Override
    public Set<T> keySet() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'keySet'");
    }

    @Override
    public E put(T arg0, E arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'put'");
    }

    @Override
    public void putAll(Map<? extends T, ? extends E> arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'putAll'");
    }

    @Override
    public E remove(Object arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'remove'");
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'size'");
    }

    @Override
    public Collection<E> values() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'values'");
    }

    @Override
    void _load() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method '_load'");
    }

    @Override
    void _store() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method '_store'");
    }

}
