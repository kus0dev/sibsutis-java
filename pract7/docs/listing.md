# Практическая работа №7 — листинги

Полные исходные листинги всех заданий. В заголовке каждого листинга указан путь к файлу; в скобках — задание (задания), в которых этот файл используется.

## Содержание

- Общие файлы (пакет src)
- Задание 1. `Files.readString/writeString`
- Задание 2. `Reader`/`Writer`
- Задание 3. Try-with-resources
- Задание 4. Ленивые строки
- Задание 5. Бинарный формат
- Задание 6. Временный файл и move
- Задание 7. Частично повреждённый импорт
- Задание 8. Потоковый импорт большого файла
- Задание 9. Версионируемый формат

---

## Общие файлы (пакет src)

### src/Course.java (задания 1-9)
```java
package src;

// общая модель курса: используется в заданиях практики 7
public class Course {
  public enum Status { OPEN, CLOSED }

  private final long id;
  private final String title;
  private int durationHours;

  private Status status = Status.OPEN;
  private int capacity = 30;
  private boolean hasPrerequisite;
  private final java.util.LinkedHashSet<Long> enrolled = new java.util.LinkedHashSet<>();

  public Course(long id, String title, int durationHours) {
    if (id <= 0) throw new IllegalArgumentException("id must be > 0");
    if (title == null || title.isBlank()) throw new IllegalArgumentException("title must not be blank");
    if (durationHours <= 0) throw new IllegalArgumentException("durationHours must be > 0");
    this.id = id;
    this.title = title.trim();
    this.durationHours = durationHours;
  }

  public long id() { return id; }
  public String title() { return title; }
  public int durationHours() { return durationHours; }

  public void setStatus(Status status) { this.status = status; }
  public Status status() { return status; }
  public void setCapacity(int capacity) { this.capacity = capacity; }
  public int capacity() { return capacity; }
  public void setHasPrerequisite(boolean has) { this.hasPrerequisite = has; }
  public boolean hasPrerequisite() { return hasPrerequisite; }

  public java.util.Set<Long> enrolledStudents() {
    return java.util.Collections.unmodifiableSet(enrolled);
  }

  public void addEnrolled(long studentId) { enrolled.add(studentId); }
  public int enrolledCount() { return enrolled.size(); }
  public int seatsLeft() { return capacity - enrolledCount(); }
}
```

### src/Student.java (задания 1-9)
```java
package src;

public class Student {
  private final long id;
  private final String name;
  private final int age;
  private final boolean hasPrerequisite;

  public Student(long id, String name, int age, boolean hasPrerequisite) {
    if (id <= 0) throw new IllegalArgumentException("id must be > 0");
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
    if (age <= 0) throw new IllegalArgumentException("age must be > 0");
    this.id = id;
    this.name = name.trim();
    this.age = age;
    this.hasPrerequisite = hasPrerequisite;
  }

  public long id() { return id; }
  public String name() { return name; }
  public int age() { return age; }
  public boolean hasPrerequisite() { return hasPrerequisite; }
}
```

---

## Задание 1. `Files.readString/writeString`

