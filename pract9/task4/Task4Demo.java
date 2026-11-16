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
          new Producer("producer-" + (i + 1), queue, PER_PRODUCER, (long) index * PER_PRODUCER),
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
            new Producer("producer-" + (i + 1), queue, PER_PRODUCER, (long) index * PER_PRODUCER)));
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
