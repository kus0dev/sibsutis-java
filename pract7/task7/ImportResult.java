package task7;

import src.Course;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// контракт результата импорта: либо атомарный успех, либо частичный с ошибками
public sealed interface ImportResult permits ImportResult.Success, ImportResult.Failure {
  record Success(List<Course> courses) implements ImportResult {
    public Success {
      courses = List.copyOf(courses);
    }
  }

  record Failure(List<ImportError> errors) implements ImportResult {
    public Failure {
      errors = List.copyOf(errors);
    }
    public boolean hasErrors() {
      return !errors.isEmpty();
    }
  }

  record ImportError(int lineNumber, String message) {}
}
