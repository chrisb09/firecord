package net.legendofwar.firecord.jedis.dataset.dataentry.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public final class SlaveCollection<E extends AbstractData<?>> implements Collection<E> {

    RMap<E> master;
    ArrayList<E> data = new ArrayList<>();

    SlaveCollection(RMap<E> master) {
        this.master = master;
        if (this.master.data != null) {
            synchronized (this.master.data) {
                master.data.forEach((name, obj) -> this.data.add(obj));
            }
        }
    }

    @Override
    public boolean add(E arg0) {
        throw new UnsupportedOperationException(
                "As this Collection is only acting as a view on the underlaying RMaps data," +
                        "add-operations cannot be supported as the key is missing");
    }

    @Override
    public boolean addAll(Collection<? extends E> arg0) {
        throw new UnsupportedOperationException(
                "As this Collection is only acting as a view on the underlaying RMaps data," +
                        "add-operations cannot be supported as the key is missing");
    }

    @Override
    public void clear() {
        this.master.clear();
    }

    @Override
    public boolean contains(Object arg0) {
        synchronized (this.data) {
            return this.data.contains(arg0);
        }
    }

    @Override
    public boolean containsAll(Collection<?> arg0) {
        synchronized (this.data) {
            return this.data.containsAll(arg0);
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (this.data) {
            return this.data.isEmpty();
        }
    }

    @Override
    public Iterator<E> iterator() {
        synchronized (this.data) {
            return this.data.iterator();
        }
    }

    @Override
    public boolean remove(Object arg0) {
        HashSet<Bytes> keys = new HashSet<>();
        synchronized (this.master.data) {
            for (Map.Entry<Bytes, E> en : master.data.entrySet()) {
                if (en.getValue().equals(arg0)) {
                    keys.add(en.getKey());
                }
            }
        }
        for (Bytes k : keys) {
            master.remove(k);
        }
        return keys.size() != 0;
    }

    @Override
    public boolean removeAll(Collection<?> arg0) {
        HashSet<Bytes> keys = new HashSet<>();
        synchronized (this.master.data) {
            for (Map.Entry<Bytes, E> en : master.data.entrySet()) {
                if (arg0.contains(en.getValue())) {
                    keys.add(en.getKey());
                }
            }
        }
        for (Bytes k : keys) {
            master.remove(k);
        }
        return keys.size() != 0;
    }

    @Override
    public boolean retainAll(Collection<?> arg0) {
        HashSet<Bytes> keys = new HashSet<>();
        synchronized (this.master.data) {
            for (Map.Entry<Bytes, E> en : master.data.entrySet()) {
                if (!arg0.contains(en.getValue())) {
                    keys.add(en.getKey());
                }
            }
        }
        for (Bytes k : keys) {
            master.remove(k);
        }
        return keys.size() != 0;
    }

    @Override
    public int size() {
        synchronized (this.data) {
            return this.data.size();
        }
    }

    @Override
    public Object[] toArray() {
        synchronized (this.data) {
            return this.data.toArray();
        }
    }

    @Override
    public <T> T[] toArray(T[] arg0) {
        synchronized (this.data) {
            return this.data.toArray(arg0);
        }
    }

}
