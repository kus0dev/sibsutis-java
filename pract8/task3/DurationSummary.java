package task3;

// value object: суммарные часы и количество курсов
public record DurationSummary(int totalHours, int courseCount) {
  public DurationSummary {
    if (totalHours < 0) throw new IllegalArgumentException("totalHours must be >= 0");
    if (courseCount < 0) throw new IllegalArgumentException("courseCount must be >= 0");
  }

  // нейтральный элемент для сложения
  public static DurationSummary empty() {
    return new DurationSummary(0, 0);
  }

  public DurationSummary plus(DurationSummary other) {
    if (other == null) throw new IllegalArgumentException("other must not be null");
    return new DurationSummary(
        Math.addExact(totalHours, other.totalHours),
        Math.addExact(courseCount, other.courseCount));
  }
}
