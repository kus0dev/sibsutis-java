# Практическая работа №8 — листинги

Полные исходные листинги всех заданий. В заголовке каждого листинга указан путь к файлу; в скобках — задание (задания), в которых этот файл используется.

## Содержание

- Общие файлы (пакет src)
- Задание 1. Сортировка и детерминизм
- Задание 2. `flatMap`
- Задание 3. `reduce` с правильной identity
- Задание 4. Collectors
- Задание 5. `Optional` как результат
- Задание 6. Собственный collector
- Задание 7. Однопроходный отчёт CourseHub

---

## Общие файлы (пакет src)

### src/Course.java (задания 1–6)

```java
package src;

import java.util.List;
import java.util.Locale;

// общая модель курса: используется в заданиях 1–6
public class Course {
  private final long id;
  private final String code;
  private final String title;
  private final Level level;
  private final int durationHours;
  private final List<Module> modules;

  public Course(long id, String code, String title, Level level, int durationHours, List<Module> modules) {
    if (id <= 0) throw new IllegalArgumentException("id must be > 0");
    if (code == null || code.isBlank()) throw new IllegalArgumentException("code must not be blank");
    if (title == null || title.isBlank()) throw new IllegalArgumentException("title must not be blank");
    if (level == null) throw new IllegalArgumentException("level must not be null");
    if (durationHours <= 0) throw new IllegalArgumentException("durationHours must be > 0");
    if (modules == null) throw new IllegalArgumentException("modules must not be null");
    this.id = id;
    this.code = code.trim().toUpperCase(Locale.ROOT);
    this.title = title.trim();
    this.level = level;
    this.durationHours = durationHours;
    this.modules = List.copyOf(modules);
  }

  public long id() { return id; }
  public String code() { return code; }
  public String title() { return title; }
  public Level level() { return level; }
  public int durationHours() { return durationHours; }
  public List<Module> modules() { return modules; }

  @Override
  public String toString() {
    return code + " (" + level + ", " + durationHours + "h)";
  }
}
```

### src/Level.java (задания 1–7)

```java
package src;

// уровень курса: естественный порядок BASIC -> INTERMEDIATE -> ADVANCED
public enum Level {
  BASIC,
  INTERMEDIATE,
  ADVANCED
}
```

### src/Module.java (задания 1–6)

```java
package src;

import java.util.List;

// модуль курса: название и темы для обратного индекса
public record Module(String title, List<String> topics) {
  public Module {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title must not be blank");
    }
    if (topics == null) {
      throw new IllegalArgumentException("topics must not be null");
    }
    topics = List.copyOf(topics);
  }

  // удобная фабрика для демонстрационных данных
  public static Module of(String title, String... topics) {
    return new Module(title, List.of(topics));
  }
}
```

## Задание 1. Сортировка и детерминизм

### task1/CourseSorting.java (задание 1)

```java
package task1;

import src.Course;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// сортировка курсов: все ничьи разрешаются явными ключами
public final class CourseSorting {
  // level -> duration -> code -> id (id уникален и закрывает последнюю ничью)
  private static final Comparator<Course> ORDER =
      Comparator.comparing(Course::level)
          .thenComparingInt(Course::durationHours)
          .thenComparing(Course::code)
          .thenComparingLong(Course::id);

  private CourseSorting() {}

  public static List<Course> sorted(List<Course> courses) {
    if (courses == null) throw new IllegalArgumentException("courses must not be null");
    List<Course> copy = new ArrayList<>(courses);
    copy.sort(ORDER);
    return List.copyOf(copy);
  }

  public static Comparator<Course> order() {
    return ORDER;
  }
}
```

### task1/Task1Demo.java (задание 1)

