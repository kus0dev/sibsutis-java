# ПРАКТИЧЕСКАЯ РАБОТА № 10

## Асинхронные вычисления и современная конкурентность

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

Освоить асинхронные вычисления в Java: `Callable` и `Future`, построение цепочек
`CompletableFuture`, объединение независимых загрузок через `allOf`, выбор
executor'а под характер задачи, platform и virtual threads, а также общий
deadline с отменой и ожиданием завершения.

### Структура проекта

Каждое задание лежит в отдельном пакете `taskN` и имеет демонстрационный класс
`TaskNDemo`. Общая модель `Course` вынесена в `src`, потому что используется в
заданиях 2 и 3.

- `src` — общий курс: идентификатор, название, автор и длительность;
- `task1` — `Callable`, `Future`, timeout, ошибка вычисления и отмена;
- `task2` — цепочка `supplyAsync` → `thenApply` → `thenAccept`, `thenCompose`;
- `task3` — `allOf`, сохранение порядка и две политики ошибок;
- `task4` — starvation и раздельные executor'ы для CPU и blocking I/O;
- `task5` — учебный эксперимент с platform и virtual threads;
- `task6` — общий deadline, отмена и ожидание завершения стадии.

Сборка:

```bash
mvn compile
```

Демонстрации запускаются из каталога `pract10`:

```bash
java -cp target/classes task1.Task1Demo
```

### Ключевые понятия

1. **`Future`.** Результат асинхронной задачи, который можно получить с
   ограничением времени, отменить и проверить на завершение.
2. **`CompletableFuture`.** Future, на который можно навешивать преобразования и
   объединять несколько асинхронных операций.
3. **Executor.** Отделяет задачи от потоков. Размер и тип пула выбираются по
   характеру работы: CPU-bound или blocking I/O.
4. **Virtual threads.** Легковесные потоки, которые размонтируются с carrier-потока
   при блокировке. Повышают масштабируемость ожидания, но не ускоряют CPU-код.
5. **Deadline.** Общий бюджет времени на несколько операций, который считается от
   монотонных часов и не сбрасывается на каждой стадии.
6. **Interruption.** Кооперативный запрос остановки: поток сам решает, как на него
   отреагировать, и обязан восстановить флаг, если не может пробросить исключение.

---

## Задание 1. `Callable` и `Future`

### Формулировка

Отправьте вычисление в `ExecutorService`, получите результат через `Future` и
корректно завершите пул. Сравните `Runnable` и `Callable`, включая результат и
checked exception. Добавьте timeout к `Future.get`. Различайте timeout вычисления
и ошибку вычисления; не ждите второй раз без нового бюджета времени. Отмените
долгую задачу через `cancel(true)`. Задача должна реагировать на interruption,
восстанавливать флаг при невозможности пробросить исключение и освобождать
ресурсы. Объясните, почему interrupt — запрос, а не принудительное убийство.

### Алгоритм решения

1. Создать пул из двух потоков с понятными именами `worker-1`, `worker-2` и
   завершать его в `finally`: `shutdown`, ограниченное `awaitTermination`, при
   неудаче — `shutdownNow` и ещё одно ожидание.
2. Отправить `Runnable` и `Callable`: первый вернёт `null`, второй — строку.
3. Отправить задачу, которая спит две секунды, и подождать её 200 мс. Получить
   `TimeoutException` и убедиться, что задача ещё работает (`isDone() == false`).
4. Отменить долгую задачу через `cancel(true)`.
5. Отправить задачу, которая сразу бросает `IllegalStateException`, и показать
   `ExecutionException` с исходной причиной.
6. Создать `InterruptibleTask`, дождаться её старта, отменить и дождаться
   фактического завершения. Проверить, что ресурс закрыт.

### Основные концепты

- **`Runnable` и `Callable`.** `Runnable.run()` возвращает `void` и не может
  пробросить checked exception. `Callable.call()` возвращает значение и может
  бросить `Exception`.
- **`Future.get` с timeout.** Ожидание ограничено бюджетом. `TimeoutException`
  означает, что бюджет закончился, а не что вычисление сломалось.
- **`ExecutionException`.** Вычисление завершилось исключением; исходная причина
  лежит в `getCause()`.
- **`CancellationException`.** Future отменён, результата не будет.
- **Повторное ожидание.** Второй `get` начинает новое ожидание, а не продолжает
  старое. После таймаута нужен новый бюджет от монотонных часов либо отмена.
