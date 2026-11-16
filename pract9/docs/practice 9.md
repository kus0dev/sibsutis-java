# ПРАКТИЧЕСКАЯ РАБОТА № 9

## Основы конкурентности

| | |
|---|---|
| Университет | ФГБОУ ВО «Сибирский государственный университет телекоммуникаций и информатики» (СибГУТИ) |
| Институт | Институт вычислительной техники (ИВТ) |
| Кафедра | Кафедра вычислительных систем (ВС) |
| Дисциплина | Основы разработки на Java |
| Студент | Устюжанин Денис Константинович |
| Группа | ИС-541 |
| Преподаватель | Нигматулин Руслан Раульевич |
| Город, дата | Новосибирск, 2026 |

---

## Введение

### Цель работы

Освоить базовые механизмы конкурентности Java: атомарность и видимость,
защиту инварианта монитором, `volatile`, потокобезопасные коллекции,
producer–consumer на ограниченной очереди, `ExecutorService`, детерминированные
тесты гонок и запись на курс с ограничением мест.

### Структура проекта

Каждое задание лежит в отдельном пакете `taskN` и имеет собственный демонстрационный
класс `TaskNDemo`. Общего домена, как в практике 8, нет: задания моделируют разные
сценарии, поэтому общие классы не выделялись.

- `task1` — потерянные инкременты и `synchronized`;
- `task2` — флаг остановки, видимость и `volatile`;
- `task3` — реестр сессий на `ConcurrentHashMap`;
- `task4` — producer–consumer на `BlockingQueue` и `ExecutorService`;
- `task5` — JUnit-тест гонки с `CountDownLatch`;
- `task6` — запись на курс с ограничением мест.

Тест запускается через Maven:

```bash
mvn test
```

Демонстрации запускаются из каталога `pract9`:

```bash
java -cp target/classes task1.Task1Demo
```

### Ключевые понятия

1. **Атомарность.** Операция атомарна, если другие потоки не видят её промежуточное
   состояние. `counter++` не атомарен: это чтение, вычисление и запись.
2. **Видимость.** Запись одного потока может быть не видна другому без
   синхронизации. `volatile`, `synchronized` и классы из `java.util.concurrent`
   задают отношения happens-before.
3. **Гонка данных.** Два потока обращаются к одной переменной, хотя бы один
   пишет, и доступа не упорядочены синхронизацией. Результат зависит от
   планировщика.
4. **Монитор.** У каждого объекта есть монитор. `synchronized`-метод блокирует
   монитор `this`, `synchronized`-блок — указанный объект.
5. **Backpressure.** Ограниченная очередь заставляет producer'а ждать, когда
   consumer'ы не успевают. Это ограничивает память и скорость поступления
   заявок.
6. **Пулы потоков.** `ExecutorService` отделяет задачи от потоков и даёт
   управляемое завершение: `shutdown`, `awaitTermination`, `shutdownNow`.

---

## Задание 1. Потерянные инкременты

### Формулировка

Несколько потоков увеличивают общий счётчик. Получите итог меньше ожидаемого и
разложите `counter++` на чтение, вычисление и запись. Тест должен многократно
выявлять проблему. Исправьте счётчик с помощью `synchronized`. Укажите
защищаемый инвариант и объект-монитор. Сравните синхронизированный метод и
минимальный синхронизированный блок.

### Алгоритм решения

1. Создать `UnsynchronizedCounter` с полем `int value`. В `increment()` явно
   записать три шага: `int current = value`, вычисление `current + 1`,
   `value = current + 1`.
2. Между чтением и записью немного расширить окно через `Thread.yield()`, чтобы
   гонка проявлялась стабильно.
3. Запустить 8 потоков по 50 000 инкрементов, повторить попытку до 10 раз и
   найти запуск, где итог меньше `400000`.
4. Исправить счётчик двумя способами: `synchronized`-методом и
   `synchronized`-блоком на отдельном объекте `lock`.
5. Показать, что оба исправленных варианта дают ровно `400000`.

### Основные концепты

