package task7;

import src.Course;
import java.util.ArrayList;
import java.util.List;

// атомарный all-or-nothing: при любой ошибке не публикует половину каталога
public class PartialImporter {
  public ImportResult importAllOrNothing(String text) {
    if (text == null) {
      return new ImportResult.Failure(List.of(new ImportResult.ImportError(0, "text is null")));
    }
    String[] lines = text.split("\\R");
    List<ImportResult.ImportError> errors = new ArrayList<>();
    List<Course> parsed = new ArrayList<>();

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if (line.isBlank()) continue;
      try {
        String[] parts = line.split(";");
        if (parts.length < 3) {
          errors.add(new ImportResult.ImportError(i + 1, "bad format"));
          continue;
        }
        long id = Long.parseLong(parts[0]);
        String title = parts[1];
        int hours = Integer.parseInt(parts[2]);
        Course c = new Course(id, title, hours);
        parsed.add(c);
      } catch (Exception e) {
        errors.add(new ImportResult.ImportError(i + 1, e.getClass().getSimpleName()));
      }
    }

    // не публикуем половину - либо все валидные при строгом подходе, либо собираем ошибки
    // выбран контракт: если есть ошибки - возвращаем Failure и не публикуем частичный каталог
    if (!errors.isEmpty()) {
      return new ImportResult.Failure(errors);
    }
    return new ImportResult.Success(parsed);
  }
}
