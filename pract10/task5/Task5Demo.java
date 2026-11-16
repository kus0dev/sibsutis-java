package task5;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

// учебный эксперимент: много блокирующих задач на platform и virtual threads
public class Task5Demo {
  private static final int TASKS = 1000;
  private static final int BLOCK_MILLIS = 20;
  private static final int PLATFORM_THREADS = 32;

  public static void main(String[] args) throws Exception {
    System.out.println("tasks=" + TASKS + ", blocking=" + BLOCK_MILLIS + "ms, cores="
        + Runtime.getRuntime().availableProcessors());
    System.out.println("blocking wait only: sleep is not a model of CPU work");
    run("platform", Executors.newFixedThreadPool(PLATFORM_THREADS));
  }

  private static void run(String name, ExecutorService executor) throws Exception {
    ThreadMXBean threads = ManagementFactory.getThreadMXBean();
    threads.resetPeakThreadCount();
    int baseline = threads.getThreadCount();

    List<Callable<Void>> tasks = new ArrayList<>();
    for (int i = 0; i < TASKS; i++) {
      tasks.add(() -> {
        try {
          Thread.sleep(BLOCK_MILLIS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        return null;
      });
    }

    long start = System.nanoTime();
    List<Future<Void>> futures = executor.invokeAll(tasks);
    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    int peak = threads.getPeakThreadCount();

    int completed = 0;
    for (Future<Void> future : futures) {
      if (future.isDone() && !future.isCancelled()) {
        completed++;
      }
    }
    System.out.println(name + ": elapsed=" + elapsedMillis + "ms"
        + ", peak platform threads=" + peak + " (baseline " + baseline + ")"
        + ", completed=" + completed + "/" + TASKS);

    executor.shutdown();
    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