- **Read-modify-write.** `value++` читает старое значение, прибавляет единицу и
  записывает результат. Если два потока прочитали одно и то же значение, одно
  обновление теряется.
- **Критическая секция.** Участок, который должен выполняться не более чем одним
  потоком. В нашем случае это чтение и запись `value`.
- **Монитор.** У `SynchronizedMethodCounter` монитором является `this`; у
  `SynchronizedBlockCounter` — поле `lock`. Все обращения к `value` должны идти
  под одним и тем же монитором.
- **Инвариант.** `value` равно числу успешно выполненных `increment`. Пока
  инвариант защищён, промежуточное состояние не наблюдается.
- **`synchronized`-метод и блок.** Метод блокирует весь объект и проще читается.
  Блок позволяет сузить критическую секцию и не блокировать посторонние методы.

### Важный листинг (фрагмент)

```java
public class UnsynchronizedCounter {
  private int value;

  public void increment() {
    int current = value;   // чтение
    Thread.yield();        // расширяем окно, чтобы гонка проявлялась стабильно
    value = current + 1;   // запись
  }

  public int value() {
    return value;
  }
}
```

```java
public synchronized void increment() {
  value++;
}
```

```java
private final Object lock = new Object();
private int value;

public void increment() {
  synchronized (lock) {
    value++;
  }
}
```

### Результат

```text
expected: 400000
unsafe: attempts=1, actual=64349, lost=335651
synchronized method: 400000
synchronized block: 400000
all increments kept: true
```

Число потерянных инкрементов зависит от планировщика, но небезопасный вариант
стабильно даёт результат меньше ожидаемого. Оба синхронизированных варианта
сохраняют инвариант.

---

## Задание 2. Флаг остановки и visibility

### Формулировка

Реализуйте worker с флагом остановки, исследуйте некорректный вариант и
исправьте его через `volatile` либо синхронизацию. Объясните, почему `volatile`
обеспечивает видимость, но не превращает составную операцию в атомарную.

### Алгоритм решения

1. `UnsafeStopFlag` хранит обычное поле `boolean running` и крутится в цикле
   `while (running)`.
2. Главный поток запускает worker, даёт ему поработать 100 мс, вызывает
   `stop()` и ждёт завершения 500 мс.
3. Если поток продолжает работать, значит, запись `running = false` не стала
   видна. Worker объявлен daemon-потоком, чтобы зависший цикл не держал JVM.
4. `VolatileStopFlag` объявляет `running` как `volatile`. Тот же сценарий теперь
   завершается.
5. Объяснить разницу между видимостью и атомарностью.

### Основные концепты

- **JMM.** Java Memory Model определяет, когда запись одного потока обязана
  быть видна другому. Без синхронизации такой гарантии нет.
- **Кеширование и JIT.** Компилятор может держать значение поля в регистре и не
  перечитывать память. Поэтому цикл `while (running)` может не увидеть
  `false`.
- **`volatile`.** Запись в `volatile`-поле сразу публикуется, а чтение всегда
  обращается к памяти. Это обеспечивает видимость и запрещает часть
  перестановок.
- **Атомарность.** `volatile` не делает `iterations++` атомарным: это всё ещё
  чтение, вычисление и запись. Для составной операции нужен `synchronized`,
  `AtomicInteger` или другой атомарный механизм.
- **`Thread.sleep`.** В демонстрации он используется только чтобы дать worker
  поработать. Это не средство синхронизации: момент старта и остановки
  определяют `start()` и `stop()`.

### Важный листинг (фрагмент)

```java
public class UnsafeStopFlag implements Runnable {
  private boolean running = true;

  public void stop() {
    running = false;
  }

  @Override
  public void run() {
    while (running) {
      // busy loop
    }
  }
}
```

```java
public class VolatileStopFlag implements Runnable {
  private volatile boolean running = true;

  public void stop() {
    running = false;
  }

  @Override
  public void run() {
    while (running) {
      // busy loop
    }
  }
}
```

### Результат

```text
plain flag worker:
  stopped: false
  worker still sees running=true
volatile flag worker:
  stopped: true
```