### task1/CourseCatalog.java (задание 1)
```java
package task1;

import src.Course;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// каталог курсов: хранит список курсов
public class CourseCatalog {
  private final List<Course> courses = new ArrayList<>();

  public void add(Course course) {
    if (course == null) throw new IllegalArgumentException("course must not be null");
    courses.add(course);
  }

  public List<Course> courses() {
    return Collections.unmodifiableList(courses);
  }

  // сериализация в простой текстовый формат
  public String toText() {
    StringBuilder sb = new StringBuilder();
    for (Course c : courses) {
      sb.append(c.id()).append(';')
        .append(c.title().replace(";", ",")).append(';')
        .append(c.durationHours()).append(';')
        .append(c.status()).append('\n');
    }
    return sb.toString();
  }

  public static CourseCatalog fromText(String text) {
    if (text == null) throw new IllegalArgumentException("text must not be null");
    CourseCatalog catalog = new CourseCatalog();
    String[] lines = text.split("\\R");
    for (String line : lines) {
      if (line.isBlank()) continue;
      String[] parts = line.split(";");
      if (parts.length < 4) continue;
      long id = Long.parseLong(parts[0]);
      String title = parts[1].replace(",", ";");
      int hours = Integer.parseInt(parts[2]);
      Course course = new Course(id, title, hours);
      try {
        Course.Status status = Course.Status.valueOf(parts[3]);
        course.setStatus(status);
      } catch (Exception ignored) {
        // при ошибке парсинга статуса оставляем значение по умолчанию
      }
      catalog.add(course);
    }
    return catalog;
  }

  // сохраняет каталог в UTF-8
  public static void save(Path path, CourseCatalog catalog) throws IOException {
    if (path == null) throw new IllegalArgumentException("path must not be null");
    if (catalog == null) throw new IllegalArgumentException("catalog must not be null");
    // не перезаписываем, если путь указывает на каталог
    if (Files.exists(path) && Files.isDirectory(path)) {
      throw new IOException("path is a directory, not a file: " + path);
    }
    String content = catalog.toText();
    Files.writeString(path, content, StandardCharsets.UTF_8);
  }

  // читает каталог из UTF-8 с обработкой ошибок
  public static CourseCatalog load(Path path) throws IOException {
    if (path == null) throw new IllegalArgumentException("path must not be null");
    if (!Files.exists(path)) {
      throw new IOException("file does not exist: " + path);
    }
    if (Files.isDirectory(path)) {
      throw new IOException("path is a directory, not a file: " + path);
    }
    String content = Files.readString(path, StandardCharsets.UTF_8);
    return fromText(content);
  }
}
```

### task1/Task1Demo.java (задание 1)
```java
package task1;

import src.Course;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// демонстрация: чтение/запись UTF-8, обработка отсутствия файла и каталога
public class Task1Demo {
  public static void main(String[] args) throws IOException {
    Path dir = Path.of("pract7/task1");
    Files.createDirectories(dir);
    Path file = dir.resolve("catalog.txt");

    // создаём и сохраняем каталог
    CourseCatalog catalog = new CourseCatalog();
    catalog.add(new Course(1, "Java", 10));
    catalog.add(new Course(2, "Algorithms", 16));
    CourseCatalog.save(file, catalog);

    // читаем обратно
    CourseCatalog loaded = CourseCatalog.load(file);
    System.out.println("loaded count: " + loaded.courses().size());
    for (Course c : loaded.courses()) {
      System.out.println(c.id() + " " + c.title());
    }

    // обработка отсутствия файла
    try {
      CourseCatalog.load(Path.of("pract7/task1/missing.txt"));
    } catch (IOException e) {
      System.out.println("missing: " + e.getMessage());
    }

    // обработка каталога вместо файла
    try {
      CourseCatalog.load(dir);
    } catch (IOException e) {
      System.out.println("dir: " + e.getMessage());
    }
  }
}
```

---

## Задание 2. `Reader`/`Writer`

