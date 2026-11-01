package task5;

import src.Course;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// поиск самого длинного курса: Optional только как результат метода
public final class CourseAnalytics {
  // при равной длительности выбираем меньший code для детерминизма
  private static final Comparator<Course> LONGEST =
      Comparator.comparingInt(Course::durationHours)
          .thenComparing(Course::code);

  private CourseAnalytics() {}

  public static Optional<Course> longest(List<Course> courses) {
    if (courses == null) throw new IllegalArgumentException("courses must not be null");
    return courses.stream().max(LONGEST);
  }
}
