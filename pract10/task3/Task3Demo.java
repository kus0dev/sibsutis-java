package task3;

import src.Course;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// allOf + join сохраняет исходный порядок и типизацию
public class Task3Demo {
  private static final Map<String, Course> CATALOG = Map.of(
      "JAVA-101", new Course("JAVA-101", "Основы Java", "Иван Петров", 720),
      "DB-301", new Course("DB-301", "Базы данных", "Анна Смирнова", 540),
      "WEB-401", new Course("WEB-401", "Веб-разработка", "Ольга Ким", 600));

  public static void main(String[] args) throws Exception {
    AtomicInteger threadNumber = new AtomicInteger();
    ExecutorService io = Executors.newFixedThreadPool(4, task -> {
      Thread thread = new Thread(task, "io-" + threadNumber.incrementAndGet());
      thread.setDaemon(false);
      return thread;
    });
    try {
      List<String> ids = List.of("JAVA-101", "DB-301", "WEB-401");
      long start = System.nanoTime();
      List<Course> courses = loadAll(ids, io);
      System.out.println("allOf: elapsed=" + millis(start) + "ms");
      for (int i = 0; i < ids.size(); i++) {
        System.out.println("  " + i + ": " + ids.get(i) + " -> " + courses.get(i).title());
      }
    } finally {
      io.shutdown();
      if (!io.awaitTermination(3, TimeUnit.SECONDS)) {
        io.shutdownNow();
        io.awaitTermination(3, TimeUnit.SECONDS);
      }
    }
  }

  private static List<Course> loadAll(List<String> ids, ExecutorService io) {
    List<CompletableFuture<Course>> futures = new ArrayList<>();
    for (String id : ids) {
      futures.add(loadAsync(id, io));
    }

    CompletableFuture<?>[] array = futures.toArray(new CompletableFuture<?>[0]);
    CompletableFuture.allOf(array).join();

    // allOf не хранит результаты: забираем их из исходных future по порядку
    List<Course> courses = new ArrayList<>();
    for (CompletableFuture<Course> future : futures) {
      courses.add(future.join());
    }
    return courses;
  }

  private static CompletableFuture<Course> loadAsync(String id, ExecutorService io) {
    return CompletableFuture.supplyAsync(() -> {
      // WEB-401 отвечает медленнее остальных
      sleep(id.equals("WEB-401") ? 500 : 100);
      Course course = CATALOG.get(id);
      if (course == null) throw new IllegalArgumentException("unknown course: " + id);
      return course;
    }, io);
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("interrupted", e);
    }
  }

  private static long millis(long start) {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
  }
}