- **Interruption.** `cancel(true)` выставляет флаг прерывания. Блокирующие вызовы
  бросают `InterruptedException`; обычный цикл должен сам проверять
  `Thread.currentThread().isInterrupted()`. JVM не убивает поток принудительно.
- **Восстановление флага.** Если `InterruptedException` нельзя пробросить дальше,
  нужно вызвать `Thread.currentThread().interrupt()`, чтобы следующий уровень
  узнал о прерывании.
- **Освобождение ресурсов.** `finally` закрывает ресурс независимо от того,
  завершилась задача нормально, с ошибкой или была отменена.

### Важный листинг (фрагмент)

```java
Future<String> slow = pool.submit(() -> {
  Thread.sleep(2_000);
  return "too late";
});

try {
  slow.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
} catch (TimeoutException e) {
  System.out.println("  TimeoutException: task still running=" + !slow.isDone());
  slow.cancel(true);
}
```

```java
@Override
public String call() throws Exception {
  started.countDown();
  try {
    Thread.sleep(10_000);
    return "finished";
  } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return "interrupted";
  } finally {
    resourceClosed = true;
    finished.countDown();
  }
}
```

```java
finally {
  pool.shutdown();
  if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
    pool.shutdownNow();
    pool.awaitTermination(2, TimeUnit.SECONDS);
  }
}
```

### Результат

```text
  Runnable: result is void
Runnable vs Callable:
  runnable.get() = null
  callable.get() = Callable: result is a value
timeout and failure:
  TimeoutException: task still running=true
  ExecutionException: cause=catalog is unavailable
cancellation:
  cancel(true)=true, stopped=true, resourceClosed=true
```

`Runnable` не возвращает результат, поэтому `Future<?>.get()` даёт `null`.
`Callable` возвращает строку. Таймаут не означает ошибку: задача продолжала
работать. `ExecutionException` содержит исходное `IllegalStateException`.
После `cancel(true)` задача получила interruption, закрыла ресурс в `finally` и
завершилась; пул освобождён.

---

## Задание 2. Последовательное преобразование

### Формулировка

Постройте цепочку `supplyAsync` → `thenApply` → `thenAccept` для загрузки и
форматирования карточки курса. Зафиксируйте, в каких потоках выполняются стадии
и какой executor используется. После загрузки пользователя асинхронно загрузите
его записи на курсы. Получите плоский `CompletableFuture<List<Enrollment>>`,
объясните различие `thenApply` и `thenCompose`.

### Алгоритм решения

1. Создать отдельный пул `io-N` для блокирующих операций.
2. Загрузить курс через `supplyAsync(..., io)`: внутри задержка и поиск в
   каталоге.
3. Отформатировать карточку через `thenApply`, вывести через `thenAccept` и
   дождаться результата через `join`.
4. На каждой стадии напечатать имя потока.
5. Загрузить пользователя через `supplyAsync`, затем через `thenCompose`
   запустить асинхронную загрузку записей.
6. Убедиться, что тип результата — плоский
   `CompletableFuture<List<Enrollment>>`, а не вложенный future.

### Основные концепты

- **`supplyAsync`.** Запускает поставщика значения в указанном executor'е.
- **`thenApply`.** Преобразует результат функции `T → U`, возвращая
  `CompletableFuture<U>`.
- **`thenAccept`.** Потребляет результат и возвращает
  `CompletableFuture<Void>`; подходит для последнего шага.
- **Поток выполнения.** Стадии без суффикса `Async` выполняются в потоке,
  завершившем предыдущую стадию. Если future уже завершён, — в потоке,
  вызвавшем стадию. `thenApplyAsync` явно отправляет работу в executor.
- **`thenCompose`.** Преобразование `T → CompletionStage<U>` с разворачиванием:
  получается `CompletableFuture<U>`, а не
  `CompletableFuture<CompletableFuture<U>>`.
- **`join`.** Ждёт результат без checked исключения. Ошибка приходит как
  `CompletionException` с исходной причиной.
- **Модель I/O.** `Thread.sleep` изображает ожидание сети или диска, а не
  CPU-работу.

### Важный листинг (фрагмент)

