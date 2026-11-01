package task7;

import src.Level;
import java.util.Locale;

// событие enrollment: студент, курс, уровень и тип
public record EnrollmentEvent(long studentId, String courseCode, Level level, Type type) {
  public enum Type { ENROLLED, COMPLETED, DROPPED }

  public EnrollmentEvent {
    if (studentId <= 0) throw new IllegalArgumentException("studentId must be > 0");
    if (courseCode == null || courseCode.isBlank()) {
      throw new IllegalArgumentException("courseCode must not be blank");
    }
    if (level == null) throw new IllegalArgumentException("level must not be null");
    if (type == null) throw new IllegalArgumentException("type must not be null");
    courseCode = courseCode.trim().toUpperCase(Locale.ROOT);
  }
}
