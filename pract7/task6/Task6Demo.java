package task6;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// демонстрация temp + move
public class Task6Demo {
  public static void main(String[] args) throws IOException {
    Path dir = Path.of("pract7/task6");
    Files.createDirectories(dir);
    Path target = dir.resolve("catalog.txt");
    AtomicReplace.replace(target, "new catalog\n");
    System.out.println(Files.readString(target));
  }
}