```java
CompletableFuture<Course> load = CompletableFuture.supplyAsync(() -> {
  log("supplyAsync: load course");
  sleep(100);
  return loadCourse("JAVA-101");
}, io);

CompletableFuture<String> format = load.thenApply(course -> {
  log("thenApply: format card");
  return course.id() + " · " + course.title() + " — " + course.author()
      + ", " + course.minutes() + " мин";
});

CompletableFuture<Void> print = format.thenAccept(card -> {
  log("thenAccept: print card");
  System.out.println("  " + card);
});
print.join();
```

```java
CompletableFuture<User> user = CompletableFuture.supplyAsync(() -> {
  log("supplyAsync: load user");
  sleep(100);
  return new User(7, "Денис");
}, io);

CompletableFuture<List<Enrollment>> enrollments = user.thenCompose(value -> {
  log("thenCompose: switch to enrollments");
  return CompletableFuture.supplyAsync(() -> {
    log("supplyAsync: load enrollments");
    sleep(100);
    return loadEnrollments(value.id());
  }, io);
});
```

### Результат

```text
card chain (caller=main):
  [io-1] supplyAsync: load course
  [io-1] thenApply: format card
  [io-1] thenAccept: print card
  JAVA-101 · Основы Java — Иван Петров, 720 мин
enrollment chain (caller=main):
  [io-2] supplyAsync: load user
  [io-2] thenCompose: switch to enrollments
  [io-3] supplyAsync: load enrollments
  user=Денис, enrollments=2
    JAVA-101 progress=40%
    DB-301 progress=15%
```

Стадии карточки выполнились в `io-1`: этот поток завершил `supplyAsync`, поэтому
`thenApply` и `thenAccept` продолжили работу в нём. Caller `main` только собрал
цепочку и ждал. `thenCompose` выполнился в `io-2`, а внутренний `supplyAsync`
ушёл в `io-3`. Тип `CompletableFuture<List<Enrollment>>` плоский: `thenCompose`
развернул вложенный future.

---

## Задание 3. `allOf` с сохранением результатов

### Формулировка

Загрузите данные для набора course ID. Соберите результаты в исходном порядке,
не теряя типизацию. Определите две политики: fail-fast по смыслу и частичный
результат с перечнем ошибок.

### Алгоритм решения

1. Создать `Map<String, Course>` с тремя курсами. `WEB-401` загружается
   медленнее остальных.
2. Для каждого id запустить `CompletableFuture.supplyAsync` в общем пуле.
3. Сохранить список `List<CompletableFuture<Course>>` и дождаться
   `CompletableFuture.allOf(...)`.
4. Забрать результаты из исходных future в порядке постановки задач: `allOf` не
   хранит значения.
5. Реализовать fail-fast: при первой ошибке завершить общий future
   исключительно, не дожидаясь остальных.
6. Реализовать частичный результат: дождаться всех задач, собрать успешные курсы
   и список ошибок с id.
7. Сравнить время: fail-fast возвращается после быстрой ошибки, частичный
   результат ждёт медленный курс.

### Основные концепты

- **`allOf`.** Возвращает `CompletableFuture<Void>`, который завершается после
  всех задач. Результаты не сохраняются, поэтому нужны исходные future.
- **Порядок.** Обход исходного списка future даёт порядок `ids`, а не порядок
  завершения задач.
- **Типизация.** `List<CompletableFuture<Course>>` позволяет вызвать
  `join()` и сразу получить `Course` без приведения типов.
- **Fail-fast.** Контракт «всё или ошибка»: общий future завершается
  исключительно сразу после первой неудачи. Остальные задачи при этом не
  отменяются автоматически.
- **Частичный результат.** Контракт «всё, что успели»: успешные значения и
  ошибки собираются отдельно. `handle` не даёт `allOf` прервать ожидание.
- **Обёртка исключений.** `join` бросает `CompletionException`; исходная причина
  извлекается из `getCause()`.

### Важный листинг (фрагмент)

```java
List<CompletableFuture<Course>> futures = new ArrayList<>();
for (String id : ids) {
  futures.add(loadAsync(id, io));
}

CompletableFuture<?>[] array = futures.toArray(new CompletableFuture<?>[0]);
CompletableFuture.allOf(array).join();

// allOf не хранит результаты: забираем их из исходных future по порядку
return order(futures);
```