### task2/TextProcessor.java (задание 2)
```java
package task2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

// обработка текста через Reader/Writer; граница bytes -> chars через InputStreamReader
public class TextProcessor {
  // считывает строки через BufferedReader
  public static int countLines(BufferedReader reader) throws IOException {
    if (reader == null) throw new IllegalArgumentException("reader must not be null");
    int count = 0;
    String line;
    while ((line = reader.readLine()) != null) {
      count++;
    }
    return count;
  }

  // считывает символы через Reader
  public static int countChars(BufferedReader reader) throws IOException {
    if (reader == null) throw new IllegalArgumentException("reader must not be null");
    int count = 0;
    int ch;
    while ((ch = reader.read()) != -1) {
      count++;
    }
    return count;
  }

  // копирует текст с использованием BufferedReader/BufferedWriter
  public static void copyText(InputStream in, OutputStream out) throws IOException {
    if (in == null || out == null) throw new IllegalArgumentException("streams must not be null");
    // bytes -> chars происходит в InputStreamReader по заданной кодировке
    try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
         BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
      String line;
      while ((line = br.readLine()) != null) {
        bw.write(line);
        bw.newLine();
      }
    }
  }

  // чтение по символам через BufferedReader
  public static String readAllChars(BufferedReader reader) throws IOException {
    if (reader == null) throw new IllegalArgumentException("reader must not be null");
    StringBuilder sb = new StringBuilder();
    int ch;
    while ((ch = reader.read()) != -1) {
      sb.append((char) ch);
    }
    return sb.toString();
  }
}
```

### task2/Task2Demo.java (задание 2)
```java
package task2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

// демонстрация Reader/Writer, считывание строк и символов, пояснение bytes -> chars
public class Task2Demo {
  public static void main(String[] args) throws IOException {
    String input = "Hello\nмир\nJava\n";

    try (BufferedReader br = new BufferedReader(new StringReader(input));
         BufferedWriter bw = new BufferedWriter(new StringWriter())) {
      String line;
      while ((line = br.readLine()) != null) {
        bw.write(line);
        bw.newLine();
      }
      System.out.println(bw.toString());
    }

    // считаем символы
    try (BufferedReader br = new BufferedReader(new StringReader(input))) {
      int count = TextProcessor.countChars(br);
      System.out.println("chars: " + count);
    }
  }
}
```

---

## Задание 3. Try-with-resources

### task3/PipelineResource.java (задание 3)
```java
package task3;

import java.io.IOException;

// демонстрация try-with-resources: закрытие обоих ресурсов, правильный порядок close
public class PipelineResource implements AutoCloseable {
  private final String name;
  private boolean closed = false;

  public PipelineResource(String name) {
    this.name = name;
  }

  public void doWork() throws IOException {
    if (closed) throw new IllegalStateException("already closed: " + name);
    throw new IOException("error in middle of pipeline: " + name);
  }

  @Override
  public void close() throws Exception {
    System.out.println("closing: " + name);
    closed = true;
  }

  public boolean isClosed() {
    return closed;
  }
}
```

### task3/Task3Demo.java (задание 3)
```java
package task3;

// демонстрация закрытия обоих ресурсов и порядка close (LIFO)
public class Task3Demo {
  public static void main(String[] args) {
    try (PipelineResource first = new PipelineResource("first");
         PipelineResource second = new PipelineResource("second")) {
      System.out.println("opened both");
      first.doWork(); // исключение в середине
    } catch (Exception e) {
      System.out.println("caught: " + e.getMessage());
    }
  }
}
```

---

## Задание 4. Ленивые строки

### task4/LazyLines.java (задание 4)
```java
package task4;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

// Files.lines только внутри try-with-resources; не возвращать stream наружу
public class LazyLines {
  // правильный способ: обработка внутри try-with-resources
  public static long countNonEmpty(Path path) throws IOException {
    if (path == null) throw new IllegalArgumentException("path must not be null");
    try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
      return lines.filter(s -> s != null && !s.isBlank()).count();
    }
    // ресурс Stream закрывается здесь
  }

  // неправильный способ (демонстрация) - возвращает stream наружу
  // вызывающий может не закрыть его -> утечка ресурса
  public static Stream<String> linesLeaked(Path path) throws IOException {
    if (path == null) throw new IllegalArgumentException("path must not be null");
    return Files.lines(path, StandardCharsets.UTF_8); // владелец не определён
  }
}
```

