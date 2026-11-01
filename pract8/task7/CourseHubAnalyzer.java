package task7;

import src.Level;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

// однопроходный отчёт: события читаются один раз, внешних side effects нет
public final class CourseHubAnalyzer {
  private CourseHubAnalyzer() {}

  public static CourseHubReport analyze(Stream<EnrollmentEvent> events, int anomalyLimit) {
    if (events == null) throw new IllegalArgumentException("events must not be null");
    if (anomalyLimit < 0) throw new IllegalArgumentException("anomalyLimit must be >= 0");

    Accumulator accumulator = new Accumulator(anomalyLimit);
    events.forEachOrdered(accumulator::accept); // единственный проход по потоку
    return accumulator.toReport();
  }

  // состояние живёт только внутри одного вызова analyze
  private static final class Accumulator {
    private final int anomalyLimit;
    private final Set<Long> students = new HashSet<>();
    private final Map<String, CourseState> courses = new TreeMap<>();
    private final Map<Level, Long> enrollmentsByLevel = new EnumMap<>(Level.class);
    private final List<CourseHubReport.Anomaly> anomalies = new ArrayList<>();
    private long eventNumber;
    private long totalAnomalies;

    Accumulator(int anomalyLimit) {
      this.anomalyLimit = anomalyLimit;
    }

    void accept(EnrollmentEvent event) {
      eventNumber++;
      if (event == null) {
        anomaly(0, "", "null event");
        return;
      }

      CourseState state = courses.get(event.courseCode());
      switch (event.type()) {
        case ENROLLED -> {
          if (state == null) {
            state = new CourseState(event.courseCode(), event.level());
            courses.put(event.courseCode(), state);
          } else if (state.level != event.level()) {
            anomaly(event.studentId(), event.courseCode(), "level mismatch");
          }

          if (state.enrolled.add(event.studentId())) {
            students.add(event.studentId());
            enrollmentsByLevel.merge(state.level, 1L, Long::sum);
          } else {
            anomaly(event.studentId(), event.courseCode(), "duplicate enrollment");
          }
        }
        case COMPLETED -> {
          if (state == null || !state.enrolled.contains(event.studentId())) {
            anomaly(event.studentId(), event.courseCode(), "completion without enrollment");
          } else if (!state.completed.add(event.studentId())) {
            anomaly(event.studentId(), event.courseCode(), "duplicate completion");
          }
        }
        case DROPPED -> {
          if (state == null || !state.enrolled.contains(event.studentId())) {
            anomaly(event.studentId(), event.courseCode(), "drop without enrollment");
          } else if (state.completed.contains(event.studentId())) {
            anomaly(event.studentId(), event.courseCode(), "drop after completion");
          }
        }
      }
    }

    // список аномалий ограничен, но их общее число сохраняется
    private void anomaly(long studentId, String courseCode, String message) {
      totalAnomalies++;
      if (anomalies.size() < anomalyLimit) {
        anomalies.add(new CourseHubReport.Anomaly(eventNumber, courseCode, studentId, message));
      }
    }

    CourseHubReport toReport() {
      Map<String, Double> rates = new TreeMap<>();
      List<CourseHubReport.CourseCompletion> completions = new ArrayList<>();

      for (CourseState state : courses.values()) {
        long enrolled = state.enrolled.size();
        long completed = state.completed.size();
        double rate = enrolled == 0 ? 0.0 : (double) completed / enrolled;
        rates.put(state.code, rate);
        completions.add(new CourseHubReport.CourseCompletion(
            state.code, state.level, enrolled, completed, rate));
      }

      // top-3: сначала rate по убыванию, при равенстве - code по возрастанию
      List<CourseHubReport.CourseCompletion> top3 = completions.stream()
          .sorted(Comparator.comparingDouble(CourseHubReport.CourseCompletion::completionRate)
              .reversed()
              .thenComparing(CourseHubReport.CourseCompletion::courseCode))
          .limit(3)
          .toList();

      return new CourseHubReport(
          students.size(), rates, top3, enrollmentsByLevel, totalAnomalies, anomalies);
    }
  }

  private static final class CourseState {
    private final String code;
    private final Level level;
    private final Set<Long> enrolled = new HashSet<>();
    private final Set<Long> completed = new HashSet<>();

    CourseState(String code, Level level) {
      this.code = code;
      this.level = level;
    }
  }
}
