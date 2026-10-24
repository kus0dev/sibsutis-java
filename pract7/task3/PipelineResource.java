package task3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

// демонстрация try-with-resources: закрытие обоих ресурсов, правильный порядок close
public class PipelineResource implements AutoCloseable {
  private final String name;
  private boolean closed = false;

  public PipelineResource(String name) {
    this.name = name;
  }

  public void doWork() throws IOException {
    if (closed) throw new IllegalStateException("already closed: " + name);
    throw new IOException("error in middle of pipeline: " + name);
  }

  @Override
  public void close() throws Exception {
    System.out.println("closing: " + name);
    closed = true;
  }

  public boolean isClosed() {
    return closed;
  }
}
