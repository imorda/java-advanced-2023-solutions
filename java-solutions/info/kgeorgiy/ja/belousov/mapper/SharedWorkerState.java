package info.kgeorgiy.ja.belousov.mapper;

/**
 * A helper class to store the shared state of the {@link IterativeParallelism}
 * computing thread.
 * It can be only created with the {@code false} state of the {@code isFinished} flag.
 * This flag can be easily read with the getter method, but it can only be modified once
 * (from {@code false} to {@code true} state with the setter method.
 * Since it can only be modified once, to a single state, it is thread-safe while not requiring any
 * synchronization techniques.
 * This flag is used to determine if one of the threads had successfully reached the global target
 * and all other threads are now safe to finish without continuing any computations.
 */
public class SharedWorkerState {
    private volatile boolean isFinished;

    /**
     * A constructor that initializes this class with a {@code false} state of the {@code isFinished} flag
     */
    public SharedWorkerState() {
        isFinished = false;
    }

    /**
     * A basic getter method that can be used by many thread simultaneously
     *
     * @return value of {@code isFinished} flag
     */
    public boolean isFinished() {
        return isFinished;
    }

    /**
     * A setter method that can only be used to set {@code isFinished} to the {@code true} state.
     * This action is irreversible.
     */
    public void setFinished() {
        isFinished = true;
    }
}