### task4/Task4Demo.java (задание 4)
```java
package task4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

// демонстрация: Files.lines требует владения ресурсом
public class Task4Demo {
  public static void main(String[] args) throws IOException {
    Path dir = Path.of("pract7/task4");
    Files.createDirectories(dir);
    Path file = dir.resolve("data.txt");
    Files.writeString(file, "a\n\nb\n");

    // правильный вариант
    long count = LazyLines.countNonEmpty(file);
    System.out.println("count: " + count);

    // ошибочный возврат наружу - показано, кто обязан владеть ресурсом
    try {
      Stream<String> leaked = LazyLines.linesLeaked(file);
      System.out.println(leaked.count());
      // leaked.close(); // обязателен, если возвращаем наружу
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
```

---

## Задание 5. Бинарный формат

### task5/CatalogBinary.java (задание 5)
```java
package task5;

import src.Course;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// бинарный формат: magic, version, count, records
public class CatalogBinary {
  private static final int MAGIC = 0x4341544C; // 'CATL'
  private static final short VERSION = 1;

  public static void write(DataOutputStream out, List<Course> courses) throws IOException {
    if (out == null || courses == null) throw new IllegalArgumentException("null args");
    out.writeInt(MAGIC);
    out.writeShort(VERSION);
    out.writeInt(courses.size());
    for (Course c : courses) {
      out.writeLong(c.id());
      out.writeUTF(c.title());
      out.writeInt(c.durationHours());
      out.writeUTF(c.status().name());
    }
    out.flush();
  }

  public static List<Course> read(DataInputStream in) throws IOException {
    if (in == null) throw new IllegalArgumentException("in must not be null");
    // проверка magic
    int magic = in.readInt();
    if (magic != MAGIC) throw new IOException("bad magic: " + Integer.toHexString(magic));
    // проверка версии
    short version = in.readShort();
    if (version != VERSION) throw new IOException("unknown version: " + version);
    int count = in.readInt();
    if (count < 0) throw new IOException("negative count");
    List<Course> list = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      try {
        long id = in.readLong();
        String title = in.readUTF();
        int hours = in.readInt();
        String status = in.readUTF();
        Course c = new Course(id, title, hours);
        try {
          c.setStatus(Course.Status.valueOf(status));
        } catch (Exception ignored) {}
        list.add(c);
      } catch (Exception e) {
        throw new IOException("truncated file at record " + i, e);
      }
    }
    return list;
  }
}
```

### task5/Task5Demo.java (задание 5)
```java
package task5;

import src.Course;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// демонстрация бинарного формата с проверками
public class Task5Demo {
  public static void main(String[] args) throws IOException {
    List<Course> courses = new ArrayList<>();
    courses.add(new Course(1, "Java", 10));
    courses.add(new Course(2, "DB", 12));

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(baos)) {
      CatalogBinary.write(out, courses);
    }

    // чтение
    byte[] data = baos.toByteArray();
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
      List<Course> read = CatalogBinary.read(in);
      System.out.println("read: " + read.size());
    }

    // обрезанный файл
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data, 0, 10))) {
      CatalogBinary.read(in);
    } catch (IOException e) {
      System.out.println("truncated: " + e.getMessage());
    }

    // неизвестная версия
    ByteArrayOutputStream bad = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(bad)) {
      out.writeInt(0x4341544C);
      out.writeShort(99); // bad version
      out.writeInt(0);
    }
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bad.toByteArray()))) {
      CatalogBinary.read(in);
    } catch (IOException e) {
      System.out.println("version: " + e.getMessage());
    }
  }
}
```

---

## Задание 6. Временный файл и move

### task6/AtomicReplace.java (задание 6)
```java
package task6;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

// временный файл в том же каталоге, flush/close, замена через move
public class AtomicReplace {
  public static void replace(Path target, String content) throws IOException {
    if (target == null || content == null) throw new IllegalArgumentException("null args");
    Path dir = target.getParent();
    if (dir == null) dir = Path.of(".");
    // временный файл в том же каталоге
    Path tmp = Files.createTempFile(dir, "catalog", ".tmp");
    try {
      Files.writeString(tmp, content, StandardCharsets.UTF_8);
      // flush/close уже сделаны Files.writeString
      try {
        // попытка атомарной замены
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        // обработка отсутствия atomic move
        System.out.println("atomic move not supported, using non-atomic replace");
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (Exception e) {
      // удаляем временный файл при ошибке
      try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
      throw e;
    }
  }
}
```

