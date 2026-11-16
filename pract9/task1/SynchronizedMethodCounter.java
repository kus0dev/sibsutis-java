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
