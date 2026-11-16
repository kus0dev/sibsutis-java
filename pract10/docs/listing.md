# Практическая работа №10 — листинги

Полные исходные листинги всех заданий. В заголовке каждого листинга указан путь к файлу.

## Содержание

- Конфигурация сборки
- Общая модель
- Задание 1. `Callable` и `Future`
- Задание 2. Последовательное преобразование
- Задание 3. `allOf` с сохранением результатов
- Задание 4. Правильный executor
- Задание 5. Platform и virtual threads
- Задание 6. Общий deadline

---

## Конфигурация сборки

### pom.xml

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>coursehub-async</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <build>
        <!-- код заданий лежит в taskN, общий код будет в src -->
        <sourceDirectory>${project.basedir}</sourceDirectory>
    </build>
</project>
```

---

## Общая модель

### src/Course.java

```java
package src;

// общая модель курса: используется в заданиях 2 и 3
public record Course(String id, String title, String author, int minutes) {
}
```

---

## Задание 1. `Callable` и `Future`

### task1/InterruptibleTask.java

```java
package task1;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// задача реагирует на interruption: ловит его, восстанавливает флаг и закрывает ресурс
public class InterruptibleTask implements Callable<String> {
  private final CountDownLatch started = new CountDownLatch(1);
  private final CountDownLatch finished = new CountDownLatch(1);
  private volatile boolean resourceClosed;

  @Override
  public String call() throws Exception {
    started.countDown();
    try {
      // имитация долгого I/O: sleep прерывается через InterruptedException
      Thread.sleep(10_000);
      return "finished";
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt(); // флаг нельзя терять
      return "interrupted";
    } finally {
      resourceClosed = true; // ресурс освобождается в любом случае
      finished.countDown();
    }
  }

  public boolean awaitStarted(long timeout, TimeUnit unit) throws InterruptedException {
    return started.await(timeout, unit);
  }

  public boolean awaitFinished(long timeout, TimeUnit unit) throws InterruptedException {
    return finished.await(timeout, unit);
  }

  public boolean resourceClosed() {
    return resourceClosed;
  }
}
```

### task1/Task1Demo.java

```java
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
    Future<?> runnable = pool.submit(() -> System.out.println("  Runnable: result is void"));
    Object runnableResult = runnable.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

    Future<String> callable = pool.submit(() -> "Callable: result is a value");
    String callableResult = callable.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

    System.out.println("Runnable vs Callable:");
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
```

---

## Задание 2. Последовательное преобразование

### task2/Task2Demo.java

```java
package task2;

import src.Course;

import java.util.List;
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
      runEnrollmentChain(io);
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

  private static void runEnrollmentChain(ExecutorService io) {
    System.out.println("enrollment chain (caller=" + Thread.currentThread().getName() + "):");
    CompletableFuture<User> user = CompletableFuture.supplyAsync(() -> {
      log("supplyAsync: load user");
      sleep(100);
      return new User(7, "Денис");
    }, io);

    // thenCompose разворачивает вложенный future в плоский
    CompletableFuture<List<Enrollment>> enrollments = user.thenCompose(value -> {
      log("thenCompose: switch to enrollments");
      return CompletableFuture.supplyAsync(() -> {
        log("supplyAsync: load enrollments");
        sleep(100);
        return loadEnrollments(value.id());
      }, io);
    });

    List<Enrollment> result = enrollments.join();
    System.out.println("  user=" + user.join().name() + ", enrollments=" + result.size());
    for (Enrollment enrollment : result) {
      System.out.println("    " + enrollment.courseId() + " progress=" + enrollment.progress() + "%");
    }
  }

  private static Course loadCourse(String id) {
    Course course = CATALOG.get(id);
    if (course == null) throw new IllegalArgumentException("unknown course: " + id);
    return course;
  }

  private static List<Enrollment> loadEnrollments(long userId) {
    if (userId != 7) throw new IllegalArgumentException("unknown user: " + userId);
    return List.of(
        new Enrollment(userId, "JAVA-101", 40),
        new Enrollment(userId, "DB-301", 15));
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

  record User(long id, String name) {
  }

  record Enrollment(long userId, String courseId, int progress) {
  }
}
```

---

## Задание 3. `allOf` с сохранением результатов

### task3/Task3Demo.java

```java
package task3;

