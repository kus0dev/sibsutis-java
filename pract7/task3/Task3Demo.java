package task3;

import java.io.IOException;

// доказать закрытие обоих ресурсов и порядок close (LIFO)
public class Task3Demo {
  public static void main(String[] args) {
    try (PipelineResource first = new PipelineResource("first");
         PipelineResource second = new PipelineResource("second")) {
      System.out.println("opened both");
      first.doWork(); // исключение в середине
    } catch (Exception e) {
      System.out.println("caught: " + e.getMessage());
    }
  }
}