Поведение обычного флага зависит от JVM: на другой реализации цикл может
завершиться. Это и есть главная проблема — корректность не гарантирована.
`volatile` даёт гарантию, поэтому является правильным исправлением.

---

## Задание 3. Потокобезопасный реестр

### Формулировка

Реализуйте реестр активных сессий на `ConcurrentHashMap`. Операция «создать,
только если отсутствует» должна быть атомарной.

### Алгоритм решения

1. Описать `Session` как неизменяемый `record` с `id` и `student`.
2. В `SessionRegistry` хранить `ConcurrentHashMap<String, Session>`.
3. Создание выполнять через `putIfAbsent`: `null` означает, что сессия создана
   этим вызовом; существующее значение — что id уже занят.
4. Поиск возвращать как `Optional<Session>`, закрытие выполнять через
   атомарный `remove`.
5. В демонстрации 32 потока одновременно создают сессию с одним id. Успешным
   должен оказаться ровно один вызов.

### Основные концепты

- **Check-then-act.** Последовательность `containsKey` + `put` не атомарна:
  между проверкой и вставкой другой поток может создать сессию.
- **`putIfAbsent`.** Проверка и вставка выполняются как одна атомарная операция
  над корзиной `ConcurrentHashMap`.
- **`computeIfAbsent`.** Альтернатива, если значение нужно лениво вычислить.
  Для готового объекта достаточно `putIfAbsent`.
- **`size()`.** Для `ConcurrentHashMap` размер является приблизительным при
  одновременных изменениях. В демонстрации все потоки уже завершились, поэтому
  значение точное.
- **`Optional`.** Отсутствие сессии — нормальный результат поиска, а не ошибка.
  `find` возвращает `Optional`, и вызывающий обязан обработать пустой случай.

### Важный листинг (фрагмент)

```java
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
```

### Результат

```text
same id created: 1
active sessions: 1
owner: student-28
distinct created: 32
active after distinct: 33
close S-1: true, active: 32
```

Из 32 одновременных попыток создать `S-1` успешной оказалась ровно одна.
Остальные получили `false` и не изменили реестр.

---

## Задание 4. Producer–consumer

### Формулировка

Организуйте обработку заявок через ограниченную `BlockingQueue`. Несколько
producers добавляют заявки, consumers обрабатывают их. Реализуйте корректное
завершение без busy waiting и объясните, как ёмкость очереди создаёт
backpressure. Перенесите обработку на `ExecutorService`. Выберите размер пула
для заданного CPU-bound или blocking-сценария, дайте потокам осмысленные имена
и гарантируйте `shutdown`, ограниченное ожидание и аварийный `shutdownNow`.

### Алгоритм решения

1. Описать `Request` с `id` и `courseCode`. Отрицательный `id` — служебная
   заявка `STOP`.
2. Создать `ArrayBlockingQueue<Request>` ёмкостью 4.
3. Producer вызывает `put`; когда очередь заполнена, он блокируется. Consumer
   вызывает `take`; когда очередь пуста, он блокируется. Busy waiting нет.
4. После завершения producers главный поток кладёт по одному `STOP` на каждого
   consumer. Consumer получает сигнал через обычный `take` и выходит.
5. Сначала запустить pipeline на обычных `Thread`, затем перенести те же задачи
   на `ExecutorService`.
6. В `finally` вызвать `shutdown`, ограниченное `awaitTermination`, при неудаче
   `shutdownNow` и ещё одно ожидание.

### Основные концепты

- **`BlockingQueue`.** Очередь с блокирующими `put` и `take`. Идеальна для
  producer–consumer: не нужно вручную писать wait/notify.
- **Ёмкость и backpressure.** Очередь хранит не более `capacity` заявок. Если
  consumers медленные, producers блокируются на `put`. Это ограничивает память
  и автоматически замедляет вход.
- **Poison pill.** Служебная заявка `STOP` сообщает consumer'у о завершении.
  Количество сигналов равно количеству consumers.
- **Размер пула.** Consumers блокируются на `take`, поэтому пул должен вмещать
  всех producers и consumers: `2 + 2 = 4`. Для чисто CPU-bound задач размер
  выбирался бы по числу ядер.
