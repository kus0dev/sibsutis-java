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
