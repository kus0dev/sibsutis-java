package task4;

// заявка в очереди; отрицательный id — служебный сигнал остановки
public record Request(long id, String courseCode) {
  public Request {
    if (courseCode == null || courseCode.isBlank()) {
      throw new IllegalArgumentException("courseCode must not be blank");
    }
  }

  public static Request stop() {
    return new Request(-1, "STOP");
  }

  public boolean isStop() {
    return id < 0;
  }
}
