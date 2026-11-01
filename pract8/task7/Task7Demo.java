package task7;

import src.Level;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// демонстрация: большой ленивый поток событий обрабатывается один раз
public class Task7Demo {
  public static void main(String[] args) {
    String[] codes = { "JAVA-101", "ALGO-201", "DB-301", "WEB-401", "ARCH-501" };
    Level[] levels = { Level.BASIC, Level.BASIC, Level.INTERMEDIATE, Level.INTERMEDIATE, Level.ADVANCED };
    int students = 2000;
    int courses = codes.length;

    // 2000 студентов * 5 курсов * 2 события, поток создаётся лениво
    Stream<EnrollmentEvent> base = IntStream.range(0, students * courses * 2)
        .mapToObj(i -> {
          int pair = i / 2;
          long studentId = pair / courses + 1;
          int course = pair % courses;
          if (i % 2 == 0) {
            return new EnrollmentEvent(studentId, codes[course], levels[course],
                EnrollmentEvent.Type.ENROLLED);
          }
          EnrollmentEvent.Type type = pair % 3 == 0
              ? EnrollmentEvent.Type.DROPPED
              : EnrollmentEvent.Type.COMPLETED;
          return new EnrollmentEvent(studentId, codes[course], levels[course], type);
        });

    // намеренные аномалии: дубли, завершение без записи, неверный level
    Stream<EnrollmentEvent> anomalies = Stream.<EnrollmentEvent>of(
        new EnrollmentEvent(1, "JAVA-101", Level.BASIC, EnrollmentEvent.Type.ENROLLED),
        new EnrollmentEvent(2, "ALGO-201", Level.BASIC, EnrollmentEvent.Type.COMPLETED),
        new EnrollmentEvent(2, "ALGO-201", Level.BASIC, EnrollmentEvent.Type.COMPLETED),
        new EnrollmentEvent(3, "DB-301", Level.ADVANCED, EnrollmentEvent.Type.ENROLLED),
        new EnrollmentEvent(9999, "JAVA-101", Level.BASIC, EnrollmentEvent.Type.COMPLETED),
        new EnrollmentEvent(9999, "JAVA-101", Level.BASIC, EnrollmentEvent.Type.DROPPED),
        null);

    CourseHubReport report = CourseHubAnalyzer.analyze(Stream.concat(base, anomalies), 10);

    System.out.println("unique students: " + report.uniqueStudents());
    System.out.println("completion rate:");
    report.completionRate().forEach((code, rate) ->
        System.out.printf(Locale.ROOT, "  %s %.3f%n", code, rate));

    System.out.println("top-3:");
    for (CourseHubReport.CourseCompletion item : report.top3()) {
      System.out.printf(Locale.ROOT, "  %s %.3f (%d/%d)%n",
          item.courseCode(), item.completionRate(), item.completed(), item.enrolled());
    }

    System.out.println("enrollments by level: " + report.enrollmentsByLevel());
    System.out.println("anomalies: " + report.totalAnomalies()
        + " (shown " + report.anomalies().size() + ")");
    for (CourseHubReport.Anomaly anomaly : report.anomalies()) {
      System.out.println("  #" + anomaly.eventNumber() + " " + anomaly.courseCode()
          + " student=" + anomaly.studentId() + ": " + anomaly.message());
    }
  }
}
