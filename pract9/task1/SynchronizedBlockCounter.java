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
