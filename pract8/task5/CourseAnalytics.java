package task5;

import src.Course;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

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

  // преобразование без Optional.get
  public static Optional<String> longestTitle(List<Course> courses) {
    return longest(courses).map(Course::title);
  }

  // fallback, если курсов нет
  public static String longestTitleOrDefault(List<Course> courses, String fallback) {
    if (fallback == null) throw new IllegalArgumentException("fallback must not be null");
    return longestTitle(courses).orElse(fallback);
  }

  // ленивый fallback: вычисляется только при пустом Optional
  public static String longestTitleOrComputed(List<Course> courses, Supplier<String> fallback) {
    if (fallback == null) throw new IllegalArgumentException("fallback must not be null");
    return longestTitle(courses).orElseGet(fallback);
  }
}
