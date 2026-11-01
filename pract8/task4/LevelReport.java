package task4;

import src.Course;
import src.Level;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

// groupingBy + teeing: count и sum собираются за один проход по элементам
public final class LevelReport {
  private LevelReport() {}

  public static Map<Level, LevelSummary> summarize(List<Course> courses) {
    if (courses == null) throw new IllegalArgumentException("courses must not be null");
    return courses.stream().collect(Collectors.groupingBy(
        Course::level,
        TreeMap::new,
        Collectors.teeing(
            Collectors.counting(),
            Collectors.summingLong(Course::durationHours),
            (count, total) -> LevelSummary.of(count, total))));
  }
}