```java
package task1;

import src.Course;
import src.Level;
import src.Module;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// демонстрация: разный входной порядок даёт одинаковый результат
public class Task1Demo {
  public static void main(String[] args) {
    List<Course> courses = List.of(
        new Course(4, "JAVA-101", "Java Basics", Level.BASIC, 12,
            List.of(Module.of("Syntax", "java", "types"))),
        new Course(2, "ALGO-201", "Algorithms", Level.BASIC, 12,
            List.of(Module.of("Graphs", "graph", "bfs"))),
        new Course(5, "JAVA-102", "Collections", Level.BASIC, 8,
            List.of(Module.of("Collections", "list", "map"))),
        new Course(3, "DB-301", "Databases", Level.INTERMEDIATE, 16,
            List.of(Module.of("SQL", "sql", "index"))),
        new Course(1, "WEB-401", "Web", Level.ADVANCED, 10,
            List.of(Module.of("HTTP", "http", "rest"))));

    List<Course> expected = CourseSorting.sorted(courses);

    // перемешиваем один и тот же набор данных с разными seed
    for (int seed = 1; seed <= 3; seed++) {
      List<Course> shuffled = new ArrayList<>(courses);
      Collections.shuffle(shuffled, new Random(seed));
      List<Course> actual = CourseSorting.sorted(shuffled);
      List<String> actualCodes = actual.stream().map(Course::code).toList();
      List<String> expectedCodes = expected.stream().map(Course::code).toList();
      System.out.println("seed " + seed + " same order: " + actualCodes.equals(expectedCodes));
    }

    // печатаем итоговый порядок: level, duration, code
    for (Course course : expected) {
      System.out.println(course.level() + " " + course.durationHours() + " " + course.code());
    }
  }
}
```

## Задание 2. `flatMap`

### task2/TopicIndex.java (задание 2)

```java
package task2;

import src.Course;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

// уникальные темы и обратный индекс topic -> courses через flatMap
public final class TopicIndex {
  private TopicIndex() {}

  // strip + схлопывание пробелов + lower case по Locale.ROOT
  public static String normalize(String topic) {
    if (topic == null) return "";
    return topic.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
  }

  // Course -> Module -> topic, без ручного создания вложенного списка
  public static Set<String> uniqueTopics(List<Course> courses) {
    if (courses == null) throw new IllegalArgumentException("courses must not be null");
    return courses.stream()
        .flatMap(course -> course.modules().stream())
        .flatMap(module -> module.topics().stream())
        .map(TopicIndex::normalize)
        .filter(topic -> !topic.isEmpty())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  // обратный индекс; TreeMap и сортировка курсов делают результат детерминированным
  public static Map<String, List<Course>> reverseIndex(List<Course> courses) {
    if (courses == null) throw new IllegalArgumentException("courses must not be null");
    return courses.stream()
        .flatMap(course -> course.modules().stream()
            .flatMap(module -> module.topics().stream())
            .map(TopicIndex::normalize)
            .filter(topic -> !topic.isEmpty())
            .distinct()
            .map(topic -> Map.entry(topic, course)))
        .collect(Collectors.groupingBy(
            Map.Entry::getKey,
            TreeMap::new,
            Collectors.mapping(
                Map.Entry::getValue,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> list.stream()
                        .sorted(Comparator.comparing(Course::code))
                        .toList()))));
  }
}
```

### task2/Task2Demo.java (задание 2)

```java
package task2;

import src.Course;
import src.Level;
import src.Module;
import java.util.List;
import java.util.Map;

// демонстрация flatMap: темы из модулей и обратный индекс
public class Task2Demo {
  public static void main(String[] args) {
    List<Course> courses = List.of(
        new Course(1, "JAVA-101", "Java Basics", Level.BASIC, 10, List.of(
            Module.of("Syntax", " Java ", "types"),
            Module.of("Collections", "List", "MAP"))),
        new Course(2, "DB-201", "Databases", Level.INTERMEDIATE, 14, List.of(
            Module.of("SQL", "sql", "types"),
            Module.of("Storage", "index", " B+ tree "))),
        new Course(3, "WEB-301", "Web", Level.ADVANCED, 12, List.of(
            Module.of("HTTP", "http", "index"))));

    // " Java " и "JAVA" схлопываются в одну нормализованную тему
    System.out.println("unique topics: " + TopicIndex.uniqueTopics(courses));

    for (Map.Entry<String, List<Course>> entry : TopicIndex.reverseIndex(courses).entrySet()) {
      List<String> codes = entry.getValue().stream().map(Course::code).toList();
      System.out.println(entry.getKey() + " -> " + codes);
    }
  }
}
```

