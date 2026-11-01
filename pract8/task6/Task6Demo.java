package task6;

import src.Course;
import src.Level;
import src.Module;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

// сравнение собственного collector с summarizingInt
public class Task6Demo {
  public static void main(String[] args) {
    List<Course> courses = List.of(
        new Course(1, "JAVA-101", "Java Basics", Level.BASIC, 8,
            List.of(Module.of("Syntax", "java"))),
        new Course(2, "ALGO-201", "Algorithms", Level.BASIC, 10,
            List.of(Module.of("Graphs", "graph"))),
        new Course(3, "DB-301", "Databases", Level.INTERMEDIATE, 12,
            List.of(Module.of("SQL", "sql"))),
        new Course(4, "WEB-401", "Web", Level.ADVANCED, 14,
            List.of(Module.of("HTTP", "http"))),
        new Course(5, "ARCH-501", "Architecture", Level.ADVANCED, 16,
            List.of(Module.of("Patterns", "patterns"))),
        new Course(6, "SEC-601", "Security", Level.ADVANCED, 20,
            List.of(Module.of("Crypto", "crypto"))));

    DurationStats sequential = courses.stream().collect(new DurationStatsCollector());
    DurationStats parallel = courses.parallelStream().collect(new DurationStatsCollector());

    // ручная проверка combiner на двух частях
    DurationStatsAccumulator left = new DurationStatsAccumulator();
    DurationStatsAccumulator right = new DurationStatsAccumulator();
    courses.subList(0, 3).forEach(left::add);
    courses.subList(3, courses.size()).forEach(right::add);
    left.combine(right);

    IntSummaryStatistics builtIn = courses.stream()
        .collect(Collectors.summarizingInt(Course::durationHours));

    System.out.println("custom sequential: " + sequential + ", avg=" + sequential.average());
    System.out.println("custom parallel:   " + parallel + ", avg=" + parallel.average());
    System.out.println("combined parts:    " + left.finish());
    System.out.printf("built-in:          count=%d, sum=%d, min=%d, max=%d, avg=%.2f%n",
        builtIn.getCount(), builtIn.getSum(), builtIn.getMin(), builtIn.getMax(), builtIn.getAverage());

    DurationStats empty = List.<Course>of().stream().collect(new DurationStatsCollector());
    System.out.println("custom empty:      " + empty);
  }
}
