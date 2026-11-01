package task1;

import src.Course;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// сортировка курсов: все ничьи разрешаются явными ключами
public final class CourseSorting {
  // level -> duration -> code -> id (id уникален и закрывает последнюю ничью)
  private static final Comparator<Course> ORDER =
      Comparator.comparing(Course::level)
          .thenComparingInt(Course::durationHours)
          .thenComparing(Course::code)
          .thenComparingLong(Course::id);

  private CourseSorting() {}

  public static List<Course> sorted(List<Course> courses) {
    if (courses == null) throw new IllegalArgumentException("courses must not be null");
    List<Course> copy = new ArrayList<>(courses);
    copy.sort(ORDER);
    return List.copyOf(copy);
  }

  public static Comparator<Course> order() {
    return ORDER;
  }
}
