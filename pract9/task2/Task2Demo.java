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
