package task6;

// неизменяемый результат статистики
public record DurationStats(long count, long sum, int min, int max) {
  public DurationStats {
    if (count < 0) throw new IllegalArgumentException("count must be >= 0");
    if (sum < 0) throw new IllegalArgumentException("sum must be >= 0");
    if (count == 0 && (min != 0 || max != 0)) {
      throw new IllegalArgumentException("empty stats must have zero min/max");
    }
    if (count > 0 && (min < 0 || min > max)) {
      throw new IllegalArgumentException("min/max are inconsistent");
    }
  }

  public double average() {
    return count == 0 ? 0.0 : (double) sum / count;
  }

  public static DurationStats empty() {
    return new DurationStats(0, 0, 0, 0);
  }
}