## Задание 3. `reduce` с правильной identity

### task3/DurationSummary.java (задание 3)

```java
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
```

### task3/DurationReducer.java (задание 3)

```java
package task3;

import src.Course;
import java.util.List;

// reduce: identity должна быть нейтральной, accumulator - ассоциативным
public final class DurationReducer {
  private DurationReducer() {}

  public static int totalHours(List<Course> courses) {
    if (courses == null) throw new IllegalArgumentException("courses must not be null");
    return courses.stream()
        .mapToInt(Course::durationHours)
        .reduce(0, Integer::sum);
  }

  // правильная identity: 0 часов и 0 курсов
  public static DurationSummary summary(List<Course> courses) {
    if (courses == null) throw new IllegalArgumentException("courses must not be null");
    return courses.stream()
        .map(course -> new DurationSummary(course.durationHours(), 1))
        .reduce(DurationSummary.empty(), DurationSummary::plus);
  }

  // та же операция в parallel: combiner тоже использует ассоциативное сложение
  public static DurationSummary summaryParallel(List<Course> courses) {
    if (courses == null) throw new IllegalArgumentException("courses must not be null");
    return courses.parallelStream()
        .map(course -> new DurationSummary(course.durationHours(), 1))
        .reduce(DurationSummary.empty(), DurationSummary::plus, DurationSummary::plus);
  }

  // неправильная identity: добавляет лишний час и лишний курс
  public static DurationSummary summaryWithWrongIdentity(List<Course> courses) {
    if (courses == null) throw new IllegalArgumentException("courses must not be null");
    return courses.stream()
        .map(course -> new DurationSummary(course.durationHours(), 1))
        .reduce(new DurationSummary(10, 1), DurationSummary::plus);
  }

  // (a + b) + c == a + (b + c)
  public static boolean associative(DurationSummary a, DurationSummary b, DurationSummary c) {
    if (a == null || b == null || c == null) throw new IllegalArgumentException("summaries must not be null");
    return a.plus(b).plus(c).equals(a.plus(b.plus(c)));
  }
}
```

### task3/Task3Demo.java (задание 3)

```java
package task3;

import src.Course;
import src.Level;
import src.Module;
import java.util.List;

// демонстрация правильной и неправильной identity
public class Task3Demo {
  public static void main(String[] args) {
    List<Course> courses = List.of(
        new Course(1, "JAVA-101", "Java Basics", Level.BASIC, 8,
            List.of(Module.of("Syntax", "java"))),
        new Course(2, "DB-201", "Databases", Level.INTERMEDIATE, 12,
            List.of(Module.of("SQL", "sql"))),
        new Course(3, "WEB-301", "Web", Level.ADVANCED, 16,
            List.of(Module.of("HTTP", "http"))));

    System.out.println("sum hours: " + DurationReducer.totalHours(courses));

    DurationSummary correct = DurationReducer.summary(courses);
    DurationSummary parallel = DurationReducer.summaryParallel(courses);
    System.out.println("correct identity: " + correct);
    System.out.println("parallel: " + parallel);
    System.out.println("parallel equals sequential: " + correct.equals(parallel));

    // identity (10, 1) не нейтральна: результат сдвинут на лишний курс
    System.out.println("wrong identity: " + DurationReducer.summaryWithWrongIdentity(courses));

    DurationSummary a = new DurationSummary(4, 1);
    DurationSummary b = new DurationSummary(7, 2);
    DurationSummary c = new DurationSummary(9, 3);
    System.out.println("associative: " + DurationReducer.associative(a, b, c));
  }
}
```

## Задание 4. Collectors

### task4/LevelSummary.java (задание 4)

```java
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
```

### task4/LevelReport.java (задание 4)

```java
package task4;

import src.Course;
import src.Level;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

// groupingBy + teeing: count и sum собираются за один проход по элементам
public final class LevelReport {
  private LevelReport() {}

  public static Map<Level, LevelSummary> summarize(List<Course> courses) {
    if (courses == null) throw new IllegalArgumentException("courses must not be null");
    return courses.stream().collect(Collectors.groupingBy(
        Course::level,
        TreeMap::new,
        Collectors.teeing(
            Collectors.counting(),
            Collectors.summingLong(Course::durationHours),
            (count, total) -> LevelSummary.of(count, total))));
  }
}
```

