package task2;

import java.util.Locale;

// нормализация темы: strip, схлопывание пробелов, lower case по Locale.ROOT
public final class TopicIndex {
  private TopicIndex() {}

  public static String normalize(String topic) {
    if (topic == null) return "";
    return topic.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
  }
}