- **`ThreadFactory`.** Даёт потокам имена `request-worker-N`, чтобы их было
  видно в стеке и логах.
- **Завершение.** `shutdown` запрещает новые задачи, `awaitTermination`
  ограничивает ожидание, `shutdownNow` прерывает оставшиеся потоки.

### Важный листинг (фрагмент)

```java
public void run() {
  try {
    for (int i = 0; i < count; i++) {
      queue.put(new Request(firstId + i, "COURSE-" + ((firstId + i) % 5 + 1)));
    }
  } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
  }
}
```

```java
public void run() {
  try {
    while (true) {
      Request request = queue.take();
      if (request.isStop()) {
        return;
      }
      processed.incrementAndGet();
    }
  } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
  }
}
```

```java
ExecutorService pool = Executors.newFixedThreadPool(PRODUCERS + CONSUMERS, factory);
try {
  // submit producers and consumers
} finally {
  pool.shutdown();
  if (!pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
    pool.shutdownNow();
    pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }
}
```

### Результат

```text
queue capacity: 4 (producers block when full)
raw threads: processed=1000/1000, producersDone=true, stopDelivered=true, consumersDone=true
executor: processed=1000/1000, poolSize=4
```

Обе версии обработали все 1000 заявок. Очередь не росла бесконечно: при
заполнении producers ждали на `put`. Завершение произошло по `STOP`, без
опросов и `Thread.sleep`.

---

## Задание 5. Детерминированный конкурентный тест

### Формулировка

Напишите JUnit-тест гонки с `CountDownLatch`, `CyclicBarrier` или `Phaser`,
управляя моментом старта и точками протокола. Не используйте `Thread.sleep` как
средство синхронизации. Тест имеет timeout и всегда освобождает executor.

### Алгоритм решения

1. Использовать `CountDownLatch ready` на число worker'ов и `CountDownLatch start`
   на единицу.
2. Каждый worker вызывает `ready.countDown()` и ждёт `start.await()`.
3. Главный поток ждёт `ready`, затем вызывает `start.countDown()`: все worker'ы
   стартуют одновременно.
4. В тесте небезопасного счётчика повторить попытку до 10 раз и проверить, что
   итог меньше ожидаемого.
5. В тесте `SynchronizedMethodCounter` проверить точное равенство ожидаемому
   числу.
6. Ограничить класс аннотацией `@Timeout(20)`.
7. В `finally` вызвать `shutdown`, `awaitTermination(10, SECONDS)`, при неудаче
   `shutdownNow` и ещё одно ожидание.

### Основные концепты

- **`CountDownLatch`.** Потокобезопасный счётчик с ожиданием нуля. `ready`
  гарантирует, что все worker'ы дошли до старта, `start` отпускает их
  одновременно.
- **Детерминизм.** Тест не зависит от `Thread.sleep` и порядка запуска задач:
  точки протокола заданы latch'ами.
- **Нестабильность гонки.** Гонка не обязана проявиться с первого запуска,
  поэтому небезопасный сценарий повторяется несколько раз.
- **Timeout.** `@Timeout` не даёт тесту висеть, если worker не завершился.
- **Освобождение ресурсов.** Executor освобождается в `finally` при любом
  исходе: успех, assertion, исключение или timeout.

### Важный листинг (фрагмент)

```java
CountDownLatch ready = new CountDownLatch(THREADS);
CountDownLatch start = new CountDownLatch(1);
executor.execute(() -> {
  ready.countDown();
  try {
    start.await();
  } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return;
  }
  for (int j = 0; j < PER_THREAD; j++) {
    increment.run();
  }
});
if (!ready.await(5, TimeUnit.SECONDS)) {
  fail("workers did not become ready");
}
start.countDown();
```

```java
finally {
  executor.shutdown();
  if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
    executor.shutdownNow();
    executor.awaitTermination(5, TimeUnit.SECONDS);
  }
}
```

### Результат

