package info.kgeorgiy.ja.shvetsov.iterative;

import info.kgeorgiy.java.advanced.iterative.NewListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link NewListIP} interface.
 */
public class IterativeParallelism implements NewListIP {

    private final ParallelMapper parallelMapper;

    public IterativeParallelism() {
        this.parallelMapper = null;
    }

    /**
     * Default constructor of {@link IterativeParallelism} class.
     */
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    private <T, R> R task(int threads,
                          List<? extends T> values,
                          Function<Stream<? extends T>, R> task,
                          Function<Stream<? extends R>, R> resCollector,
                          int step)
            throws InterruptedException {

        if (threads < 1) {
            throw new IllegalArgumentException(
                    "ERROR: Not enough threads to start the task:\n " +
                            "Should be at least 1, given " + threads
            );
        } else if (values == null) {
            throw new IllegalArgumentException(
                    "ERROR: 'values' list should not be null"
            );
        } else if (values.isEmpty()) {
            throw new IllegalArgumentException(
                    "ERROR: 'values' list should not be empty"
            );
        } else if (step < 1) {
            throw new IllegalArgumentException(
                    "ERROR: 'step' should be greater than zero"
            );
        }

        threads = Math.min(threads, values.size());
        final int baseThreadSize = values.size() / threads;
        int remainingTasks = values.size() % threads;

        List<Stream<? extends T>> streamsList = new ArrayList<>();
        List<Thread> threadsList = new ArrayList<>();
        List<R> threadsRes = new ArrayList<>(Collections.nCopies(threads, null));

        for (int threadsIndex = 0, valuesIndex = 0; threadsIndex < threads; ++threadsIndex) {
            final int finalCurrentLeftBound = valuesIndex;
            valuesIndex += baseThreadSize + (remainingTasks-- > 0 ? 1 : 0);
            final int finalCurrentRightBound = valuesIndex;
            final int finalCurrentThreadsIndex = threadsIndex;

            List<T> subList = new ArrayList<>();
            int start = finalCurrentLeftBound + ((step - (finalCurrentLeftBound % step)) % step);
            for (int j = start; j < finalCurrentRightBound; j += step) {
                subList.add(values.get(j));
            }

            if (parallelMapper == null) {
                Thread currentThread = new Thread(
                        () -> threadsRes.set(
                                finalCurrentThreadsIndex,
                                task.apply(subList.stream())
                        )
                );

                currentThread.start();
                threadsList.add(currentThread);
            } else {
                streamsList.add(subList.stream());
            }
        }

        if (parallelMapper == null) {
            for (Thread thread : threadsList) {
                try {
                    thread.join();
                } catch (InterruptedException ignored) {
                }
            }

            return resCollector.apply(threadsRes.stream());
        } else {
            return resCollector.apply(parallelMapper.map(task, streamsList).stream());
        }
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator, int step) throws InterruptedException {
        return task(
                threads,
                values,
                stream -> stream.max(comparator).orElse(null),
                stream -> stream.filter(Objects::nonNull).max(comparator).orElse(null),
                step
        );
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator, int step) throws InterruptedException {
        return maximum(
                threads,
                values,
                comparator.reversed(),
                step
        );
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return task(
                threads,
                values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue),
                step
        );
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return !all(
                threads,
                values,
                predicate.negate(),
                step
        );
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return filter(
                threads,
                values,
                predicate,
                step
        ).size();
    }

    @Override
    public String join(int threads, List<?> values, int step) throws InterruptedException {
        return task(
                threads,
                values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()),
                step
        );
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return task(
                threads,
                values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()),
                step
        );
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f, int step) throws InterruptedException {
        return task(
                threads,
                values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()),
                step
        );
    }
}