### task4/Task4Demo.java (задание 4)

```java
package task4;

import src.Course;
import src.Level;
import src.Module;
import java.util.List;
import java.util.Map;

// демонстрация группировки курсов по level
public class Task4Demo {
  public static void main(String[] args) {
    List<Course> courses = List.of(
        new Course(1, "JAVA-101", "Java Basics", Level.BASIC, 8,
            List.of(Module.of("Syntax", "java"))),
        new Course(2, "ALGO-201", "Algorithms", Level.BASIC, 12,
            List.of(Module.of("Graphs", "graph"))),
        new Course(3, "DB-301", "Databases", Level.INTERMEDIATE, 16,
            List.of(Module.of("SQL", "sql"))),
        new Course(4, "WEB-401", "Web", Level.ADVANCED, 10,
            List.of(Module.of("HTTP", "http"))),
        new Course(5, "ARCH-501", "Architecture", Level.ADVANCED, 20,
            List.of(Module.of("Patterns", "patterns"))));

    // не делаем три отдельных прохода: groupingBy и teeing работают за один обход
    Map<Level, LevelSummary> report = LevelReport.summarize(courses);
    for (Map.Entry<Level, LevelSummary> entry : report.entrySet()) {
      LevelSummary summary = entry.getValue();
      System.out.printf("%s: count=%d, total=%d, avg=%.2f%n",
          entry.getKey(), summary.count(), summary.totalHours(), summary.averageDuration());
    }
  }
}
```

## Задание 5. `Optional` как результат

### task5/CourseAnalytics.java (задание 5)

```java
package task5;

import src.Course;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

// поиск самого длинного курса: Optional только как результат метода
public final class CourseAnalytics {
  // при равной длительности выбираем меньший code для детерминизма
  private static final Comparator<Course> LONGEST =
      Comparator.comparingInt(Course::durationHours)
          .thenComparing(Course::code);

  private CourseAnalytics() {}

  public static Optional<Course> longest(List<Course> courses) {
    if (courses == null) throw new IllegalArgumentException("courses must not be null");
    return courses.stream().max(LONGEST);
  }

  // преобразование без Optional.get
  public static Optional<String> longestTitle(List<Course> courses) {
    return longest(courses).map(Course::title);
  }

  // fallback, если курсов нет
  public static String longestTitleOrDefault(List<Course> courses, String fallback) {
    if (fallback == null) throw new IllegalArgumentException("fallback must not be null");
    return longestTitle(courses).orElse(fallback);
  }

  // ленивый fallback: вычисляется только при пустом Optional
  public static String longestTitleOrComputed(List<Course> courses, Supplier<String> fallback) {
    if (fallback == null) throw new IllegalArgumentException("fallback must not be null");
    return longestTitle(courses).orElseGet(fallback);
  }
}
```

### task5/Task5Demo.java (задание 5)

```java
package task5;

import src.Course;
import src.Level;
import src.Module;
import java.util.List;

// демонстрация Optional как результата и fallback
public class Task5Demo {
  public static void main(String[] args) {
    List<Course> courses = List.of(
        new Course(1, "JAVA-101", "Java Basics", Level.BASIC, 8,
            List.of(Module.of("Syntax", "java"))),
        new Course(2, "DB-301", "Databases", Level.INTERMEDIATE, 16,
            List.of(Module.of("SQL", "sql"))),
        new Course(3, "WEB-401", "Web", Level.ADVANCED, 10,
            List.of(Module.of("HTTP", "http"))));

    // результат есть: map преобразует Course в title без get
    CourseAnalytics.longestTitle(courses).ifPresent(title -> System.out.println("longest: " + title));

    // результат пуст: срабатывает fallback
    System.out.println("fallback: " + CourseAnalytics.longestTitleOrDefault(List.of(), "no courses"));
    System.out.println("computed: " + CourseAnalytics.longestTitleOrComputed(List.of(), () -> "empty catalog"));

    // Optional не хранится в поле сущности: CourseAnalytics только возвращает его из метода
  }
}
```

