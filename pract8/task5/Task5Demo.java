package task5;

import src.Course;
import src.Level;
import src.Module;
import java.util.List;

// демонстрация Optional как результата и fallback
public class Task5Demo {
  public static void main(String[] args) {
    List<Course> courses = List.of(
        new Course(1, "JAVA-101", "Java Basics", Level.BASIC, 8,
            List.of(Module.of("Syntax", "java"))),
        new Course(2, "DB-301", "Databases", Level.INTERMEDIATE, 16,
            List.of(Module.of("SQL", "sql"))),
        new Course(3, "WEB-401", "Web", Level.ADVANCED, 10,
            List.of(Module.of("HTTP", "http"))));

    // результат есть: map преобразует Course в title без get
    CourseAnalytics.longestTitle(courses).ifPresent(title -> System.out.println("longest: " + title));

    // результат пуст: срабатывает fallback
    System.out.println("fallback: " + CourseAnalytics.longestTitleOrDefault(List.of(), "no courses"));
    System.out.println("computed: " + CourseAnalytics.longestTitleOrComputed(List.of(), () -> "empty catalog"));

    // Optional не хранится в поле сущности: CourseAnalytics только возвращает его из метода
  }
}
