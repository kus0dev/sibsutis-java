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
