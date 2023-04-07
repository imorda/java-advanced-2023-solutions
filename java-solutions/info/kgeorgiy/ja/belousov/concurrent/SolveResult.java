package info.kgeorgiy.ja.belousov.concurrent;

import java.util.Optional;

/**
 * A class for storing the intermediate {@link IterativeParallelism} threads computing results
 *
 * @param <I> type for storing some intermediate data that is used during the thread life
 *            (can be used as an accumulator for counting for example)
 * @param <R> type for storing the final thread result.
 */
public class SolveResult<I, R> {
    private Optional<R> result;
    private int iteration;
    private boolean isDone;

    private I intermediateData;

    /**
     * Constructor that instantiates this class with the default state for {@code intermediateData}
     *
     * @param defaultData a value to initialize {@code intermediateData} with
     */
    public SolveResult(I defaultData) {
        this.result = Optional.empty();
        this.iteration = 0;
        this.isDone = false;
        this.intermediateData = defaultData;
    }

    /**
     * Basic getter method for {@code intermediateData}
     *
     * @return value
     */
    public I getIntermediateData() {
        return intermediateData;
    }

    /**
     * Basic setter method for {@code intermediateData}
     *
     * @param intermediateData value
     */
    public void setIntermediateData(I intermediateData) {
        this.intermediateData = intermediateData;
    }

    /**
     * Basic getter method for {@code result}
     *
     * @return value
     */
    public Optional<R> getResult() {
        return result;
    }

    /**
     * Basic setter method for {@code result}
     *
     * @param result value
     */
    public void setResult(R result) {
        this.result = Optional.of(result);
    }

    /**
     * Basic getter method for {@code iteration}
     *
     * @return value
     */
    public int getIteration() {
        return iteration;
    }

    /**
     * Basic setter method for {@code iteration}
     *
     * @param iteration value
     */
    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    /**
     * Basic getter method for the finishing flag of this worker
     *
     * @return value
     */
    public boolean isDone() {
        return isDone;
    }

    /**
     * Basic setter method for the finishing flag of this worker
     *
     * @param done value
     */
    public void setDone(boolean done) {
        isDone = done;
    }
}