```java
CompletableFuture<List<Course>> result = new CompletableFuture<>();
AtomicInteger remaining = new AtomicInteger(futures.size());
for (CompletableFuture<Course> future : futures) {
  future.whenComplete((course, error) -> {
    if (error != null) {
      Throwable cause = error instanceof CompletionException ? error.getCause() : error;
      result.completeExceptionally(cause);
    } else if (remaining.decrementAndGet() == 0) {
      result.complete(order(futures));
    }
  });
}
return result.join();
```

```java
CompletableFuture<?>[] array = futures.toArray(new CompletableFuture<?>[0]);
// handle не даёт allOf завершиться исключительно: дожидаемся всех задач
CompletableFuture.allOf(array).handle((value, error) -> null).join();
```

### Результат

```text
allOf: elapsed=504ms
  0: JAVA-101 -> Основы Java
  1: DB-301 -> Базы данных
  2: WEB-401 -> Веб-разработка
fail-fast: elapsed=101ms, error=unknown course: NOPE-1
partial: elapsed=502ms, courses=2, errors=1
  ok: JAVA-101
  ok: WEB-401
  error: NOPE-1: unknown course: NOPE-1
```

`allOf` вернул курсы в исходном порядке, хотя `WEB-401` завершился последним.
Fail-fast вернул ошибку через ~100 мс, как только завершился `NOPE-1`, не ожидая
медленный `WEB-401`. Частичный вариант дождался всех задач, сохранил успешные
курсы и отдельно перечислил ошибку. Это разные контракты, и вызывающий код
должен выбирать осознанно.

---

## Задание 4. Правильный executor

### Формулировка

Разделите CPU-bound вычисления и blocking I/O между исполнителями.
Продемонстрируйте starvation при неудачной конфигурации и аргументируйте
размеры/тип каждого executor. Перепишите blocking-операции на
virtual-thread-per-task executor. Сохраните прямолинейный стиль и обычную
обработку исключений. Объясните, почему virtual threads повышают
масштабируемость ожидания, но не ускоряют CPU-bound код.

### Алгоритм решения

1. Создать общий пул из двух потоков. Запустить в нём две блокирующие задачи,
   которые ждут latch и занимают оба потока.
2. Поставить в очередь две CPU-задачи. Подождать 200 мс и проверить, что они не
   начались: свободных потоков нет.
3. Отпустить latch, дождаться выполнения CPU-задач.
4. Создать отдельный CPU-пул размером `availableProcessors()` и
   virtual-thread-per-task executor для I/O.
5. Запустить 100 блокирующих виртуальных задач, удерживаемых latch'ом, и
   убедиться, что CPU-задачи стартуют сразу.
6. Освободить все executor'ы в `finally`.

### Основные концепты

- **Starvation.** Пул исчерпан блокирующими задачами, и задачи в очереди не
  получают поток, хотя процессор свободен. Это не то же самое, что перегрузка
  CPU.
- **CPU-bound пул.** Размер равен числу ядер: лишние потоки не ускоряют счёт,
  а добавляют переключения контекста.
- **Blocking I/O пул.** Для platform threads размер определяется числом
  одновременных ожиданий, а не числом ядер.
- **Virtual threads.** Поток на задачу; при блокировке он снимается с
  carrier-потока, поэтому тысячи ожидающих задач стоят несколько platform
  threads.
- **Предел ускорения.** Виртуальные потоки не добавляют ядер. CPU-bound код
  по-прежнему ограничен процессором.
- **Прямолинейный стиль.** Обычные `submit`, `Future.get`, `try/finally` и
  исключения; без колбэков и ручных цепочек.

### Важный листинг (фрагмент)

```java
ExecutorService shared = Executors.newFixedThreadPool(2, named("shared"));
...
boolean started = cpuStarted.await(200, TimeUnit.MILLISECONDS);
System.out.println("  cpu tasks started while I/O holds both threads: " + started);
if (started) {
  throw new AssertionError("expected starvation");
}
```

```java
ExecutorService cpu = Executors.newFixedThreadPool(cores, named("cpu"));
ExecutorService io = Executors.newVirtualThreadPerTaskExecutor();
...
boolean started = cpuStarted.await(1, TimeUnit.SECONDS);
System.out.println("  100 virtual I/O tasks waiting, cpu tasks started: " + started);
if (!started) {
  throw new AssertionError("cpu tasks starved");
}
```

```java
finally {
  release.countDown();
  stop(io);
  stop(cpu);
}
```

