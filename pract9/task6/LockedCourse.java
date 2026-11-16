package task6;

import java.util.HashSet;
import java.util.Set;

// вариант 1: один coarse-grained lock защищает и места, и список студентов
public class LockedCourse {
  private final int capacity;
  private final Set<Long> students = new HashSet<>();

  public LockedCourse(int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
    this.capacity = capacity;
  }

  public synchronized boolean enroll(long studentId) {
    if (studentId <= 0) throw new IllegalArgumentException("studentId must be > 0");
    if (students.size() >= capacity || students.contains(studentId)) {
      return false;
    }
    students.add(studentId);
    return true;
  }

  public synchronized int enrolled() {
    return students.size();
  }

  public synchronized int available() {
    return capacity - students.size();
  }
}
