package task2;

import src.Course;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// цепочка supplyAsync -> thenApply -> thenAccept
public class Task2Demo {
  private static final Map<String, Course> CATALOG = Map.of(
      "JAVA-101", new Course("JAVA-101", "Основы Java", "Иван Петров", 720),
      "DB-301", new Course("DB-301", "Базы данных", "Анна Смирнова", 540));

  public static void main(String[] args) throws Exception {
    AtomicInteger threadNumber = new AtomicInteger();
    ExecutorService io = Executors.newFixedThreadPool(3, task -> {
      Thread thread = new Thread(task, "io-" + threadNumber.incrementAndGet());
      thread.setDaemon(false);
      return thread;
    });
    try {
      runCardChain(io);
    } finally {
      io.shutdown();
      if (!io.awaitTermination(3, TimeUnit.SECONDS)) {
        io.shutdownNow();
        io.awaitTermination(3, TimeUnit.SECONDS);
      }
    }
  }

  private static void runCardChain(ExecutorService io) {
    System.out.println("card chain (caller=" + Thread.currentThread().getName() + "):");
    CompletableFuture<Course> load = CompletableFuture.supplyAsync(() -> {
      log("supplyAsync: load course");
      sleep(100);
      return loadCourse("JAVA-101");
    }, io);

    CompletableFuture<String> format = load.thenApply(course -> {
      log("thenApply: format card");
      return course.id() + " · " + course.title() + " — " + course.author()
          + ", " + course.minutes() + " мин";
    });

    CompletableFuture<Void> print = format.thenAccept(card -> {
      log("thenAccept: print card");
      System.out.println("  " + card);
    });
    print.join();
  }

  private static Course loadCourse(String id) {
    Course course = CATALOG.get(id);
    if (course == null) throw new IllegalArgumentException("unknown course: " + id);
    return course;
  }

  private static void log(String stage) {
    System.out.println("  [" + Thread.currentThread().getName() + "] " + stage);
  }

  // sleep моделирует ожидание I/O; для CPU-работы он не подходит
  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("interrupted", e);
    }
  }
}
