package task1;

import src.Course;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// каталог курсов: хранит список курсов
public class CourseCatalog {
  private final List<Course> courses = new ArrayList<>();

  public void add(Course course) {
    if (course == null) throw new IllegalArgumentException("course must not be null");
    courses.add(course);
  }

  public List<Course> courses() {
    return Collections.unmodifiableList(courses);
  }

  // сериализация в простой текстовый формат
  public String toText() {
    StringBuilder sb = new StringBuilder();
    for (Course c : courses) {
      sb.append(c.id()).append(';')
        .append(c.title().replace(";", ",")).append(';')
        .append(c.durationHours()).append(';')
        .append(c.status()).append('\n');
    }
    return sb.toString();
  }

  public static CourseCatalog fromText(String text) {
    if (text == null) throw new IllegalArgumentException("text must not be null");
    CourseCatalog catalog = new CourseCatalog();
    String[] lines = text.split("\\R");
    for (String line : lines) {
      if (line.isBlank()) continue;
      String[] parts = line.split(";");
      if (parts.length < 4) continue;
      long id = Long.parseLong(parts[0]);
      String title = parts[1].replace(",", ";");
      int hours = Integer.parseInt(parts[2]);
      Course course = new Course(id, title, hours);
      try {
        Course.Status status = Course.Status.valueOf(parts[3]);
        course.setStatus(status);
      } catch (Exception ignored) {
        // при ошибке парсинга статуса оставляем значение по умолчанию
      }
      catalog.add(course);
    }
    return catalog;
  }

  // сохраняет каталог в UTF-8
  public static void save(Path path, CourseCatalog catalog) throws IOException {
    if (path == null) throw new IllegalArgumentException("path must not be null");
    if (catalog == null) throw new IllegalArgumentException("catalog must not be null");
    // не перезаписываем, если путь указывает на каталог
    if (Files.exists(path) && Files.isDirectory(path)) {
      throw new IOException("path is a directory, not a file: " + path);
    }
    String content = catalog.toText();
    Files.writeString(path, content, StandardCharsets.UTF_8);
  }

  // читает каталог из UTF-8 с обработкой ошибок
  public static CourseCatalog load(Path path) throws IOException {
    if (path == null) throw new IllegalArgumentException("path must not be null");
    if (!Files.exists(path)) {
      throw new IOException("file does not exist: " + path);
    }
    if (Files.isDirectory(path)) {
      throw new IOException("path is a directory, not a file: " + path);
    }
    String content = Files.readString(path, StandardCharsets.UTF_8);
    return fromText(content);
  }
}
