package info.kgeorgiy.ja.shvetsov.crawler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class Host {
    private final BlockingQueue<Runnable> queuedTasks;
    private final Semaphore semaphore;
    private final ExecutorService downloaders;

    public Host(ExecutorService downloaders, int perHost) {
        queuedTasks = new LinkedBlockingQueue<>();
        this.semaphore = new Semaphore(perHost);
        this.downloaders = downloaders;
    }

    public void addTask(Runnable task) {
        if (semaphore.tryAcquire()) {
            downloaders.execute(task);
            return;
        }

        queuedTasks.add(task);
    }

    public void execute() {
        Runnable task = queuedTasks.poll();

        if (task != null) {
            try {
                semaphore.acquire();
                downloaders.execute(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void releaseSemaphore() {
        semaphore.release();
    }
}
