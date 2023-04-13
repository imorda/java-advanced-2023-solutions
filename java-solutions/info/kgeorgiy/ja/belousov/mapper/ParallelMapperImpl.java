package info.kgeorgiy.ja.belousov.mapper;

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

                        task.run();
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

        synchronized (tasks) {
            for (int i = 0; i < args.size(); i++) {
                int finalI = i;
                tasks.add(() -> {
                    result.set(finalI, f.apply(args.get(finalI)));

                    synchronized (progressCounter) {
                        progressCounter.increment();
                        progressCounter.notify();
                    }
                });
            }
            tasks.notifyAll();
        }

        synchronized (progressCounter) {
            while (progressCounter.getValue() < args.size()) {
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