## Задание 6. Собственный collector

### task6/DurationStats.java (задание 6)

```java
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
```

### task6/DurationStatsAccumulator.java (задание 6)

```java
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
  DurationStatsAccumulator combine(DurationStatsAccumulator other) {
    if (other == null) throw new IllegalArgumentException("other must not be null");
    if (other.count == 0) return this;
    count += other.count;
    sum += other.sum;
    min = Math.min(min, other.min);
    max = Math.max(max, other.max);
    return this;
  }

  DurationStats finish() {
    if (count == 0) return DurationStats.empty();
    return new DurationStats(count, sum, min, max);
  }
}
```

### task6/DurationStatsCollector.java (задание 6)

```java
package task6;

import src.Course;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

// собственный collector: supplier -> accumulator -> combiner -> finisher
public final class DurationStatsCollector
    implements Collector<Course, DurationStatsAccumulator, DurationStats> {

  @Override
  public Supplier<DurationStatsAccumulator> supplier() {
    return DurationStatsAccumulator::new;
  }

  @Override
  public BiConsumer<DurationStatsAccumulator, Course> accumulator() {
    return DurationStatsAccumulator::add;
  }

  @Override
  public BinaryOperator<DurationStatsAccumulator> combiner() {
    return DurationStatsAccumulator::combine;
  }

  @Override
  public Function<DurationStatsAccumulator, DurationStats> finisher() {
    return DurationStatsAccumulator::finish;
  }

  @Override
  public Set<Characteristics> characteristics() {
    return Set.of(Characteristics.UNORDERED);
  }
}
```

### task6/Task6Demo.java (задание 6)

```java
package task6;

import src.Course;
import src.Level;
import src.Module;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

// сравнение собственного collector с summarizingInt
public class Task6Demo {
  public static void main(String[] args) {
    List<Course> courses = List.of(
        new Course(1, "JAVA-101", "Java Basics", Level.BASIC, 8,
            List.of(Module.of("Syntax", "java"))),
        new Course(2, "ALGO-201", "Algorithms", Level.BASIC, 10,
            List.of(Module.of("Graphs", "graph"))),
        new Course(3, "DB-301", "Databases", Level.INTERMEDIATE, 12,
            List.of(Module.of("SQL", "sql"))),
        new Course(4, "WEB-401", "Web", Level.ADVANCED, 14,
            List.of(Module.of("HTTP", "http"))),
        new Course(5, "ARCH-501", "Architecture", Level.ADVANCED, 16,
            List.of(Module.of("Patterns", "patterns"))),
        new Course(6, "SEC-601", "Security", Level.ADVANCED, 20,
            List.of(Module.of("Crypto", "crypto"))));

    DurationStats sequential = courses.stream().collect(new DurationStatsCollector());
    DurationStats parallel = courses.parallelStream().collect(new DurationStatsCollector());

    // ручная проверка combiner на двух частях
    DurationStatsAccumulator left = new DurationStatsAccumulator();
    DurationStatsAccumulator right = new DurationStatsAccumulator();
    courses.subList(0, 3).forEach(left::add);
    courses.subList(3, courses.size()).forEach(right::add);
    left.combine(right);

    IntSummaryStatistics builtIn = courses.stream()
        .collect(Collectors.summarizingInt(Course::durationHours));

    System.out.println("custom sequential: " + sequential + ", avg=" + sequential.average());
    System.out.println("custom parallel:   " + parallel + ", avg=" + parallel.average());
    System.out.println("combined parts:    " + left.finish());
    System.out.printf("built-in:          count=%d, sum=%d, min=%d, max=%d, avg=%.2f%n",
        builtIn.getCount(), builtIn.getSum(), builtIn.getMin(), builtIn.getMax(), builtIn.getAverage());

    DurationStats empty = List.<Course>of().stream().collect(new DurationStatsCollector());
    System.out.println("custom empty:      " + empty);
  }
}
```

## Задание 7. Однопроходный отчёт CourseHub

### task7/EnrollmentEvent.java (задание 7)

