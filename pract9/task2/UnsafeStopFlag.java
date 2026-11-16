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
