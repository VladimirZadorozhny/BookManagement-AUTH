package org.mystudying.bookmanagementauth.support.concurrency;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Helper component for orchestrating concurrent execution in tests with CountDownLatch.
 */
@Component
public class ConcurrentTestHelperLatch {

    /**
     * Executes a list of tasks in parallel using a fixed thread pool.
     */
    public <T> List<T> runParallel(List<Callable<T>> tasks, int threadCount) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r);
            t.setName("test-worker-" + t.getId());
            return t;
        });

        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);

        List<Future<T>> futures = new ArrayList<>();

        try {

            for (Callable<T> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Start signal timeout");
                    }
                    return task.call();
                }));
            }
            // wait until all threads are ready
            if (!ready.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Threads were not ready in time.");
            }
            start.countDown();  // release them simultaneously

            List<T> results = new ArrayList<>();
            for (Future<T> f : futures) {
                results.add(f.get(10, TimeUnit.SECONDS));
            }
            return results;

        } catch (Exception e) {
            throw new RuntimeException("Concurrent execution failed", e);
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }


    /**
     * Executes the same task multiple times in parallel.
     */
    public <T> List<T> runParallel(Callable<T> task, int count) throws InterruptedException {
        List<Callable<T>> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(task);
        }
        return runParallel(tasks, count);
    }
}
