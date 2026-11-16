package task3;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// реестр сессий: создание и закрытие атомарны на уровне ConcurrentHashMap
public class SessionRegistry {
  private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

  public boolean create(Session session) {
    if (session == null) throw new IllegalArgumentException("session must not be null");
    // putIfAbsent проверяет и вставляет под внутренней блокировкой корзины
    return sessions.putIfAbsent(session.id(), session) == null;
  }

  public Optional<Session> find(String id) {
    if (id == null) throw new IllegalArgumentException("id must not be null");
    return Optional.ofNullable(sessions.get(id));
  }

  public boolean close(String id) {
    if (id == null) throw new IllegalArgumentException("id must not be null");
    return sessions.remove(id) != null;
  }

  public int activeCount() {
    return sessions.size();
  }
}
