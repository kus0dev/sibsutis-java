package task4;

import src.Course;
import src.Level;
import src.Module;
import java.util.List;
import java.util.Map;

// демонстрация группировки курсов по level
public class Task4Demo {
  public static void main(String[] args) {
    List<Course> courses = List.of(
        new Course(1, "JAVA-101", "Java Basics", Level.BASIC, 8,
            List.of(Module.of("Syntax", "java"))),
        new Course(2, "ALGO-201", "Algorithms", Level.BASIC, 12,
            List.of(Module.of("Graphs", "graph"))),
        new Course(3, "DB-301", "Databases", Level.INTERMEDIATE, 16,
            List.of(Module.of("SQL", "sql"))),
        new Course(4, "WEB-401", "Web", Level.ADVANCED, 10,
            List.of(Module.of("HTTP", "http"))),
        new Course(5, "ARCH-501", "Architecture", Level.ADVANCED, 20,
            List.of(Module.of("Patterns", "patterns"))));

    // не делаем три отдельных прохода: groupingBy и teeing работают за один обход
    Map<Level, LevelSummary> report = LevelReport.summarize(courses);
    for (Map.Entry<Level, LevelSummary> entry : report.entrySet()) {
      LevelSummary summary = entry.getValue();
      System.out.printf("%s: count=%d, total=%d, avg=%.2f%n",
          entry.getKey(), summary.count(), summary.totalHours(), summary.averageDuration());
    }
  }
}
