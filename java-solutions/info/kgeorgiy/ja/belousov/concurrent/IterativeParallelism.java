package info.kgeorgiy.ja.belousov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * A class that implements iterative parallelism computing for
 * scalar methods described in {@link ScalarIP} interface
 *
 * @author Timofey Belousov
 */
public class IterativeParallelism implements ScalarIP {
    private static <T> BiFunction<List<T>, Integer, List<List<T>>> listSplitter() {
        return (original, count) -> {
            int lastBorder = 0;
            final int step = Math.max(1, ((original.size() - 1 + count) / count));
            ArrayList<List<T>> result = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int newBorder = Math.min(original.size(), lastBorder + step);
                result.add(original.subList(lastBorder, newBorder));
                lastBorder = newBorder;
            }
            return result;
        };
    }

    private <T, R> R computeThreaded(int threads, BiFunction<List<T>, Integer, List<List<T>>> splitter,
                                     Function<List<T>, R> solver,
                                     Function<List<R>, R> resultCombiner, List<T> data) throws InterruptedException {
        List<List<T>> threadsData = splitter.apply(data, threads);

        List<Thread> threadInstances = new ArrayList<>();
        ArrayList<Optional<R>> threadSolveResults = new ArrayList<>(Collections.nCopies(threads, Optional.empty()));

        for (int i = 0; i < threads; i++) {
            final int finalI = i;
            threadInstances.add(new Thread(() -> threadSolveResults.set(finalI, Optional.ofNullable(solver.apply(threadsData.get(finalI))))));
        }

        try {
            threadInstances.forEach(Thread::start);
            for (Thread thread : threadInstances) {
                thread.join();
            }
        } catch (InterruptedException e) {
            for (Thread thread : threadInstances) {
                if (thread.isAlive()) {
                    thread.interrupt();
                    try {
                        thread.join();
                    } catch (InterruptedException ignored) {
                    }
                }
            }

            throw e;
        }

        List<R> preparedResults = threadSolveResults.stream().filter(Optional::isPresent).map(Optional::get).toList();

        return resultCombiner.apply(preparedResults);
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return computeThreaded(threads, listSplitter(), (data) -> {
            T curMax = null;
            for (T datum : data) {
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (curMax == null) {
                    curMax = datum;
                } else if (comparator.compare(datum, curMax) > 0) {
                    curMax = datum;
                }
            }
            return curMax;
        }, (threadedResult) -> threadedResult.stream().max(comparator).orElseThrow(), values);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, predicate.negate());
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        SharedWorkerState sharedState = new SharedWorkerState();
        return computeThreaded(threads, listSplitter(), (data) -> {
            for (T datum : data) {
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (sharedState.isFinished()) {
                    break;
                }

                if (predicate.test(datum)) {
                    sharedState.setFinished();
                    return true;
                }
            }
            return false;
        }, threadedResult -> threadedResult.stream().anyMatch(val -> val), values);
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return computeThreaded(threads, listSplitter(), (data) -> {
            int count = 0;
            for (T datum : data) {
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (predicate.test(datum)) {
                    count++;
                }
            }
            return count;
        }, threadedResult -> threadedResult.stream().mapToInt(Integer::intValue).sum(), values);
    }
}