### Результат

```text
available processors: 12
shared fixed pool of 2:
  cpu tasks started while I/O holds both threads: false
  [shared-1] blocking task finished
  [shared-2] blocking task finished
  cpu tasks started after release: true
  [shared-1] cpu task sum=499999500000
  [shared-2] cpu task sum=499999500000
separate executors:
  100 virtual I/O tasks waiting, cpu tasks started: true
  cpu total=5999994000000
```

В общем пуле CPU-задачи не стартовали, пока I/O занимал оба потока. После
разделения 100 ожидающих виртуальных задач не помешали CPU-пулу: он сразу начал
считать. Размер CPU-пула равен 12 (число ядер), I/O-исполнитель — виртуальный
поток на задачу.

---

## Задание 5. Platform и virtual threads

### Формулировка

Проведите воспроизводимый учебный эксперимент с большим числом блокирующих
задач. Сравните время, число platform threads и потребление ресурсов. Отделите
наблюдение от полноценного benchmark и не используйте `sleep` как модель
CPU-работы.

### Алгоритм решения

1. Создать 1000 задач, каждая блокируется на 20 мс.
2. Запустить их в `newFixedThreadPool(32)` и замерить общее время.
3. Сбросить пик `ThreadMXBean`, замерить пиковое число platform threads.
4. Повторить те же задачи в `newVirtualThreadPerTaskExecutor()`.
5. Сравнить время и пиковое число platform threads.
6. Объяснить, что `sleep` моделирует ожидание, а не CPU-работу, и что один
   запуск — наблюдение, а не benchmark.

### Основные концепты

- **Ограничение пула.** Platform-пул из 32 потоков одновременно блокирует
  только 32 задачи; остальные ждут в очереди.
- **Виртуальные потоки.** 1000 виртуальных потоков паркуются одновременно, а
  carrier-потоков нужно примерно по числу ядер.
- **`ThreadMXBean`.** Считает platform threads; виртуальные потоки в этот
  счётчик не входят.
- **Ресурсы.** Platform thread резервирует стек (по умолчанию порядка мегабайта
  адресного пространства); виртуальный поток хранит состояние в куче и растёт по
  необходимости.
- **Наблюдение и benchmark.** Один запуск подвержен JIT-прогреву, планировщику,
  гранулярности `sleep` и фоновой нагрузке. Для полноценного сравнения нужны
  прогрев, несколько итераций и распределение времени.
- **Границы применимости.** Virtual threads масштабируют ожидание, но не
  ускоряют CPU-bound код.

### Важный листинг (фрагмент)

```java
List<Callable<Void>> tasks = new ArrayList<>();
for (int i = 0; i < TASKS; i++) {
  tasks.add(() -> {
    try {
      Thread.sleep(BLOCK_MILLIS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return null;
  });
}

long start = System.nanoTime();
List<Future<Void>> futures = executor.invokeAll(tasks);
long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
int peak = threads.getPeakThreadCount();
```

```java
run("platform", Executors.newFixedThreadPool(PLATFORM_THREADS));
run("virtual", Executors.newVirtualThreadPerTaskExecutor());
```

### Результат

```text
tasks=1000, blocking=20ms, cores=12
blocking wait only: sleep is not a model of CPU work
platform: elapsed=646ms, peak platform threads=38 (baseline 6), completed=1000/1000
virtual: elapsed=36ms, peak platform threads=20 (baseline 6), completed=1000/1000
```

Platform-вариант прошёл примерно за `1000 / 32 * 20 ≈ 625` мс: пул успевал
блокировать только 32 задачи. Виртуальный вариант завершился почти за одно
ожидание. Пик platform threads в первом случае вырос на 32 (рабочие потоки пула),
во втором — только на carrier-потоки. Все 1000 задач завершились в обоих
случаях.

Цифры зависят от машины и нагрузки; устойчив качественный вывод: platform-пул
ограничивает число одновременных ожиданий, virtual-thread-per-task — нет. Память
точно не измерялась, поэтому вывод о ресурсах опирается на число platform
threads, а не на профилирование.

---

## Задание 6. Общий deadline

### Формулировка

Несколько подзадач делят один временной бюджет. Перед каждой стадией вычисляйте
оставшееся время от монотонных часов, отменяйте ненужную работу и дожидайтесь
её завершения. Не используйте preview API; сформулируйте, какую проблему решает
structured concurrency.

