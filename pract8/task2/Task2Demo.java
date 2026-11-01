package task2;

import src.Course;
import src.Level;
import src.Module;
import java.util.List;
import java.util.Map;

// демонстрация flatMap: темы из модулей и обратный индекс
public class Task2Demo {
  public static void main(String[] args) {
    List<Course> courses = List.of(
        new Course(1, "JAVA-101", "Java Basics", Level.BASIC, 10, List.of(
            Module.of("Syntax", " Java ", "types"),
            Module.of("Collections", "List", "MAP"))),
        new Course(2, "DB-201", "Databases", Level.INTERMEDIATE, 14, List.of(
            Module.of("SQL", "sql", "types"),
            Module.of("Storage", "index", " B+ tree "))),
        new Course(3, "WEB-301", "Web", Level.ADVANCED, 12, List.of(
            Module.of("HTTP", "http", "index"))));

    // " Java " и "JAVA" схлопываются в одну нормализованную тему
    System.out.println("unique topics: " + TopicIndex.uniqueTopics(courses));

    for (Map.Entry<String, List<Course>> entry : TopicIndex.reverseIndex(courses).entrySet()) {
      List<String> codes = entry.getValue().stream().map(Course::code).toList();
      System.out.println(entry.getKey() + " -> " + codes);
    }
  }
}
