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
