package info.kgeorgiy.ja.belousov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;


/**
 * A class that implements a parallel mapper for
 * according to {@link ParallelMapper} interface
 *
 * @author Timofey Belousov
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threads;
    private final Queue<Runnable> tasks;

    /**
     * A constructor that creates an instance of {@link ParallelMapperImpl} that
     * uses {@code threads_count} threads for parallel computing.
     * Note: created threads ignore all runtime exceptions thrown by an executing task,
     * the task must handle them itself.
     *
     * @param threads_count number of treads to create
     */
    public ParallelMapperImpl(int threads_count) {
        threads = new ArrayList<>(threads_count);
        tasks = new ArrayDeque<>();

        for (int i = 0; i < threads_count; i++) {
            threads.add(new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        Runnable task;
                        synchronized (tasks) {
                            while (tasks.isEmpty()) {
                                tasks.wait();
                            }
                            task = tasks.poll();
                        }

                        try {
                            task.run();
                        } catch (RuntimeException ignored) {
                        }
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    // Восстановление флага прерывания
                    Thread.currentThread().interrupt();
                }
            }));
        }

        threads.forEach(Thread::start);
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        List<R> result = new ArrayList<>(Collections.nCopies(args.size(), null));

        ThreadSafeCounter progressCounter = new ThreadSafeCounter();
        AtomicContainer<RuntimeException> exception = new AtomicContainer<>(null);

        synchronized (tasks) {
            for (int i = 0; i < args.size(); i++) {
                int finalI = i;
                tasks.add(() -> {
                    try {
                        result.set(finalI, f.apply(args.get(finalI)));

                        synchronized (progressCounter) {
                            progressCounter.increment();
                            progressCounter.notify();
                        }
                    } catch (RuntimeException e) {
                        exception.setValue(e);
                        progressCounter.notify();
                    }
                });
            }
            tasks.notifyAll();
        }

        synchronized (progressCounter) {
            while (progressCounter.getValue() < args.size()) {
                RuntimeException e = exception.getValue();
                if (e != null) {
                    throw e;
                }
                progressCounter.wait();
            }
            return result;
        }
    }

    @Override
    public void close() {
        for (Thread thread : threads) {
            if (thread.isAlive()) {
                thread.interrupt();
                try {
                    thread.join();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