### task6/Task6Demo.java (задание 6)
```java
package task6;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// демонстрация temp + move
public class Task6Demo {
  public static void main(String[] args) throws IOException {
    Path dir = Path.of("pract7/task6");
    Files.createDirectories(dir);
    Path target = dir.resolve("catalog.txt");
    AtomicReplace.replace(target, "new catalog\n");
    System.out.println(Files.readString(target));
  }
}
```

---

## Задание 7. Частично повреждённый импорт

### task7/ImportResult.java (задание 7)
```java
package task7;

import src.Course;
import java.util.List;

// контракт результата импорта: либо атомарный успех, либо частичный с ошибками
public sealed interface ImportResult permits ImportResult.Success, ImportResult.Failure {
  record Success(List<Course> courses) implements ImportResult {
    public Success {
      courses = List.copyOf(courses);
    }
  }

  record Failure(List<ImportError> errors) implements ImportResult {
    public Failure {
      errors = List.copyOf(errors);
    }
    public boolean hasErrors() {
      return !errors.isEmpty();
    }
  }

  record ImportError(int lineNumber, String message) {}
}
```

### task7/PartialImporter.java (задание 7)
```java
package task7;

import src.Course;
import java.util.ArrayList;
import java.util.List;

// атомарный all-or-nothing: при любой ошибке не публикует половину каталога
public class PartialImporter {
  public ImportResult importAllOrNothing(String text) {
    if (text == null) {
      return new ImportResult.Failure(List.of(new ImportResult.ImportError(0, "text is null")));
    }
    String[] lines = text.split("\\R");
    List<ImportResult.ImportError> errors = new ArrayList<>();
    List<Course> parsed = new ArrayList<>();

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if (line.isBlank()) continue;
      try {
        String[] parts = line.split(";");
        if (parts.length < 3) {
          errors.add(new ImportResult.ImportError(i + 1, "bad format"));
          continue;
        }
        long id = Long.parseLong(parts[0]);
        String title = parts[1];
        int hours = Integer.parseInt(parts[2]);
        Course c = new Course(id, title, hours);
        parsed.add(c);
      } catch (Exception e) {
        errors.add(new ImportResult.ImportError(i + 1, e.getClass().getSimpleName()));
      }
    }

    // не публикуем половину - либо все валидные при строгом подходе, либо собираем ошибки
    // выбран контракт: если есть ошибки - возвращаем Failure и не публикует частичный каталог
    if (!errors.isEmpty()) {
      return new ImportResult.Failure(errors);
    }
    return new ImportResult.Success(parsed);
  }
}
```

### task7/Task7Demo.java (задание 7)
```java
package task7;

// демонстрация частично повреждённого импорта
public class Task7Demo {
  public static void main(String[] args) {
    PartialImporter importer = new PartialImporter();
    String bad = "1;Java;10\nbad;data\n2;DB;12\n";
    ImportResult res = importer.importAllOrNothing(bad);
    if (res instanceof ImportResult.Failure f) {
      System.out.println("errors: " + f.errors().size());
      for (ImportResult.ImportError e : f.errors()) {
        System.out.println("line " + e.lineNumber() + ": " + e.message());
      }
    } else if (res instanceof ImportResult.Success s) {
      System.out.println("ok: " + s.courses().size());
    }
  }
}
```

---

## Задание 8. Потоковый импорт большого файла

