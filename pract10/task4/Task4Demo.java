package task4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// CPU-bound и blocking I/O нельзя держать в одном маленьком пуле
public class Task4Demo {
  public static void main(String[] args) throws Exception {
    int cores = Runtime.getRuntime().availableProcessors();
    System.out.println("available processors: " + cores);
    showStarvation();
    showSeparatedExecutors(cores);
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

  private static void showSeparatedExecutors(int cores) throws Exception {
    System.out.println("separate executors:");
    ExecutorService cpu = Executors.newFixedThreadPool(cores, named("cpu"));
    ExecutorService io = Executors.newVirtualThreadPerTaskExecutor();
    CountDownLatch blockingStarted = new CountDownLatch(100);
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch cpuStarted = new CountDownLatch(cores);
    List<Future<Long>> cpuFutures = new ArrayList<>();
    try {
      for (int i = 0; i < 100; i++) {
        io.submit(() -> {
          blockingStarted.countDown();
          await(release);
          return null;
        });
      }
      if (!blockingStarted.await(2, TimeUnit.SECONDS)) {
        throw new IllegalStateException("blocking tasks did not start");
      }

      for (int i = 0; i < cores; i++) {
        cpuFutures.add(cpu.submit(() -> {
          cpuStarted.countDown();
          long sum = 0;
          for (int j = 0; j < 1_000_000; j++) {
            sum += j;
          }
          return sum;
        }));
      }

      boolean started = cpuStarted.await(1, TimeUnit.SECONDS);
      System.out.println("  100 virtual I/O tasks waiting, cpu tasks started: " + started);
      if (!started) {
        throw new AssertionError("cpu tasks starved");
      }

      long total = 0;
      for (Future<Long> future : cpuFutures) {
        total += future.get(2, TimeUnit.SECONDS);
      }
      System.out.println("  cpu total=" + total);
    } finally {
      release.countDown();
      stop(io);
      stop(cpu);
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
