package task3;

// активная сессия студента
public record Session(String id, String student) {
  public Session {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id must not be blank");
    }
    if (student == null || student.isBlank()) {
      throw new IllegalArgumentException("student must not be blank");
    }
  }
}