### task8/StreamImporter.java (задание 8)
```java
package task8;

import src.Course;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// потоковый импорт без readAllLines, с ограничением памяти
public class StreamImporter {
  private final int errorLimit;
  private final int cancelThreshold;

  public StreamImporter(int errorLimit, int cancelThreshold) {
    this.errorLimit = errorLimit;
    this.cancelThreshold = cancelThreshold;
  }

  public ImportStats importStream(BufferedReader reader) throws IOException {
    if (reader == null) throw new IllegalArgumentException("reader must not be null");
    int lineNo = 0;
    int okCount = 0;
    int badCount = 0;
    List<String> errors = new ArrayList<>(Math.min(errorLimit, 16));

    String line;
    while ((line = reader.readLine()) != null) {
      lineNo++;
      if (line.isBlank()) continue;
      try {
        String[] parts = line.split(";");
        if (parts.length < 3) throw new IllegalArgumentException("bad");
        long id = Long.parseLong(parts[0]);
        String title = parts[1];
        int hours = Integer.parseInt(parts[2]);
        // обрабатываем без хранения всего файла
        new Course(id, title, hours);
        okCount++;
      } catch (Exception e) {
        badCount++;
        if (errors.size() < errorLimit) {
          errors.add("line " + lineNo + ": " + e.getClass().getSimpleName());
        }
      }
      if (badCount >= cancelThreshold) {
        // отмена после заданного порога
        return new ImportStats(okCount, badCount, errors, true);
      }
    }
    return new ImportStats(okCount, badCount, errors, false);
  }

  public record ImportStats(int ok, int bad, List<String> errors, boolean cancelled) {
    public ImportStats {
      errors = List.copyOf(errors);
    }
  }
}
```

### task8/Task8Demo.java (задание 8)
```java
package task8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

// потоковый импорт большого файла без readAllLines
public class Task8Demo {
  public static void main(String[] args) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (int i = 1; i <= 100; i++) {
      sb.append(i).append(";Course").append(i).append(";").append(i % 5 + 1).append("\n");
    }
    sb.append("x;bad\n").append("y;bad\n").append("z;bad\n");

    StreamImporter importer = new StreamImporter(5, 2); // отмена после 2 ошибок
    StreamImporter.ImportStats stats = importer.importStream(new BufferedReader(new StringReader(sb.toString())));
    System.out.println("ok=" + stats.ok() + " bad=" + stats.bad() + " cancelled=" + stats.cancelled());
    for (String e : stats.errors()) {
      System.out.println(e);
    }
  }
}
```

---

## Задание 9. Версионируемый формат

