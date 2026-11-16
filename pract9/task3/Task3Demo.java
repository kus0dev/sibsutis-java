package task3;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// демонстрация: гонка create для одного id и создание разных сессий
public class Task3Demo {
  private static final int THREADS = 32;

  public static void main(String[] args) throws InterruptedException {
    SessionRegistry registry = new SessionRegistry();
    AtomicInteger created = new AtomicInteger();

    runConcurrently(() -> {
      if (registry.create(new Session("S-1", "student-" + Thread.currentThread().threadId()))) {
        created.incrementAndGet();
      }
    });

    System.out.println("same id created: " + created.get());
    System.out.println("active sessions: " + registry.activeCount());
    System.out.println("owner: " + registry.find("S-1").map(Session::student).orElse("none"));

    AtomicInteger nextId = new AtomicInteger(100);
    AtomicInteger distinctCreated = new AtomicInteger();
    runConcurrently(() -> {
      int id = nextId.getAndIncrement();
      if (registry.create(new Session("S-" + id, "student-" + id))) {
        distinctCreated.incrementAndGet();
      }
    });

    System.out.println("distinct created: " + distinctCreated.get());
    System.out.println("active after distinct: " + registry.activeCount());
    System.out.println("close S-1: " + registry.close("S-1")
        + ", active: " + registry.activeCount());
  }

  private static void runConcurrently(Runnable task) throws InterruptedException {
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    CountDownLatch start = new CountDownLatch(1);
    try {
      for (int i = 0; i < THREADS; i++) {
        pool.execute(() -> {
          try {
            start.await();
            task.run();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        });
      }
      start.countDown();
    } finally {
      pool.shutdown();
      if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
        pool.shutdownNow();
      }
    }
  }
}
