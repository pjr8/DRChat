package me.paulrobinson;

import java.util.function.IntFunction;

public final class CircularBuffer<E> {
    private final E[] buffer;
    private int head = 0;
    private int size = 0;


    @SuppressWarnings("unchecked")
    public CircularBuffer(int capacity) {
        this.buffer = (E[]) new Object[capacity];
    }

    public void add(E element) {
        buffer[head] = element;
        head = (head + 1) % buffer.length;
        if (size < buffer.length) size++;
    }

    public E[] toArray(IntFunction<E[]> factory) {
        E[] result = factory.apply(size);
        int start = size < buffer.length ? 0 : head;
        for (int i = 0; i < size; i++) {
            result[i] = buffer[(start + i) % buffer.length];
        }
        return result;
    }
}
