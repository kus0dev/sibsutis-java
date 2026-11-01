package task1;

import src.Course;
import src.Level;
import src.Module;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// демонстрация: разный входной порядок даёт одинаковый результат
public class Task1Demo {
  public static void main(String[] args) {
    List<Course> courses = List.of(
        new Course(4, "JAVA-101", "Java Basics", Level.BASIC, 12,
            List.of(Module.of("Syntax", "java", "types"))),
        new Course(2, "ALGO-201", "Algorithms", Level.BASIC, 12,
            List.of(Module.of("Graphs", "graph", "bfs"))),
        new Course(5, "JAVA-102", "Collections", Level.BASIC, 8,
            List.of(Module.of("Collections", "list", "map"))),
        new Course(3, "DB-301", "Databases", Level.INTERMEDIATE, 16,
            List.of(Module.of("SQL", "sql", "index"))),
        new Course(1, "WEB-401", "Web", Level.ADVANCED, 10,
            List.of(Module.of("HTTP", "http", "rest"))));

    List<Course> expected = CourseSorting.sorted(courses);

    // перемешиваем один и тот же набор данных с разными seed
    for (int seed = 1; seed <= 3; seed++) {
      List<Course> shuffled = new ArrayList<>(courses);
      Collections.shuffle(shuffled, new Random(seed));
      List<Course> actual = CourseSorting.sorted(shuffled);
      List<String> actualCodes = actual.stream().map(Course::code).toList();
      List<String> expectedCodes = expected.stream().map(Course::code).toList();
      System.out.println("seed " + seed + " same order: " + actualCodes.equals(expectedCodes));
    }

    // печатаем итоговый порядок: level, duration, code
    for (Course course : expected) {
      System.out.println(course.level() + " " + course.durationHours() + " " + course.code());
    }
  }
}
