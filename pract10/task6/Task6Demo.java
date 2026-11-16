package task6;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// несколько стадий делят один deadline; перед каждой стадией считаем остаток
public class Task6Demo {
  private static final long BUDGET_MILLIS = 600;

  public static void main(String[] args) throws Exception {
    ExecutorService pool = Executors.newSingleThreadExecutor(
        task -> new Thread(task, "stage-worker"));
    long start = System.nanoTime();
    long deadline = start + TimeUnit.MILLISECONDS.toNanos(BUDGET_MILLIS);
    System.out.println("budget=" + BUDGET_MILLIS + "ms");
    try {
      runStage("load course", 150, deadline, pool);
      runStage("load modules", 300, deadline, pool);
      runStage("build report", 400, deadline, pool);
      runStage("send notification", 50, deadline, pool);
    } finally {
      pool.shutdown();
      if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
        pool.shutdownNow();
        pool.awaitTermination(2, TimeUnit.SECONDS);
      }
    }
    System.out.println("total elapsed="
        + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + "ms");
  }

  private static void runStage(
      String name, long workMillis, long deadline, ExecutorService pool)
      throws InterruptedException {
    long remaining = deadline - System.nanoTime();
    if (remaining <= 0) {
      System.out.println(name + ": skipped, no time left");
      return;
    }

    Future<String> future = pool.submit(() -> {
      try {
        Thread.sleep(workMillis);
        return "done";
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return "interrupted";
      }
    });

    try {
      System.out.println(name + ": " + future.get(remaining, TimeUnit.NANOSECONDS));
    } catch (TimeoutException e) {
      future.cancel(true);
      System.out.println(name + ": timeout, cancel(true) sent");
    } catch (ExecutionException e) {
      System.out.println(name + ": failed: " + e.getCause().getMessage());
    }
  }
}