import src.Course;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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

      List<String> withFailure = List.of("JAVA-101", "NOPE-1", "WEB-401");
      start = System.nanoTime();
      try {
        loadAllOrFail(withFailure, io);
      } catch (CompletionException e) {
        System.out.println("fail-fast: elapsed=" + millis(start) + "ms"
            + ", error=" + e.getCause().getMessage());
      }

      start = System.nanoTime();
      PartialResult partial = loadPartial(withFailure, io);
      System.out.println("partial: elapsed=" + millis(start) + "ms"
          + ", courses=" + partial.courses().size()
          + ", errors=" + partial.errors().size());
      for (Course course : partial.courses()) {
        System.out.println("  ok: " + course.id());
      }
      for (String error : partial.errors()) {
        System.out.println("  error: " + error);
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
    return order(futures);
  }

  // fail-fast по смыслу: результат завершается ошибкой сразу после первой неудачи
  private static List<Course> loadAllOrFail(List<String> ids, ExecutorService io) {
    List<CompletableFuture<Course>> futures = new ArrayList<>();
    for (String id : ids) {
      futures.add(loadAsync(id, io));
    }

    CompletableFuture<List<Course>> result = new CompletableFuture<>();
    AtomicInteger remaining = new AtomicInteger(futures.size());
    for (CompletableFuture<Course> future : futures) {
      future.whenComplete((course, error) -> {
        if (error != null) {
          Throwable cause = error instanceof CompletionException ? error.getCause() : error;
          result.completeExceptionally(cause);
        } else if (remaining.decrementAndGet() == 0) {
          result.complete(order(futures));
        }
      });
    }
    return result.join();
  }

  // частичный результат: ждём все задачи и отдельно собираем ошибки
  private static PartialResult loadPartial(List<String> ids, ExecutorService io) {
    List<CompletableFuture<Course>> futures = new ArrayList<>();
    for (String id : ids) {
      futures.add(loadAsync(id, io));
    }

    CompletableFuture<?>[] array = futures.toArray(new CompletableFuture<?>[0]);
    // handle не даёт allOf завершиться исключительно: дожидаемся всех задач
    CompletableFuture.allOf(array).handle((value, error) -> null).join();

    List<Course> courses = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    for (int i = 0; i < ids.size(); i++) {
      try {
        courses.add(futures.get(i).join());
      } catch (CompletionException e) {
        errors.add(ids.get(i) + ": " + e.getCause().getMessage());
      }
    }
    return new PartialResult(courses, errors);
  }

  private static List<Course> order(List<CompletableFuture<Course>> futures) {
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

  record PartialResult(List<Course> courses, List<String> errors) {
  }
}
```

---

## Задание 4. Правильный executor

### task4/Task4Demo.java

```java
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
```

---

## Задание 5. Platform и virtual threads

### task5/Task5Demo.java

```java
package task5;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

// учебный эксперимент: много блокирующих задач на platform и virtual threads
public class Task5Demo {
  private static final int TASKS = 1000;
  private static final int BLOCK_MILLIS = 20;
  private static final int PLATFORM_THREADS = 32;

  public static void main(String[] args) throws Exception {
    System.out.println("tasks=" + TASKS + ", blocking=" + BLOCK_MILLIS + "ms, cores="
        + Runtime.getRuntime().availableProcessors());
    System.out.println("blocking wait only: sleep is not a model of CPU work");
    run("platform", Executors.newFixedThreadPool(PLATFORM_THREADS));
    run("virtual", Executors.newVirtualThreadPerTaskExecutor());
  }

  private static void run(String name, ExecutorService executor) throws Exception {
    ThreadMXBean threads = ManagementFactory.getThreadMXBean();
    threads.resetPeakThreadCount();
    int baseline = threads.getThreadCount();

    List<Callable<Void>> tasks = new ArrayList<>();
    for (int i = 0; i < TASKS; i++) {
      tasks.add(() -> {
        try {
          Thread.sleep(BLOCK_MILLIS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        return null;
      });
    }

    long start = System.nanoTime();
    List<Future<Void>> futures = executor.invokeAll(tasks);
    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    int peak = threads.getPeakThreadCount();

    int completed = 0;
    for (Future<Void> future : futures) {
      if (future.isDone() && !future.isCancelled()) {
        completed++;
      }
    }
    System.out.println(name + ": elapsed=" + elapsedMillis + "ms"
        + ", peak platform threads=" + peak + " (baseline " + baseline + ")"
        + ", completed=" + completed + "/" + TASKS);

    executor.shutdown();
    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
```

---

## Задание 6. Общий deadline

### task6/Task6Demo.java

```java
package task6;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// несколько стадий делят один deadline; перед каждой стадией считаем остаток
public class Task6Demo {
  private static final long BUDGET_MILLIS = 600;
  private static final long CANCEL_GRACE_MILLIS = 200;

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

    CountDownLatch finished = new CountDownLatch(1);
    Future<String> future = pool.submit(() -> {
      try {
        Thread.sleep(workMillis);
        return "done";
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return "interrupted";
      } finally {
        finished.countDown();
      }
    });

    try {
      System.out.println(name + ": " + future.get(remaining, TimeUnit.NANOSECONDS));
    } catch (TimeoutException e) {
      future.cancel(true);
      // отдельный небольшой бюджет именно на завершение отмены
      boolean stopped = finished.await(CANCEL_GRACE_MILLIS, TimeUnit.MILLISECONDS);
      System.out.println(name + ": timeout, cancel(true), stopped=" + stopped);
    } catch (ExecutionException e) {
      System.out.println(name + ": failed: " + e.getCause().getMessage());
    }
  }
}
```
