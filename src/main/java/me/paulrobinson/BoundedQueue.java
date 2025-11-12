package me.paulrobinson;

import java.util.ArrayDeque;
import java.util.function.IntFunction;

public final class BoundedQueue<E> {
    private final int cap;
    private final ArrayDeque<E> arrayDeque = new ArrayDeque<>();
    private final IntFunction<E[]> arrayFactory;

    public BoundedQueue(int cap, IntFunction<E[]> arrayFactory) {
        this.cap = cap;
        this.arrayFactory = arrayFactory;
    }

    public void add (E element) {
        if (arrayDeque.size() == cap) arrayDeque.removeFirst();
        arrayDeque.addLast(element);
    }

    public E first() {
        return arrayDeque.getFirst();
    }
    public E last() {
        return arrayDeque.getLast();
    }

    public int size() {
        return arrayDeque.size();
    }
    public E[] toArray() {
        return arrayDeque.toArray(arrayFactory);
    }

}
