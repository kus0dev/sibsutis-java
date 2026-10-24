package task4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

// демонстрация: Files.lines требует владения ресурсом
public class Task4Demo {
  public static void main(String[] args) throws IOException {
    Path dir = Path.of("pract7/task4");
    Files.createDirectories(dir);
    Path file = dir.resolve("data.txt");
    Files.writeString(file, "a\n\nb\n");

    // правильный вариант
    long count = LazyLines.countNonEmpty(file);
    System.out.println("count: " + count);

    // ошибочный возврат наружу - показано, кто обязан владеть ресурсом
    try {
      Stream<String> leaked = LazyLines.linesLeaked(file);
      // если забыть close - ресурс не закрыт
      System.out.println(leaked.count());
      // leaked.close(); // обязателен, если возвращаем наружу
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
