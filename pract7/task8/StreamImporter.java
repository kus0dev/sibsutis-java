package task8;

import src.Course;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// потоковый импорт без readAllLines, с ограничением памяти
public class StreamImporter {
  private final int errorLimit;
  private final int cancelThreshold;

  public StreamImporter(int errorLimit, int cancelThreshold) {
    this.errorLimit = errorLimit;
    this.cancelThreshold = cancelThreshold;
  }

  public ImportStats importStream(BufferedReader reader) throws IOException {
    if (reader == null) throw new IllegalArgumentException("reader must not be null");
    int lineNo = 0;
    int okCount = 0;
    int badCount = 0;
    List<String> errors = new ArrayList<>(Math.min(errorLimit, 16));

    String line;
    while ((line = reader.readLine()) != null) {
      lineNo++;
      if (line.isBlank()) continue;
      try {
        String[] parts = line.split(";");
        if (parts.length < 3) throw new IllegalArgumentException("bad");
        long id = Long.parseLong(parts[0]);
        String title = parts[1];
        int hours = Integer.parseInt(parts[2]);
        // обрабатываем без хранения всего файла
        new Course(id, title, hours);
        okCount++;
      } catch (Exception e) {
        badCount++;
        if (errors.size() < errorLimit) {
          errors.add("line " + lineNo + ": " + e.getClass().getSimpleName());
        }
      }
      if (badCount >= cancelThreshold) {
        // отмена после заданного порога
        return new ImportStats(okCount, badCount, errors, true);
      }
    }
    return new ImportStats(okCount, badCount, errors, false);
  }

  public record ImportStats(int ok, int bad, List<String> errors, boolean cancelled) {
    public ImportStats {
      errors = List.copyOf(errors);
    }
  }
}