```text
[INFO] Running task5.RaceTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Тест небезопасного счётчика обнаружил потерянные инкременты, тест
синхронизированного — подтвердил точный итог. Executor освобождён в обоих
случаях.

---

## Задание 6. Запись на курс с ограничением мест

### Формулировка

Много потоков одновременно вызывают `enroll`. Число успешных записей не
превышает capacity, один студент не занимает место дважды, а отказ не изменяет
состояние. Реализуйте минимум два варианта и сравните coarse-grained lock,
атомарные операции и concurrent collections.

### Алгоритм решения

1. `LockedCourse` хранит `HashSet<Long>` студентов. `enroll` объявлен
   `synchronized`: один монитор защищает проверку capacity, проверку дубликата
   и вставку.
2. `AtomicCourse` хранит `ConcurrentHashMap.newKeySet()` студентов и
   `AtomicInteger reserved` для резервирования мест.
3. В `AtomicCourse` сначала CAS-циклом резервируется место, затем
   `students.add` проверяет уникальность. Если студент уже был добавлен другим
   потоком, резерв возвращается через `decrementAndGet`.
4. В демонстрации 100 студентов на 32 потоках, каждый пытается записаться
   дважды. Проверяется: успешных записей ровно `capacity`, дубликатов нет,
   `enrolled` совпадает с числом уникальных победителей.
5. После заполнения ещё одна попытка должна быть отклонена без изменения
   количества занятых мест.

### Основные концепты

- **Coarse-grained lock.** Один `synchronized` защищает всё состояние курса.
  Просто и надёжно, но все записи сериализуются.
- **CAS.** `compareAndSet` обновляет значение, только если оно не изменилось с
  момента чтения. Цикл повторяется при конкуренции.
- **Concurrent set.** `ConcurrentHashMap.newKeySet()` даёт потокобезопасную
  уникальность без внешней блокировки.
- **Резервирование и rollback.** В lock-free варианте место сначала резервируется.
  Если уникальность не подтвердилась, резерв возвращается.
- **Наблюдаемое состояние.** `available()` считает места по размеру set
  студентов, поэтому отклонённая запись не меняет результат.
- **Инварианты.** `students.size() <= capacity`; каждый студент встречается не
  более одного раза; успешная запись увеличивает `enrolled` ровно на единицу.

### Важный листинг (фрагмент)

```java
public synchronized boolean enroll(long studentId) {
  if (studentId <= 0) throw new IllegalArgumentException("studentId must be > 0");
  if (students.size() >= capacity || students.contains(studentId)) {
    return false;
  }
  students.add(studentId);
  return true;
}
```

```java
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
```

### Результат

```text
coarse lock: successes=10, unique=10, duplicates=0, enrolled=10, available=0
  rejection unchanged: true
atomics + concurrent set: successes=10, unique=10, duplicates=0, enrolled=10, available=0
  rejection unchanged: true
```

Оба варианта заполнили ровно 10 мест, не допустили повторной записи и не
изменили состояние при отказе. Lock-вариант проще; атомарный вариант допускает
больше параллелизма, но требует аккуратного rollback резерва.

---

## Итоги

| Задание | Ключевые API | Главная идея |
|---|---|---|
| 1 | `synchronized`, монитор | `counter++` не атомарен; инкремент нужно защищать |
| 2 | `volatile` | Видимость не равна атомарности |
| 3 | `ConcurrentHashMap`, `putIfAbsent` | Проверка и вставка должны быть одной операцией |
| 4 | `BlockingQueue`, `ExecutorService` | Ограниченная очередь даёт backpressure и корректное завершение |
| 5 | JUnit, `CountDownLatch`, `@Timeout` | Старт и протокол теста управляются latch'ами, а не `sleep` |
| 6 | `synchronized`, CAS, concurrent set | Один инвариант можно защитить разными способами с разной ценой |

Все задания показывают, что конкурентный код нужно проектировать вокруг
инвариантов. Синхронизация защищает и видимость, и атомарность; `volatile`
решает только видимость; concurrent collections и атомарные классы убирают
часть блокировок, но требуют понимания составных операций и откатов.