### task9/VersionedFormat.java (задание 9)
```java
package task9;

import src.Course;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

// schema version, checksum, migration v1->v2
public class VersionedFormat {
  private static final int MAGIC = 0x56455253; // 'VERS'
  private static final short VERSION_1 = 1;
  private static final short VERSION_2 = 2;

  public static void writeV2(DataOutputStream out, List<Course> courses) throws IOException {
    if (out == null || courses == null) throw new IllegalArgumentException("null args");
    ByteArrayData ba = new ByteArrayData();
    try (DataOutputStream tmp = new DataOutputStream(ba)) {
      tmp.writeInt(courses.size());
      for (Course c : courses) {
        tmp.writeLong(c.id());
        tmp.writeUTF(c.title());
        tmp.writeInt(c.durationHours());
        tmp.writeUTF(c.status().name());
        tmp.writeInt(c.capacity());
      }
    }
    byte[] payload = ba.toByteArray();
    byte[] checksum = sha256(payload);
    out.writeInt(MAGIC);
    out.writeShort(VERSION_2);
    out.writeInt(payload.length);
    out.write(payload);
    out.write(checksum);
    out.flush();
  }

  public static List<Course> read(DataInputStream in) throws IOException {
    if (in == null) throw new IllegalArgumentException("null");
    int magic = in.readInt();
    if (magic != MAGIC) throw new IOException("bad magic");
    short version = in.readShort();
    if (version > VERSION_2) throw new IOException("unknown future version: " + version); // не читаем как старую
    int payloadLen = in.readInt();
    if (payloadLen < 0) throw new IOException("bad length");
    byte[] payload = new byte[payloadLen];
    in.readFully(payload);
    byte[] given = new byte[32];
    in.readFully(given);
    byte[] calc = sha256(payload);
    if (!MessageDigest.isEqual(calc, given)) throw new IOException("checksum mismatch"); // повреждение до публикации
    try (DataInputStream din = new DataInputStream(new java.io.ByteArrayInputStream(payload))) {
      if (version == VERSION_1) {
        // миграция v1 -> v2
        int count = din.readInt();
        List<Course> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
          long id = din.readLong();
          String title = din.readUTF();
          int hours = din.readInt();
          String status = din.readUTF();
          Course c = new Course(id, title, hours);
          try { c.setStatus(Course.Status.valueOf(status)); } catch (Exception ignored) {}
          // в v1 не было capacity - мигрируем дефолтным
          c.setCapacity(30);
          list.add(c);
        }
        return list;
      } else if (version == VERSION_2) {
        int count = din.readInt();
        List<Course> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
          long id = din.readLong();
          String title = din.readUTF();
          int hours = din.readInt();
          String status = din.readUTF();
          int capacity = din.readInt();
          Course c = new Course(id, title, hours);
          try { c.setStatus(Course.Status.valueOf(status)); } catch (Exception ignored) {}
          c.setCapacity(capacity);
          list.add(c);
        }
        return list;
      }
    }
    throw new IOException("unsupported version");
  }

  public static void writeV1(DataOutputStream out, List<Course> courses) throws IOException {
    ByteArrayData ba = new ByteArrayData();
    try (DataOutputStream tmp = new DataOutputStream(ba)) {
      tmp.writeInt(courses.size());
      for (Course c : courses) {
        tmp.writeLong(c.id());
        tmp.writeUTF(c.title());
        tmp.writeInt(c.durationHours());
        tmp.writeUTF(c.status().name());
      }
    }
    byte[] payload = ba.toByteArray();
    byte[] checksum = sha256(payload);
    out.writeInt(MAGIC);
    out.writeShort(VERSION_1);
    out.writeInt(payload.length);
    out.write(payload);
    out.write(checksum);
    out.flush();
  }

  private static byte[] sha256(byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return md.digest(data);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  static class ByteArrayData extends java.io.ByteArrayOutputStream {}
}
```

### task9/Task9Demo.java (задание 9)
```java
package task9;

import src.Course;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// демонстрация версионируемого формата
public class Task9Demo {
  public static void main(String[] args) throws IOException {
    List<Course> courses = new ArrayList<>();
    Course c = new Course(1, "Java", 10);
    c.setCapacity(25);
    courses.add(c);

    // v2
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(baos)) {
      VersionedFormat.writeV2(out, courses);
    }
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
      List<Course> read = VersionedFormat.read(in);
      System.out.println("v2: " + read.get(0).capacity());
    }

    // миграция v1 -> v2
    ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(baos1)) {
      VersionedFormat.writeV1(out, courses);
    }
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos1.toByteArray()))) {
      List<Course> read = VersionedFormat.read(in);
      System.out.println("migrated: " + read.get(0).capacity());
    }

    // будущая версия не читается как старая
    ByteArrayOutputStream badv = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(badv)) {
      out.writeInt(0x56455253);
      out.writeShort(99);
      out.writeInt(0);
      out.write(new byte[32]);
    }
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(badv.toByteArray()))) {
      VersionedFormat.read(in);
    } catch (IOException e) {
      System.out.println("future: " + e.getMessage());
    }

    // повреждение до публикации
    byte[] data = baos.toByteArray();
    data[data.length - 1] ^= 0xFF;
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
      VersionedFormat.read(in);
    } catch (IOException e) {
      System.out.println("corrupt: " + e.getMessage());
    }
  }
}
```
