package task1;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// задача реагирует на interruption: ловит его, восстанавливает флаг и закрывает ресурс
public class InterruptibleTask implements Callable<String> {
  private final CountDownLatch started = new CountDownLatch(1);
  private final CountDownLatch finished = new CountDownLatch(1);
  private volatile boolean resourceClosed;

  @Override
  public String call() throws Exception {
    started.countDown();
    try {
      // имитация долгого I/O: sleep прерывается через InterruptedException
      Thread.sleep(10_000);
      return "finished";
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt(); // флаг нельзя терять
      return "interrupted";
    } finally {
      resourceClosed = true; // ресурс освобождается в любом случае
      finished.countDown();
    }
  }

  public boolean awaitStarted(long timeout, TimeUnit unit) throws InterruptedException {
    return started.await(timeout, unit);
  }

  public boolean awaitFinished(long timeout, TimeUnit unit) throws InterruptedException {
    return finished.await(timeout, unit);
  }

  public boolean resourceClosed() {
    return resourceClosed;
  }
}
