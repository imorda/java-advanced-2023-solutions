package info.kgeorgiy.ja.belousov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
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

    private static <T> void incrementIter(List<? extends T> data, SolveResult<?, ? extends T> lastResult) {
        int iter = lastResult.getIteration();
        iter += 1;
        if (iter >= data.size()) {
            lastResult.setDone(true);
        }
        lastResult.setIteration(iter);
    }

    private <T, R, I> R computeThreaded(int threads, BiFunction<List<T>, Integer, List<List<T>>> splitter,
                                        I defaultResult, BiConsumer<List<T>, SolveResult<I, R>> solver,
                                        Function<List<R>, R> resultCombiner, List<T> data) throws InterruptedException {
        List<List<T>> threadsData = splitter.apply(data, threads);

        List<Thread> threadInstances = new ArrayList<>();
        List<SolveResult<I, R>> threadSolveResults = new ArrayList<>();

        for (List<T> threadData : threadsData) {
            SolveResult<I, R> currentThreadResult = new SolveResult<>(defaultResult);
            threadSolveResults.add(currentThreadResult);

            threadInstances.add(new Thread(() -> {

                while (!currentThreadResult.isDone()) {
                    solver.accept(threadData, currentThreadResult);

                    if (Thread.interrupted()) {
                        return;
                    }
                }
            }));
        }

        for (Thread thread : threadInstances) {
            thread.start();
        }
        for (Thread thread : threadInstances) {
            thread.join();
        }

        List<R> preparedResults = new ArrayList<>();
        for (SolveResult<I, R> result : threadSolveResults) {
            if (result.getResult().isPresent()) {
                preparedResults.add(result.getResult().get());
            }
        }

        return resultCombiner.apply(preparedResults);
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return computeThreaded(threads, listSplitter(), Optional.<T>empty(), (data, lastResult) -> {
            int iter = lastResult.getIteration();
            if (iter < data.size()) {
                if (lastResult.getIntermediateData().isEmpty()) {
                    lastResult.setIntermediateData(Optional.of(data.get(iter)));
                } else if (comparator.compare(data.get(iter), lastResult.getIntermediateData().orElseThrow()) > 0) {
                    lastResult.setIntermediateData(Optional.of(data.get(iter)));
                }
            }

            incrementIter(data, lastResult);
            if (lastResult.isDone() && lastResult.getIntermediateData().isPresent()) {
                lastResult.setResult(lastResult.getIntermediateData().orElseThrow());
            }
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
        return computeThreaded(threads, listSplitter(), null, (data, lastResult) -> {
            if (sharedState.isFinished()) {
                lastResult.setResult(false);
                lastResult.setDone(true);
                return;
            }

            int iter = lastResult.getIteration();
            if (iter < data.size() && predicate.test(data.get(iter))) {
                sharedState.setFinished();
                lastResult.setResult(true);
                lastResult.setDone(true);
                return;
            }

            incrementIter(data, lastResult);
            if (lastResult.isDone()) {
                lastResult.setResult(false);
            }
        }, threadedResult -> threadedResult.stream().anyMatch(val -> val), values);
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return computeThreaded(threads, listSplitter(), 0, (data, lastResult) -> {
            int iter = lastResult.getIteration();
            if (iter < data.size() && predicate.test(data.get(iter))) {
                lastResult.setIntermediateData(lastResult.getIntermediateData() + 1);
            }

            incrementIter(data, lastResult);
            if (lastResult.isDone()) {
                lastResult.setResult(lastResult.getIntermediateData());
            }
        }, threadedResult -> threadedResult.stream().mapToInt(Integer::intValue).sum(), values);
    }
}