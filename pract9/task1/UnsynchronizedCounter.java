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
