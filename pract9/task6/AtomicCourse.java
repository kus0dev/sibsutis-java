package task6;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// вариант 2: concurrent set для уникальности и CAS для резервирования мест
public class AtomicCourse {
  private final int capacity;
  private final Set<Long> students = ConcurrentHashMap.newKeySet();
  private final AtomicInteger reserved = new AtomicInteger();

  public AtomicCourse(int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
    this.capacity = capacity;
  }

  public boolean enroll(long studentId) {
    if (studentId <= 0) throw new IllegalArgumentException("studentId must be > 0");
    if (students.contains(studentId)) {
      return false;
    }

    // сначала резервируем место, не допуская переполнения capacity
    while (true) {
      int current = reserved.get();
      if (current >= capacity) {
        return false;
      }
      if (reserved.compareAndSet(current, current + 1)) {
        break;
      }
    }

    // если студент уже успел записаться, возвращаем резерв
    if (students.add(studentId)) {
      return true;
    }
    reserved.decrementAndGet();
    return false;
  }

  public int enrolled() {
    return students.size();
  }

  public int available() {
    return capacity - students.size();
  }
}
