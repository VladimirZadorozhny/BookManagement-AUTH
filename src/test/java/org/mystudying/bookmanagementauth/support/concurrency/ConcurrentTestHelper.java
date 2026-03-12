package org.mystudying.bookmanagementauth.support.concurrency;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Helper component for orchestrating concurrent execution in tests.
 */
@Component
public class ConcurrentTestHelper {

    /**
     * Executes a list of tasks in parallel using a fixed thread pool.
     */
    public <T> List<T> runParallel(List<Callable<T>> tasks, int threadCount) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);

        List<Future<T>> futures = new ArrayList<>();

        try {

            for (Callable<T> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                }));
            }

            ready.await();      // wait until all threads are ready
            start.countDown();  // release them simultaneously

            List<T> results = new ArrayList<>();
            for (Future<T> f : futures) {
                results.add(f.get());
            }
            return results;

        } catch (Exception e) {
            throw new RuntimeException("Concurrent execution failed", e);
        } finally {
            executor.shutdown();
        }
    }


    /**
     * Executes the same task multiple times in parallel.
     */
    public <T> List<T> runParallel(Callable<T> task, int count) {
        List<Callable<T>> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(task);
        }
        return runParallel(tasks, count);
    }
}
