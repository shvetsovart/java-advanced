package info.kgeorgiy.ja.shvetsov.iterative;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Implementation of {@link ParallelMapper} interface.
 */
public class ParallelMapperImpl implements ParallelMapper {

    private final static int STACK_MAX_SIZE = 10000;

    private final List<Thread> threadsList;
    private final Queue<Runnable> tasksQueue;

    /**
     * ParallelMapperImpl constructor of number of threads.
     *
     * @param threads number of threads.
     */
    public ParallelMapperImpl(int threads) {
        threadsList = new ArrayList<>();
        tasksQueue = new ArrayDeque<>();

        IntStream.range(0, threads)
                .mapToObj(i -> new Thread(runTask()))
                .peek(threadsList::add)
                .forEach(Thread::start);
    }

    private static class TaskList<T> {
        private final List<T> values;
        private int counter = 0;

        public TaskList(final int size) {
            this.values = new ArrayList<>(Collections.nCopies(size, null));
        }

        private synchronized void isCompleted() {
            if (++counter >= values.size()) {
                notify();
            }
        }

        public void set(final int pos, final T value) {
            values.set(pos, value);
            isCompleted();
        }

        public synchronized List<T> get() throws InterruptedException {
            while (counter < values.size()) {
                wait();
            }

            return values;
        }
    }

    private void addTask(final Runnable task) throws InterruptedException {
        synchronized (tasksQueue) {
            while (tasksQueue.size() == STACK_MAX_SIZE) {
                task.wait();
            }

            tasksQueue.add(task);
            tasksQueue.notifyAll();
        }
    }

    private Runnable runTask() {
        return () -> {
            try {
                while (!Thread.interrupted()) {
                    final Runnable task;
                    synchronized (tasksQueue) {
                        while (tasksQueue.isEmpty()) {
                            tasksQueue.wait();
                        }
                        task = tasksQueue.poll();
                        tasksQueue.notifyAll();
                    }
                    task.run();
                }
            } catch (final InterruptedException ignored) {
            } catch (RuntimeException e) {
                System.err.println("Runtime exception during the task: " + e);
            } finally {
                Thread.currentThread().interrupt();
            }
        };
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> task, List<? extends T> values) throws InterruptedException {

        final TaskList<R> res = new TaskList<>(values.size());
        for (int i = 0; i < values.size(); ++i) {
            final int finalI = i;
            try {
                addTask(
                        () -> res.set(finalI, task.apply(values.get(finalI)))
                );
            } catch (InterruptedException ignored) {
            }
        }

        return res.get();
    }


    @Override
    public void close() {
        threadsList.forEach(
                thread -> {
                    thread.interrupt();
                    try {
                        thread.join();
                    } catch (InterruptedException ignored) {
                    }
                }
        );
    }
}