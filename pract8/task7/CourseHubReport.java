package task7;

import src.Level;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// неизменяемый результат однопроходного отчёта
public record CourseHubReport(
    int uniqueStudents,
    Map<String, Double> completionRate,
    List<CourseCompletion> top3,
    Map<Level, Long> enrollmentsByLevel,
    long totalAnomalies,
    List<Anomaly> anomalies) {

  public CourseHubReport {
    if (uniqueStudents < 0) throw new IllegalArgumentException("uniqueStudents must be >= 0");
    if (totalAnomalies < 0) throw new IllegalArgumentException("totalAnomalies must be >= 0");
    completionRate = Collections.unmodifiableMap(new TreeMap<>(completionRate));
    top3 = List.copyOf(top3);
    EnumMap<Level, Long> levels = new EnumMap<>(Level.class);
    levels.putAll(enrollmentsByLevel);
    enrollmentsByLevel = Collections.unmodifiableMap(levels);
    anomalies = List.copyOf(anomalies);
  }

  // строка top-3 и карты completion rate
  public record CourseCompletion(
      String courseCode,
      Level level,
      long enrolled,
      long completed,
      double completionRate) {
  }

  // аномалия с номером события в исходном потоке
  public record Anomaly(long eventNumber, String courseCode, long studentId, String message) {
  }
}