```java
package task7;

import src.Level;
import java.util.Locale;

// событие enrollment: студент, курс, уровень и тип
public record EnrollmentEvent(long studentId, String courseCode, Level level, Type type) {
  public enum Type { ENROLLED, COMPLETED, DROPPED }

  public EnrollmentEvent {
    if (studentId <= 0) throw new IllegalArgumentException("studentId must be > 0");
    if (courseCode == null || courseCode.isBlank()) {
      throw new IllegalArgumentException("courseCode must not be blank");
    }
    if (level == null) throw new IllegalArgumentException("level must not be null");
    if (type == null) throw new IllegalArgumentException("type must not be null");
    courseCode = courseCode.trim().toUpperCase(Locale.ROOT);
  }
}
```

### task7/CourseHubReport.java (задание 7)

```java
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
```

### task7/CourseHubAnalyzer.java (задание 7)

```java
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
```

### task7/Task7Demo.java (задание 7)

```java
package task7;

import src.Level;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// демонстрация: большой ленивый поток событий обрабатывается один раз
public class Task7Demo {
  public static void main(String[] args) {
    String[] codes = { "JAVA-101", "ALGO-201", "DB-301", "WEB-401", "ARCH-501" };
    Level[] levels = { Level.BASIC, Level.BASIC, Level.INTERMEDIATE, Level.INTERMEDIATE, Level.ADVANCED };
    int students = 2000;
    int courses = codes.length;

    // 2000 студентов * 5 курсов * 2 события, поток создаётся лениво
    Stream<EnrollmentEvent> base = IntStream.range(0, students * courses * 2)
        .mapToObj(i -> {
          int pair = i / 2;
          long studentId = pair / courses + 1;
          int course = pair % courses;
          if (i % 2 == 0) {
            return new EnrollmentEvent(studentId, codes[course], levels[course],
                EnrollmentEvent.Type.ENROLLED);
          }
          EnrollmentEvent.Type type = pair % 3 == 0
              ? EnrollmentEvent.Type.DROPPED
              : EnrollmentEvent.Type.COMPLETED;
          return new EnrollmentEvent(studentId, codes[course], levels[course], type);
        });

    // намеренные аномалии: дубли, завершение без записи, неверный level
    Stream<EnrollmentEvent> anomalies = Stream.<EnrollmentEvent>of(
        new EnrollmentEvent(1, "JAVA-101", Level.BASIC, EnrollmentEvent.Type.ENROLLED),
        new EnrollmentEvent(2, "ALGO-201", Level.BASIC, EnrollmentEvent.Type.COMPLETED),
        new EnrollmentEvent(2, "ALGO-201", Level.BASIC, EnrollmentEvent.Type.COMPLETED),
        new EnrollmentEvent(3, "DB-301", Level.ADVANCED, EnrollmentEvent.Type.ENROLLED),
        new EnrollmentEvent(9999, "JAVA-101", Level.BASIC, EnrollmentEvent.Type.COMPLETED),
        new EnrollmentEvent(9999, "JAVA-101", Level.BASIC, EnrollmentEvent.Type.DROPPED),
        null);

    CourseHubReport report = CourseHubAnalyzer.analyze(Stream.concat(base, anomalies), 10);

    System.out.println("unique students: " + report.uniqueStudents());
    System.out.println("completion rate:");
    report.completionRate().forEach((code, rate) ->
        System.out.printf(Locale.ROOT, "  %s %.3f%n", code, rate));

    System.out.println("top-3:");
    for (CourseHubReport.CourseCompletion item : report.top3()) {
      System.out.printf(Locale.ROOT, "  %s %.3f (%d/%d)%n",
          item.courseCode(), item.completionRate(), item.completed(), item.enrolled());
    }

    System.out.println("enrollments by level: " + report.enrollmentsByLevel());
    System.out.println("anomalies: " + report.totalAnomalies()
        + " (shown " + report.anomalies().size() + ")");
    for (CourseHubReport.Anomaly anomaly : report.anomalies()) {
      System.out.println("  #" + anomaly.eventNumber() + " " + anomaly.courseCode()
          + " student=" + anomaly.studentId() + ": " + anomaly.message());
    }
  }
}
```
