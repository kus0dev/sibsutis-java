package task4;

// сводка по уровню: количество, сумма и средняя длительность
public record LevelSummary(long count, long totalHours, double averageDuration) {
  public LevelSummary {
    if (count < 0) throw new IllegalArgumentException("count must be >= 0");
    if (totalHours < 0) throw new IllegalArgumentException("totalHours must be >= 0");
    if (averageDuration < 0) throw new IllegalArgumentException("averageDuration must be >= 0");
  }

  public static LevelSummary of(long count, long totalHours) {
    double average = count == 0 ? 0.0 : (double) totalHours / count;
    return new LevelSummary(count, totalHours, average);
  }
}