### Алгоритм решения

1. Вычислить дедлайн один раз: `System.nanoTime() + бюджет`.
2. Перед каждой стадией посчитать остаток `deadline - System.nanoTime()`.
3. Если остаток не положительный, стадию не запускать.
4. Иначе отправить задачу и ждать `future.get(remaining, NANOSECONDS)`.
5. При `TimeoutException` вызвать `cancel(true)`.
6. Дождаться фактического завершения задачи по отдельному latch'у с небольшим
   новым бюджетом.
7. Продолжить со следующей стадией, используя тот же общий дедлайн.

### Основные концепты

- **Монотонные часы.** `System.nanoTime()` измеряет интервалы и не зависит от
  перевода системных часов. `currentTimeMillis()` для дедлайна ненадёжен.
- **Общий бюджет.** Остаток считается от одного дедлайна; время, потраченное на
  предыдущих стадиях, не возвращается.
- **Timeout и отмена.** `Future.get` ограничивает ожидание. `cancel(true)`
  посылает interruption, но не ждёт завершения.
- **Ожидание после отмены.** Повторный `get` после отмены сразу бросает
  `CancellationException`, поэтому нужен отдельный сигнал завершения и новый
  небольшой бюджет.
- **Пропуск стадий.** Если остаток нулевой или отрицательный, работа не
  запускается — это и есть отмена ненужной работы на уровне плана.
- **Structured concurrency.** Дочерние задачи живут внутри scope родителя:
  родитель ждёт их завершения, отмена и ошибки распространяются по дереву. Это
  решает проблему «осиротевших» задач и ручного управления временем жизни, а не
  проблему скорости. Preview API не используется.

### Важный листинг (фрагмент)

```java
long remaining = deadline - System.nanoTime();
if (remaining <= 0) {
  System.out.println(name + ": skipped, no time left");
  return;
}

CountDownLatch finished = new CountDownLatch(1);
Future<String> future = pool.submit(() -> {
  try {
    Thread.sleep(workMillis);
    return "done";
  } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return "interrupted";
  } finally {
    finished.countDown();
  }
});

try {
  System.out.println(name + ": " + future.get(remaining, TimeUnit.NANOSECONDS));
} catch (TimeoutException e) {
  future.cancel(true);
  boolean stopped = finished.await(CANCEL_GRACE_MILLIS, TimeUnit.MILLISECONDS);
  System.out.println(name + ": timeout, cancel(true), stopped=" + stopped);
}
```

### Результат

```text
budget=600ms
load course: done
load modules: done
build report: timeout, cancel(true), stopped=true
send notification: skipped, no time left
total elapsed=602ms
```

Первые две стадии заняли 150 и 300 мс. Третьей осталось около 150 мс, хотя ей
нужно 400 мс: она была отменена, дождалась завершения и освободила ресурс.
Четвёртая стадия не стартовала, потому что бюджет исчерпан. Общее время
осталось в пределах 600 мс плюс небольшой запас на отмену.

---

## Итоги

| Задание | Ключевые API | Главная идея |
|---|---|---|
| 1 | `Callable`, `Future`, `cancel` | Timeout, ошибка вычисления и отмена — разные события; interrupt кооперативен |
| 2 | `supplyAsync`, `thenApply`, `thenCompose` | Цепочка выполняется в завершающем потоке; `thenCompose` убирает вложенность |
| 3 | `allOf`, `whenComplete`, `handle` | `allOf` не хранит результаты; fail-fast и частичный результат — разные контракты |
| 4 | `ExecutorService`, virtual threads | Blocking I/O не должен занимать потоки CPU-пула |
| 5 | `ThreadMXBean`, virtual threads | Virtual threads масштабируют ожидание, но не ускоряют вычисления |
| 6 | `nanoTime`, `Future.get`, `cancel` | Общий дедлайн считается от монотонных часов и не сбрасывается на стадиях |

Асинхронный код остаётся управляемым, если явно определены три вещи: где
выполняется каждая стадия, какой у операции бюджет времени и кто отвечает за
отмену и освобождение ресурсов. `CompletableFuture` упрощает композицию, но не
отменяет эти вопросы. Virtual threads снимают ограничение на число ожидающих
задач, однако CPU-bound работу по-прежнему ограничивает число ядер.
