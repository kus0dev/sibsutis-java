package task1;

import src.Course;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// демонстрация: чтение/запись UTF-8, обработка отсутствия файла и каталога
public class Task1Demo {
  public static void main(String[] args) throws IOException {
    Path dir = Path.of("pract7/task1");
    Files.createDirectories(dir);
    Path file = dir.resolve("catalog.txt");

    // создаём и сохраняем каталог
    CourseCatalog catalog = new CourseCatalog();
    catalog.add(new Course(1, "Java", 10));
    catalog.add(new Course(2, "Algorithms", 16));
    CourseCatalog.save(file, catalog);

    // читаем обратно
    CourseCatalog loaded = CourseCatalog.load(file);
    System.out.println("loaded count: " + loaded.courses().size());
    for (Course c : loaded.courses()) {
      System.out.println(c.id() + " " + c.title());
    }

    // обработка отсутствия файла
    try {
      CourseCatalog.load(Path.of("pract7/task1/missing.txt"));
    } catch (IOException e) {
      System.out.println("missing: " + e.getMessage());
    }

    // обработка каталога вместо файла
    try {
      CourseCatalog.load(dir);
    } catch (IOException e) {
      System.out.println("dir: " + e.getMessage());
    }
  }
}
