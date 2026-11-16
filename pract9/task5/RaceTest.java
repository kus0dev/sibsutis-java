package task5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import task1.SynchronizedMethodCounter;
import task1.UnsynchronizedCounter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

// гонка запускается latch-ами: без Thread.sleep и без опроса флагов
@Timeout(20)
public class RaceTest {
  private static final int THREADS = 8;
  private static final int PER_THREAD = 50_000;
  private static final int EXPECTED = THREADS * PER_THREAD;
  private static final int ATTEMPTS = 10;

  @Test
  void unsafeCounterLosesUpdates() throws Exception {
    int actual = EXPECTED;
    int attempts = 0;

    // гонка не обязана проявиться с первого раза, поэтому повторяем попытки
    while (attempts < ATTEMPTS) {
      attempts++;
      UnsynchronizedCounter counter = new UnsynchronizedCounter();
      actual = runCounter(counter::increment, counter::value);
      if (actual < EXPECTED) {
        break;
      }
    }

    int lost = EXPECTED - actual;
    assertTrue(lost > 0, "race was not observed in " + ATTEMPTS + " attempts");
  }

  @Test
  void synchronizedCounterKeepsAllUpdates() throws Exception {
    SynchronizedMethodCounter counter = new SynchronizedMethodCounter();

    int actual = runCounter(counter::increment, counter::value);

    assertEquals(EXPECTED, actual);
  }

  private static int runCounter(Runnable increment, IntSupplier value) throws InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(THREADS);
    CountDownLatch ready = new CountDownLatch(THREADS);
    CountDownLatch start = new CountDownLatch(1);
    try {
      for (int i = 0; i < THREADS; i++) {
        executor.execute(() -> {
          ready.countDown();
          try {
            start.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
          }
          for (int j = 0; j < PER_THREAD; j++) {
            increment.run();
          }
        });
      }

      if (!ready.await(5, TimeUnit.SECONDS)) {
        fail("workers did not become ready");
      }
      start.countDown();
    } finally {
      executor.shutdown();
      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
      }
    }
    return value.getAsInt();
  }
}
