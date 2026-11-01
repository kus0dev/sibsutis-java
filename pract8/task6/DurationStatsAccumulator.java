package task6;

import src.Course;

// изменяемый аккумулятор: поля только экземпляра, mutable static state нет
final class DurationStatsAccumulator {
  private long count;
  private long sum;
  private int min = Integer.MAX_VALUE;
  private int max = Integer.MIN_VALUE;

  void add(Course course) {
    if (course == null) throw new IllegalArgumentException("course must not be null");
    int hours = course.durationHours();
    count++;
    sum += hours;
    min = Math.min(min, hours);
    max = Math.max(max, hours);
  }

  // combiner корректен для разбиения: пустая часть не меняет результат
  void combine(DurationStatsAccumulator other) {
    if (other == null) throw new IllegalArgumentException("other must not be null");
    if (other.count == 0) return;
    count += other.count;
    sum += other.sum;
    min = Math.min(min, other.min);
    max = Math.max(max, other.max);
  }

  DurationStats finish() {
    if (count == 0) return DurationStats.empty();
    return new DurationStats(count, sum, min, max);
  }
}
