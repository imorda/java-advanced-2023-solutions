package info.kgeorgiy.ja.belousov.mapper;

/**
 * Counter thread-safe implementation.
 */
public class ThreadSafeCounter {
    private int value = 0;

    /**
     * Basic getter for the current counter value
     *
     * @return value
     */
    public synchronized int getValue() {
        return value;
    }

    /**
     * Increments the counter value by 1
     */
    public synchronized void increment() {
        value++;
    }
}
