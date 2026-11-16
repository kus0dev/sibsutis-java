package task6;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.function.LongPredicate;

// демонстрация: много потоков одновременно записываются на курс с capacity
public class Task6Demo {
  private static final int CAPACITY = 10;
  private static final int STUDENTS = 100;
  private static final int THREADS = 32;

  public static void main(String[] args) throws InterruptedException {
    LockedCourse locked = new LockedCourse(CAPACITY);
    check("coarse lock", locked::enroll, locked::enrolled, locked::available);

    AtomicCourse atomic = new AtomicCourse(CAPACITY);
    check("atomics + concurrent set", atomic::enroll, atomic::enrolled, atomic::available);
  }

  private static void check(
      String name,
      LongPredicate enroll,
      IntSupplier enrolled,
      IntSupplier available) throws InterruptedException {
    AtomicInteger successes = new AtomicInteger();
    AtomicInteger duplicates = new AtomicInteger();
    Set<Long> winners = ConcurrentHashMap.newKeySet();

    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    CountDownLatch start = new CountDownLatch(1);
    try {
      for (long studentId = 1; studentId <= STUDENTS; studentId++) {
        long id = studentId;
        pool.execute(() -> {
          try {
            start.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
          }
          // каждый студент пытается записаться дважды
          for (int attempt = 0; attempt < 2; attempt++) {
            if (enroll.test(id)) {
              successes.incrementAndGet();
              if (!winners.add(id)) {
                duplicates.incrementAndGet();
              }
            }
          }
        });
      }
      start.countDown();
    } finally {
      pool.shutdown();
      if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
        pool.shutdownNow();
        pool.awaitTermination(5, TimeUnit.SECONDS);
      }
    }

    int enrolledNow = enrolled.getAsInt();
    System.out.println(name + ": successes=" + successes.get()
        + ", unique=" + winners.size()
        + ", duplicates=" + duplicates.get()
        + ", enrolled=" + enrolledNow
        + ", available=" + available.getAsInt());

    if (successes.get() > CAPACITY) {
      throw new AssertionError("capacity exceeded");
    }
    if (successes.get() != CAPACITY) {
      throw new AssertionError("seats were not filled");
    }
    if (winners.size() != successes.get()) {
      throw new AssertionError("student enrolled twice");
    }
    if (enrolledNow != winners.size()) {
      throw new AssertionError("enrolled count mismatch");
    }

    int before = enrolledNow;
    boolean rejected = !enroll.test(999);
    int after = enrolled.getAsInt();
    System.out.println("  rejection unchanged: " + (rejected && before == after));
    if (!rejected || before != after) {
      throw new AssertionError("rejection changed state");
    }
  }
}
