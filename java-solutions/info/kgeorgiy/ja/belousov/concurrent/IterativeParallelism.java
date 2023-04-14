package info.kgeorgiy.ja.belousov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    private final ParallelMapper mapper;

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    public IterativeParallelism() {
        mapper = null;
    }

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

    private <T, R> R splitAndMap(int threads, BiFunction<List<T>, Integer, List<List<T>>> splitter,
                                 Function<List<T>, Optional<R>> solver,
                                 Function<List<R>, R> resultCombiner, List<T> data) throws InterruptedException {
        List<List<T>> splitData = splitter.apply(data, threads);
        if (mapper == null) {
            try (ParallelMapperImpl localMapper = new ParallelMapperImpl(threads)) {
                return computeThreaded(localMapper, splitData, solver, resultCombiner);
            }
        }
        return computeThreaded(mapper, splitData, solver, resultCombiner);
    }

    private <T, R> R computeThreaded(ParallelMapper mapper, List<List<T>> splitData, Function<List<T>, Optional<R>> solver,
                                     Function<List<R>, R> resultCombiner) throws InterruptedException {
        ArrayList<Optional<R>> threadSolveResults = new ArrayList<>(mapper.map(solver, splitData));

        List<R> preparedResults = threadSolveResults.stream().filter(Optional::isPresent).map(Optional::get).toList();

        return resultCombiner.apply(preparedResults);
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return splitAndMap(threads, listSplitter(), (data) -> {
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
            return Optional.ofNullable(curMax);
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
        return splitAndMap(threads, listSplitter(), (data) -> {
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
                    return Optional.of(true);
                }
            }
            return Optional.of(false);
        }, threadedResult -> threadedResult.stream().anyMatch(val -> val), values);
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return splitAndMap(threads, listSplitter(), (data) -> {
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
            return Optional.of(count);
        }, threadedResult -> threadedResult.stream().mapToInt(Integer::intValue).sum(), values);
    }
}