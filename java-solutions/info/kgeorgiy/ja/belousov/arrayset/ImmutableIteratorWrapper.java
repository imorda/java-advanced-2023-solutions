package info.kgeorgiy.ja.belousov.arrayset;

import java.util.Iterator;

public class ImmutableIteratorWrapper<E> implements Iterator<E> {
    final private Iterator<E> original;

    public ImmutableIteratorWrapper(Iterator<E> original) {
        this.original = original;
    }

    @Override
    public boolean hasNext() {
        return original.hasNext();
    }

    @Override
    public E next() {
        return original.next();
    }
}
