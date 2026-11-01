package task3;

import src.Course;
import java.util.List;

// reduce: identity должна быть нейтральной, accumulator - ассоциативным
public final class DurationReducer {
  private DurationReducer() {}

  public static int totalHours(List<Course> courses) {
    if (courses == null) throw new IllegalArgumentException("courses must not be null");
    return courses.stream()
        .mapToInt(Course::durationHours)
        .reduce(0, Integer::sum);
  }

  // правильная identity: 0 часов и 0 курсов
  public static DurationSummary summary(List<Course> courses) {
    if (courses == null) throw new IllegalArgumentException("courses must not be null");
    return courses.stream()
        .map(course -> new DurationSummary(course.durationHours(), 1))
        .reduce(DurationSummary.empty(), DurationSummary::plus);
  }

  // та же операция в parallel: combiner тоже использует ассоциативное сложение
  public static DurationSummary summaryParallel(List<Course> courses) {
    if (courses == null) throw new IllegalArgumentException("courses must not be null");
    return courses.parallelStream()
        .map(course -> new DurationSummary(course.durationHours(), 1))
        .reduce(DurationSummary.empty(), DurationSummary::plus, DurationSummary::plus);
  }

  // неправильная identity: добавляет лишний час и лишний курс
  public static DurationSummary summaryWithWrongIdentity(List<Course> courses) {
    if (courses == null) throw new IllegalArgumentException("courses must not be null");
    return courses.stream()
        .map(course -> new DurationSummary(course.durationHours(), 1))
        .reduce(new DurationSummary(10, 1), DurationSummary::plus);
  }

  // (a + b) + c == a + (b + c)
  public static boolean associative(DurationSummary a, DurationSummary b, DurationSummary c) {
    if (a == null || b == null || c == null) throw new IllegalArgumentException("summaries must not be null");
    return a.plus(b).plus(c).equals(a.plus(b.plus(c)));
  }
}
