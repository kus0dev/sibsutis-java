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
