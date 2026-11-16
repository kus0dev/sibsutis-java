package task4;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// CPU-bound и blocking I/O нельзя держать в одном маленьком пуле
public class Task4Demo {
  public static void main(String[] args) throws Exception {
    System.out.println("available processors: " + Runtime.getRuntime().availableProcessors());
    showStarvation();
  }

  private static void showStarvation() throws Exception {
    System.out.println("shared fixed pool of 2:");
    ExecutorService shared = Executors.newFixedThreadPool(2, named("shared"));
    CountDownLatch blockingStarted = new CountDownLatch(2);
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch cpuStarted = new CountDownLatch(2);
    try {
      for (int i = 0; i < 2; i++) {
        shared.execute(() -> {
          blockingStarted.countDown();
          await(release);
          System.out.println("  [" + Thread.currentThread().getName() + "] blocking task finished");
        });
      }
      if (!blockingStarted.await(2, TimeUnit.SECONDS)) {
        throw new IllegalStateException("blocking tasks did not start");
      }

      for (int i = 0; i < 2; i++) {
        shared.execute(() -> {
          cpuStarted.countDown();
          long sum = 0;
          for (int j = 0; j < 1_000_000; j++) {
            sum += j;
          }
          System.out.println("  [" + Thread.currentThread().getName() + "] cpu task sum=" + sum);
        });
      }

      boolean started = cpuStarted.await(200, TimeUnit.MILLISECONDS);
      System.out.println("  cpu tasks started while I/O holds both threads: " + started);
      if (started) {
        throw new AssertionError("expected starvation");
      }

      release.countDown();
      if (!cpuStarted.await(2, TimeUnit.SECONDS)) {
        throw new IllegalStateException("cpu tasks did not start after release");
      }
      System.out.println("  cpu tasks started after release: true");
    } finally {
      release.countDown();
      stop(shared);
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static ThreadFactory named(String prefix) {
    AtomicInteger number = new AtomicInteger();
    return task -> new Thread(task, prefix + "-" + number.incrementAndGet());
  }

  private static void stop(ExecutorService executor) throws InterruptedException {
    executor.shutdown();
    if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
      executor.shutdownNow();
      executor.awaitTermination(3, TimeUnit.SECONDS);
    }
  }
}
