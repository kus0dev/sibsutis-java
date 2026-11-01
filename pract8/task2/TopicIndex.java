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
