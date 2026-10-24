package task6;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

// временный файл в том же каталоге, flush/close, замена через move
public class AtomicReplace {
  public static void replace(Path target, String content) throws IOException {
    if (target == null || content == null) throw new IllegalArgumentException("null args");
    Path dir = target.getParent();
    if (dir == null) dir = Path.of(".");
    // временный файл в том же каталоге
    Path tmp = Files.createTempFile(dir, "catalog", ".tmp");
    try {
      Files.writeString(tmp, content, StandardCharsets.UTF_8);
      // flush/close уже сделаны Files.writeString
      try {
        // попытка атомарной замены
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        // обработка отсутствия atomic move
        System.out.println("atomic move not supported, using non-atomic replace");
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (Exception e) {
      // удаляем временный файл при ошибке
      try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
      throw e;
    }
  }
}
