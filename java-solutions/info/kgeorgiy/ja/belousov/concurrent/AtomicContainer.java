package info.kgeorgiy.ja.belousov.concurrent;

/**
 * Thread safe class that can store some values
 *
 * @param <T> type of the stored object
 */
public class AtomicContainer<T> {
    private T value;

    /**
     * Constructor with a default stored value
     *
     * @param value value to store
     */
    public AtomicContainer(T value) {
        this.value = value;
    }

    /**
     * Thread sage getter for the stored value
     *
     * @return value
     */
    public synchronized T getValue() {
        return value;
    }

    /**
     * Thread safe setter for the stored value
     *
     * @param value new value
     */
    public synchronized void setValue(T value) {
        this.value = value;
    }
}
