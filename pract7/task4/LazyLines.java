package task4;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

// Files.lines только внутри try-with-resources; не возвращать stream наружу
public class LazyLines {
  // правильный способ: обработка внутри try-with-resources
  public static long countNonEmpty(Path path) throws IOException {
    if (path == null) throw new IllegalArgumentException("path must not be null");
    try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
      return lines.filter(s -> s != null && !s.isBlank()).count();
    }
    // ресурс Stream закрывается здесь
  }

  // неправильный способ (демонстрация) - возвращает stream наружу
  // вызывающий может не закрыть его -> утечка ресурса
  public static Stream<String> linesLeaked(Path path) throws IOException {
    if (path == null) throw new IllegalArgumentException("path must not be null");
    return Files.lines(path, StandardCharsets.UTF_8); // владелец не определён
  }
}
