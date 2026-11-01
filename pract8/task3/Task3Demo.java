package task3;

import src.Course;
import src.Level;
import src.Module;
import java.util.List;

// демонстрация правильной и неправильной identity
public class Task3Demo {
  public static void main(String[] args) {
    List<Course> courses = List.of(
        new Course(1, "JAVA-101", "Java Basics", Level.BASIC, 8,
            List.of(Module.of("Syntax", "java"))),
        new Course(2, "DB-201", "Databases", Level.INTERMEDIATE, 12,
            List.of(Module.of("SQL", "sql"))),
        new Course(3, "WEB-301", "Web", Level.ADVANCED, 16,
            List.of(Module.of("HTTP", "http"))));

    System.out.println("sum hours: " + DurationReducer.totalHours(courses));

    DurationSummary correct = DurationReducer.summary(courses);
    DurationSummary parallel = DurationReducer.summaryParallel(courses);
    System.out.println("correct identity: " + correct);
    System.out.println("parallel: " + parallel);
    System.out.println("parallel equals sequential: " + correct.equals(parallel));

    // identity (10, 1) не нейтральна: результат сдвинут на лишний курс
    System.out.println("wrong identity: " + DurationReducer.summaryWithWrongIdentity(courses));

    DurationSummary a = new DurationSummary(4, 1);
    DurationSummary b = new DurationSummary(7, 2);
    DurationSummary c = new DurationSummary(9, 3);
    System.out.println("associative: " + DurationReducer.associative(a, b, c));
  }
}
