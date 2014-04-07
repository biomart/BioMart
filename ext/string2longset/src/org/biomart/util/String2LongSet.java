package org.biomart.util;

import ie.ucd.murmur.MurmurHash2;
import it.unimi.dsi.fastutil.longs.LongOpenHashBigSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author jhsu
 */
public final class String2LongSet implements Set<String> {
    LongSet set = new LongOpenHashBigSet();

    @Override
    public void clear() {
        set.clear();
    }

    @Override
    public boolean removeAll(Collection c) {
        LongSet s = new LongOpenHashSet();
        for (Object o : c) {
            s.add(object2long(o));
        }
        return set.removeAll(s);
    }

    @Override
    public boolean retainAll(Collection c) {
        LongSet s = new LongOpenHashSet();
        for (Object o : c) {
            s.add(object2long(o));
        }
        return set.retainAll(s);
    }

    @Override
    public boolean addAll(Collection c) {
        LongSet s = new LongOpenHashSet();
        for (Object o : c) {
            s.add(object2long(o));
        }
        return set.addAll(s);
    }

    @Override
    public boolean containsAll(Collection c) {
        LongSet s = new LongOpenHashSet();
        for (Object o : c) {
            s.add(object2long(o));
        }
        return s.containsAll(set);
    }

    @Override
    public boolean remove(Object o) {
        return set.remove(object2long(o));
    }

    @Override
    public boolean add(String s) {
        return add(s.getBytes());
    }

    public boolean add(byte[] bytes) {
        long l = MurmurHash2.hash64(bytes, bytes.length);
        return set.add(l);
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(object2long(o));
    }

    public boolean contains(byte[] bytes) {
        long l = MurmurHash2.hash64(bytes, bytes.length);
        return set.contains(l);
    }

    @Override
    public <T> T[] toArray(T[] objects) {
        return set.toArray(objects);
    }


    @Override
    public Object[] toArray() {
        return set.toArray();
    }

    @Override
    public Iterator iterator() {
        return set.iterator();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public int size() {
        return set.size();
    }

    private long object2long(Object o) {
        byte[] bytes = ((String)o).getBytes();
        return MurmurHash2.hash64(bytes, bytes.length);
    }
}
