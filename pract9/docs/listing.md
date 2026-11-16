# Практическая работа №9 — листинги

Полные исходные листинги всех заданий. В заголовке каждого листинга указан путь к файлу.

## Содержание

- Конфигурация сборки
- Задание 1. Потерянные инкременты
- Задание 2. Флаг остановки и visibility
- Задание 3. Потокобезопасный реестр
- Задание 4. Producer–consumer
- Задание 5. Детерминированный конкурентный тест
- Задание 6. Запись на курс с ограничением мест

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
    <artifactId>coursehub-concurrency</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <!-- код заданий лежит в taskN, общий код будет в src -->
        <sourceDirectory>${project.basedir}</sourceDirectory>
        <!-- тесты лежат рядом с заданием: task5/RaceTest.java -->
        <testSourceDirectory>${project.basedir}</testSourceDirectory>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <!-- тесты компилируются отдельно, JUnit не нужен основному коду -->
                    <excludes>
                        <exclude>**/*Test.java</exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Задание 1. Потерянные инкременты

### task1/UnsynchronizedCounter.java

```java
package task1;

// небезопасный счётчик: инкремент разложен на чтение, вычисление и запись
public class UnsynchronizedCounter {
  private int value;

  public void increment() {
    int current = value;   // чтение
    Thread.yield();        // расширяем окно, чтобы гонка проявлялась стабильно
    value = current + 1;   // запись
  }

  public int value() {
    return value;
  }
}
```

### task1/SynchronizedMethodCounter.java

```java
package task1;

// исправление через synchronized-метод: монитор — this
public class SynchronizedMethodCounter {
  private int value;

  public synchronized void increment() {
    value++;
  }

  public synchronized int value() {
    return value;
  }
}
```

### task1/SynchronizedBlockCounter.java

```java
package task1;

// тот же инвариант, но под минимальным synchronized-блоком
public class SynchronizedBlockCounter {
  private final Object lock = new Object();
  private int value;

  public void increment() {
    synchronized (lock) {
      value++;
    }
  }

  public int value() {
    synchronized (lock) {
      return value;
    }
  }
}
```

### task1/Task1Demo.java

```java
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
```

---

## Задание 2. Флаг остановки и visibility

### task2/UnsafeStopFlag.java

```java
package task2;

// некорректный флаг: запись из другого потока может быть не видна
public class UnsafeStopFlag implements Runnable {
  private boolean running = true;

  public void stop() {
    running = false;
  }

  @Override
  public void run() {
    while (running) {
      // busy loop
    }
  }
}
```

### task2/VolatileStopFlag.java

```java
package task2;

// volatile гарантирует видимость записи флага
public class VolatileStopFlag implements Runnable {
  private volatile boolean running = true;

  public void stop() {
    running = false;
  }

  @Override
  public void run() {
    while (running) {
      // busy loop
    }
  }
}
```

### task2/Task2Demo.java

```java
package task2;

// демонстрация: обычный флаг может остаться в кеше потока, volatile — нет
public class Task2Demo {
  public static void main(String[] args) throws InterruptedException {
    System.out.println("plain flag worker:");
    runPlain();

    System.out.println("volatile flag worker:");
    runVolatile();
  }

  private static void runPlain() throws InterruptedException {
    UnsafeStopFlag worker = new UnsafeStopFlag();
    Thread thread = new Thread(worker, "plain-stop-worker");
    thread.setDaemon(true); // если флаг не виден, поток не должен держать JVM
    thread.start();

    Thread.sleep(100); // даём циклу поработать
    worker.stop();
    thread.join(500);

    System.out.println("  stopped: " + !thread.isAlive());
    if (thread.isAlive()) {
      System.out.println("  worker still sees running=true");
    }
  }

  private static void runVolatile() throws InterruptedException {
    VolatileStopFlag worker = new VolatileStopFlag();
    Thread thread = new Thread(worker, "volatile-stop-worker");
    thread.start();

    Thread.sleep(100);
    worker.stop();
    thread.join(500);

    System.out.println("  stopped: " + !thread.isAlive());
  }
}
```

---

## Задание 3. Потокобезопасный реестр

### task3/Session.java

```java
package task3;

// активная сессия студента
public record Session(String id, String student) {
  public Session {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id must not be blank");
    }
    if (student == null || student.isBlank()) {
      throw new IllegalArgumentException("student must not be blank");
    }
  }
}
```

### task3/SessionRegistry.java

```java
package task3;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// реестр сессий: создание и закрытие атомарны на уровне ConcurrentHashMap
public class SessionRegistry {
  private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

  public boolean create(Session session) {
    if (session == null) throw new IllegalArgumentException("session must not be null");
    // putIfAbsent проверяет и вставляет под внутренней блокировкой корзины
    return sessions.putIfAbsent(session.id(), session) == null;
  }

  public Optional<Session> find(String id) {
    if (id == null) throw new IllegalArgumentException("id must not be null");
    return Optional.ofNullable(sessions.get(id));
  }

  public boolean close(String id) {
    if (id == null) throw new IllegalArgumentException("id must not be null");
    return sessions.remove(id) != null;
  }

  public int activeCount() {
    return sessions.size();
  }
}
```

### task3/Task3Demo.java

```java
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
```

---

## Задание 4. Producer–consumer

### task4/Request.java

```java
package task4;

// заявка в очереди; отрицательный id — служебный сигнал остановки
public record Request(long id, String courseCode) {
  public Request {
    if (courseCode == null || courseCode.isBlank()) {
      throw new IllegalArgumentException("courseCode must not be blank");
    }
  }

  public static Request stop() {
    return new Request(-1, "STOP");
  }

  public boolean isStop() {
    return id < 0;
  }
}
```

### task4/Producer.java

```java
package task4;

import java.util.concurrent.BlockingQueue;

// producer блокируется на put, когда очередь заполнена: это и есть backpressure
public class Producer implements Runnable {
  private final BlockingQueue<Request> queue;
  private final int count;
  private final long firstId;

  public Producer(BlockingQueue<Request> queue, int count, long firstId) {
    if (queue == null) throw new IllegalArgumentException("queue must not be null");
    if (count < 0) throw new IllegalArgumentException("count must be >= 0");
    this.queue = queue;
    this.count = count;
    this.firstId = firstId;
  }

  @Override
  public void run() {
    try {
      for (int i = 0; i < count; i++) {
        queue.put(new Request(firstId + i, "COURSE-" + ((firstId + i) % 5 + 1)));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
```

### task4/Consumer.java

```java
package task4;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

// consumer блокируется на take и завершается по служебной заявке
public class Consumer implements Runnable {
  private final BlockingQueue<Request> queue;
  private final AtomicInteger processed;

  public Consumer(BlockingQueue<Request> queue, AtomicInteger processed) {
    if (queue == null) throw new IllegalArgumentException("queue must not be null");
    if (processed == null) throw new IllegalArgumentException("processed must not be null");
    this.queue = queue;
    this.processed = processed;
  }

  @Override
  public void run() {
    try {
      while (true) {
        Request request = queue.take();
        if (request.isStop()) {
          return;
        }
        processed.incrementAndGet();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
```

### task4/Task4Demo.java

```java
package task4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// producer-consumer: сначала ручные потоки, затем тот же pipeline на ExecutorService
public class Task4Demo {
  private static final int CAPACITY = 4;
  private static final int PRODUCERS = 2;
  private static final int CONSUMERS = 2;
  private static final int PER_PRODUCER = 500;
  private static final int TOTAL = PRODUCERS * PER_PRODUCER;
  private static final long TIMEOUT_SECONDS = 5;

  public static void main(String[] args) throws Exception {
    System.out.println("queue capacity: " + CAPACITY + " (producers block when full)");
    runRawThreads();
    runExecutor();
  }

  private static void runRawThreads() throws InterruptedException {
    BlockingQueue<Request> queue = new ArrayBlockingQueue<>(CAPACITY);
    AtomicInteger processed = new AtomicInteger();
    List<Thread> producers = new ArrayList<>();
    List<Thread> consumers = new ArrayList<>();

    for (int i = 0; i < PRODUCERS; i++) {
      int index = i;
      producers.add(new Thread(
          new Producer(queue, PER_PRODUCER, (long) index * PER_PRODUCER),
          "producer-" + (i + 1)));
    }
    for (int i = 0; i < CONSUMERS; i++) {
      consumers.add(new Thread(new Consumer(queue, processed), "consumer-" + (i + 1)));
    }

    producers.forEach(Thread::start);
    consumers.forEach(Thread::start);

    boolean producersDone = true;
    for (Thread producer : producers) {
      producersDone &= join(producer);
    }
    boolean stopDelivered = true;
    for (int i = 0; i < CONSUMERS; i++) {
      stopDelivered &= queue.offer(Request.stop(), TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
    boolean consumersDone = true;
    for (Thread consumer : consumers) {
      consumersDone &= join(consumer);
    }

    System.out.println("raw threads: processed=" + processed.get() + "/" + TOTAL
        + ", producersDone=" + producersDone
        + ", stopDelivered=" + stopDelivered
        + ", consumersDone=" + consumersDone);
  }

  private static void runExecutor() throws Exception {
    BlockingQueue<Request> queue = new ArrayBlockingQueue<>(CAPACITY);
    AtomicInteger processed = new AtomicInteger();
    AtomicInteger workerNumber = new AtomicInteger();

    // consumers блокируются на take, поэтому пул вмещает всех producers и consumers.
    // Для чисто CPU-bound задач размер пула был бы равен числу ядер.
    ThreadFactory factory = task -> {
      Thread thread = new Thread(task, "request-worker-" + workerNumber.incrementAndGet());
      thread.setDaemon(false);
      return thread;
    };
    ExecutorService pool = Executors.newFixedThreadPool(PRODUCERS + CONSUMERS, factory);
    List<Future<?>> producerFutures = new ArrayList<>();
    List<Future<?>> consumerFutures = new ArrayList<>();
    try {
      for (int i = 0; i < PRODUCERS; i++) {
        int index = i;
        producerFutures.add(pool.submit(
            new Producer(queue, PER_PRODUCER, (long) index * PER_PRODUCER)));
      }
      for (int i = 0; i < CONSUMERS; i++) {
        consumerFutures.add(pool.submit(new Consumer(queue, processed)));
      }

      for (Future<?> future : producerFutures) {
        future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      }
      for (int i = 0; i < CONSUMERS; i++) {
        if (!queue.offer(Request.stop(), TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
          throw new IllegalStateException("could not deliver stop signal");
        }
      }
      for (Future<?> future : consumerFutures) {
        future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdown();
      if (!pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        pool.shutdownNow();
        pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      }
    }

    System.out.println("executor: processed=" + processed.get() + "/" + TOTAL
        + ", poolSize=" + (PRODUCERS + CONSUMERS));
  }

  // join с ограничением: если поток не завершился, прерываем его
  private static boolean join(Thread thread) throws InterruptedException {
    thread.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
    if (thread.isAlive()) {
      thread.interrupt();
      return false;
    }
    return true;
  }
}
```

---

## Задание 5. Детерминированный конкурентный тест

### task5/RaceTest.java

```java
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
```

---

## Задание 6. Запись на курс с ограничением мест

### task6/LockedCourse.java

```java
package task6;

import java.util.HashSet;
import java.util.Set;

// вариант 1: один coarse-grained lock защищает и места, и список студентов
public class LockedCourse {
  private final int capacity;
  private final Set<Long> students = new HashSet<>();

  public LockedCourse(int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
    this.capacity = capacity;
  }

  public synchronized boolean enroll(long studentId) {
    if (studentId <= 0) throw new IllegalArgumentException("studentId must be > 0");
    if (students.size() >= capacity || students.contains(studentId)) {
      return false;
    }
    students.add(studentId);
    return true;
  }

  public synchronized int enrolled() {
    return students.size();
  }

  public synchronized int available() {
    return capacity - students.size();
  }
}
```

### task6/AtomicCourse.java

```java
package task6;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// вариант 2: concurrent set для уникальности и CAS для резервирования мест
public class AtomicCourse {
  private final int capacity;
  private final Set<Long> students = ConcurrentHashMap.newKeySet();
  private final AtomicInteger reserved = new AtomicInteger();

  public AtomicCourse(int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
    this.capacity = capacity;
  }

  public boolean enroll(long studentId) {
    if (studentId <= 0) throw new IllegalArgumentException("studentId must be > 0");
    if (students.contains(studentId)) {
      return false;
    }

    // сначала резервируем место, не допуская переполнения capacity
    while (true) {
      int current = reserved.get();
      if (current >= capacity) {
        return false;
      }
      if (reserved.compareAndSet(current, current + 1)) {
        break;
      }
    }

    // если студент уже успел записаться, возвращаем резерв
    if (students.add(studentId)) {
      return true;
    }
    reserved.decrementAndGet();
    return false;
  }

  public int enrolled() {
    return students.size();
  }

  public int available() {
    return capacity - students.size();
  }
}
```

### task6/Task6Demo.java

```java
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
```
