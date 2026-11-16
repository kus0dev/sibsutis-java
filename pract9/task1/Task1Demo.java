package task1;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;

// демонстрация: потерянные инкременты и два способа синхронизации
public class Task1Demo {
  private static final int THREADS = 8;
  private static final int PER_THREAD = 50_000;
  private static final int EXPECTED = THREADS * PER_THREAD;
  private static final int ATTEMPTS = 10;

  public static void main(String[] args) throws InterruptedException {
    int actual = EXPECTED;
    int attempts = 0;
    while (attempts < ATTEMPTS) {
      attempts++;
      UnsynchronizedCounter counter = new UnsynchronizedCounter();
      actual = run(counter::increment, counter::value);
      if (actual < EXPECTED) {
        break;
      }
    }
    System.out.println("expected: " + EXPECTED);
    System.out.println("unsafe: attempts=" + attempts + ", actual=" + actual
        + ", lost=" + (EXPECTED - actual));

    SynchronizedMethodCounter methodCounter = new SynchronizedMethodCounter();
    int methodActual = run(methodCounter::increment, methodCounter::value);
    System.out.println("synchronized method: " + methodActual);

    SynchronizedBlockCounter blockCounter = new SynchronizedBlockCounter();
    int blockActual = run(blockCounter::increment, blockCounter::value);
    System.out.println("synchronized block: " + blockActual);

    boolean allIncrementsKept = methodActual == EXPECTED && blockActual == EXPECTED;
    System.out.println("all increments kept: " + allIncrementsKept);
  }

  private static int run(Runnable increment, IntSupplier value) throws InterruptedException {
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    CountDownLatch start = new CountDownLatch(1);
    try {
      for (int i = 0; i < THREADS; i++) {
        pool.execute(() -> incrementAfterStart(start, increment));
      }
      start.countDown();
    } finally {
      pool.shutdown();
      if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
        pool.shutdownNow();
      }
    }
    return value.getAsInt();
  }

  private static void incrementAfterStart(CountDownLatch start, Runnable increment) {
    try {
      start.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }
    for (int i = 0; i < PER_THREAD; i++) {
      increment.run();
    }
  }
}
