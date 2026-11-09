package eu.isygoit.solver.index;

import java.util.Arrays;

/**
 * Minimal growable {@code int} list used while building the index (no boxing). Package-private, single-threaded.
 */
final class IntList {

    private int[] data;
    private int size;

    IntList() {
        this(16);
    }

    IntList(int capacity) {
        data = new int[Math.max(4, capacity)];
    }

    void add(int value) {
        if (size == data.length) {
            data = Arrays.copyOf(data, (int) Math.min(Integer.MAX_VALUE - 8L, data.length * 2L));
        }
        data[size++] = value;
    }

    int get(int i) {
        return data[i];
    }

    void set(int i, int value) {
        data[i] = value;
    }

    int size() {
        return size;
    }

    boolean isEmpty() {
        return size == 0;
    }

    void clear() {
        size = 0;
    }

    int[] toArray() {
        return Arrays.copyOf(data, size);
    }
}
