package task1;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

// Runnable возвращает void, Callable — значение и checked exception;
// Future задаёт бюджет ожидания и отмену
public class Task1Demo {
  private static final long TIMEOUT_MILLIS = 200;

  public static void main(String[] args) throws Exception {
    AtomicInteger threadNumber = new AtomicInteger();
    ExecutorService pool = Executors.newFixedThreadPool(2, task -> {
      Thread thread = new Thread(task, "worker-" + threadNumber.incrementAndGet());
      thread.setDaemon(false);
      return thread;
    });
    try {
      runnableVsCallable(pool);
      timeoutAndFailure(pool);
      cancelInterruptibly(pool);
    } finally {
      // shutdown запрещает новые задачи, awaitTermination ограничивает ожидание
      pool.shutdown();
      if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
        pool.shutdownNow();
        pool.awaitTermination(2, TimeUnit.SECONDS);
      }
    }
  }

  private static void runnableVsCallable(ExecutorService pool) throws Exception {
    System.out.println("Runnable vs Callable:");
    Future<?> runnable = pool.submit(() -> System.out.println("  Runnable: result is void"));
    Object runnableResult = runnable.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

    Future<String> callable = pool.submit(() -> "Callable: result is a value");
    String callableResult = callable.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

    System.out.println("  runnable.get() = " + runnableResult);
    System.out.println("  callable.get() = " + callableResult);
  }

  private static void timeoutAndFailure(ExecutorService pool) {
    System.out.println("timeout and failure:");
    Future<String> slow = pool.submit(() -> {
      Thread.sleep(2_000);
      return "too late";
    });

    try {
      slow.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      // timeout — не ошибка вычисления: задача всё ещё работает
      System.out.println("  TimeoutException: task still running=" + !slow.isDone());
      slow.cancel(true);
    } catch (Exception e) {
      System.out.println("  unexpected: " + e);
    }

    Future<String> broken = pool.submit(() -> {
      throw new IllegalStateException("catalog is unavailable");
    });
    try {
      broken.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    } catch (ExecutionException e) {
      System.out.println("  ExecutionException: cause=" + e.getCause().getMessage());
    } catch (Exception e) {
      System.out.println("  unexpected: " + e);
    }
  }

  private static void cancelInterruptibly(ExecutorService pool) throws Exception {
    System.out.println("cancellation:");
    InterruptibleTask task = new InterruptibleTask();
    Future<String> future = pool.submit(task);
    if (!task.awaitStarted(2, TimeUnit.SECONDS)) {
      throw new IllegalStateException("task did not start");
    }

    boolean cancelled = future.cancel(true);
    boolean stopped = task.awaitFinished(2, TimeUnit.SECONDS);
    System.out.println("  cancel(true)=" + cancelled
        + ", stopped=" + stopped
        + ", resourceClosed=" + task.resourceClosed());
  }
}
