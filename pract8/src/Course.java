package src;

import java.util.List;
import java.util.Locale;

// общая модель курса: используется во всех заданиях практики 8
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
