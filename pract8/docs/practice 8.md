# ПРАКТИЧЕСКАЯ РАБОТА № 8

## Stream API и функциональная обработка коллекций

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

Освоить функциональную обработку коллекций в Java: детерминированную сортировку через `Comparator`, преобразование вложенных структур через `flatMap`, свёртку значений через `reduce` с корректной identity, готовые и собственные `Collector`, применение `Optional` как результата поиска, а также построение однопроходного отчёта по большому потоку событий с ограниченной памятью и без внешних side effects.

### Общая предметная модель

В заданиях 1–6 используется одна и та же модель курса. Задание 7 работает с потоком событий и ссылается только на общий `src.Level`. Общие классы вынесены в каталог `pract8/src`, потому что они не меняются от задания к заданию и переиспользуются в нескольких задачах.

- `src.Level` — уровень курса: `BASIC`, `INTERMEDIATE`, `ADVANCED`. Естественный порядок `enum` совпадает с порядком объявления, поэтому его можно использовать в `Comparator.comparing`.
- `src.Module` — модуль курса: название и список тем. Список тем копируется через `List.copyOf`, поэтому объект модуля неизменяем.
- `src.Course` — неизменяемая модель курса: `id`, `code`, `title`, `level`, `durationHours`, `modules`. Код нормализуется к верхнему регистру, список модулей копируется.

Такая модель позволяет заданиям 1–7 работать с одним и тем же набором данных и не дублировать доменные классы.

### Ключевые свойства Stream API

1. **Ленивость.** Промежуточные операции (`map`, `filter`, `flatMap`, `sorted`, `distinct`, `limit`) не начинают работу, пока не вызвана терминальная операция (`collect`, `reduce`, `forEach`, `count`, `max`).
2. **Одноразовость.** Поток можно потребить только один раз. Повторное использование того же объекта `Stream` приводит к `IllegalStateException`.
3. **Декларативность.** Код описывает «что нужно получить», а не «как перебирать элементы». Но это не отменяет необходимости понимать стоимость операций и порядок обхода.
4. **Отсутствие внешних side effects.** Хорошая функция в потоке зависит только от входного элемента и возвращает результат, а не печатает, не пишет в файл и не меняет общий изменяемый объект.
5. **Детерминизм.** Если результат зависит от порядка, нужно либо явно задать порядок (`sorted`, `TreeMap`, `LinkedHashMap`), либо использовать структуры без порядка и честно это документировать.

---

## Задание 1. Сортировка и детерминизм

### Формулировка

Отсортируйте по `level`, `duration` и `code` с явными tie-breakers. Результат одинаков для разных исходных порядков, если множество данных одинаково.

### Алгоритм решения

1. Построить `Comparator<Course>` из трёх основных ключей: `level`, `durationHours`, `code`.
2. Добавить последний tie-breaker `id`. Он уникален и гарантирует, что даже полностью совпадающие по трём ключам курсы получат стабильный порядок.
3. Не сортировать исходный список на месте: сделать копию, отсортировать её и вернуть неизменяемый результат через `List.copyOf`.
4. В демонстрации несколько раз перемешать один и тот же набор курсов с разными `Random(seed)` и проверить, что последовательность кодов после сортировки одинаковая.
5. Напечатать итоговый порядок, чтобы увидеть, как работают tie-breakers.

### Основные концепты

- **Comparator** задаёт отношение порядка. Если `compare(a, b) < 0`, то `a` идёт раньше `b`. Компаратор должен быть согласован: антисимметричен, транзитивен и не противоречить `equals` там, где это важно.
- **Tie-breaker** — дополнительный ключ, который разрешает ничью по предыдущему ключу. Без него порядок равных элементов может зависеть от исходного порядка. Стабильная сортировка сохраняет взаимный порядок равных элементов, но не делает результат независимым от входного порядка.
- **Полный порядок** достигается тогда, когда последний ключ уникален. В нашем случае это `id`: два разных курса не могут иметь одинаковый `id`.
- **`Comparator.comparing`** использует естественный порядок ключа. Для `enum` это порядок объявления: `BASIC < INTERMEDIATE < ADVANCED`. Для строк — лексикографический порядок.
- **`thenComparingInt`**, **`thenComparing`**, **`thenComparingLong`** добавляют ключи по цепочке. Цепочка читается сверху вниз: сначала `level`, затем `duration`, затем `code`, затем `id`.
- **Отсутствие побочных эффектов.** Метод `sorted` не должен незаметно менять переданный список. Копирование делает функцию чище и безопаснее.

### Важный листинг (фрагмент)

```java
private static final Comparator<Course> ORDER =
    Comparator.comparing(Course::level)
        .thenComparingInt(Course::durationHours)
        .thenComparing(Course::code)
        .thenComparingLong(Course::id);

public static List<Course> sorted(List<Course> courses) {
  if (courses == null) throw new IllegalArgumentException("courses must not be null");
  List<Course> copy = new ArrayList<>(courses);
  copy.sort(ORDER);
  return List.copyOf(copy);
}
```

Проверка независимости от входного порядка:

```java
List<Course> expected = CourseSorting.sorted(courses);
for (int seed = 1; seed <= 3; seed++) {
  List<Course> shuffled = new ArrayList<>(courses);
  Collections.shuffle(shuffled, new Random(seed));
  List<Course> actual = CourseSorting.sorted(shuffled);
  System.out.println(actual.stream().map(Course::code).toList()
      .equals(expected.stream().map(Course::code).toList()));
}
```

### Результат

Демонстрация подтверждает, что три разных перемешивания одного набора дают одинаковый порядок:

```text
seed 1 same order: true
seed 2 same order: true
seed 3 same order: true
BASIC 8 JAVA-102
BASIC 12 ALGO-201
BASIC 12 JAVA-101
INTERMEDIATE 16 DB-301
ADVANCED 10 WEB-401
```

Видно, что `ALGO-201` и `JAVA-101` имеют одинаковые `level` и `duration`, поэтому их порядок определяется ключом `code`.

---

## Задание 2. `flatMap`

### Формулировка

Из курсов со списком модулей получите уникальные нормализованные темы и обратный индекс `topic → courses`. Не создавайте вложенный список вручную.

### Алгоритм решения

1. Реализовать нормализацию темы: `strip()`, схлопывание любых последовательностей пробельных символов в один пробел, приведение к нижнему регистру через `Locale.ROOT`.
2. Для уникальных тем построить поток:
   `courses.stream()` → `flatMap(course -> course.modules().stream())` → `flatMap(module -> module.topics().stream())` → `map(normalize)` → `filter(not empty)` → `collect(toCollection(TreeSet::new))`.
3. Для обратного индекса из каждого курса получить пары `(нормализованная тема, курс)`. Внутри одного курса убрать повторяющиеся темы через `distinct`, чтобы один курс не попал в индекс темы дважды.
4. Сгруппировать пары по теме через `Collectors.groupingBy` с `TreeMap` в качестве фабрики карты.
5. Внутри каждой группы отсортировать курсы по `code`, чтобы порядок в списке не зависел от порядка обхода.
6. Продемонстрировать нормализацию на темах с лишними пробелами и разным регистром.

### Основные концепты

- **`map` против `flatMap`.** `map(course -> course.modules())` дал бы `Stream<List<Module>>`. `flatMap` «разворачивает» каждый вложенный поток в общий поток элементов: `Stream<Course>` → `Stream<Module>` → `Stream<String>`. Ручное создание `List<List<String>>` и последующая вложенная агрегация не нужны.
- **Два уровня вложенности.** Здесь вложенность двойная: курс содержит модули, модуль содержит темы. Поэтому применяются два последовательных `flatMap`.
- **Нормализация.** `strip()` убирает пробелы по краям, `replaceAll("\\s+", " ")` схлопывает внутренние пробелы, `toLowerCase(Locale.ROOT)` приводит регистр. `Locale.ROOT` важен для предсказуемости: регистронезависимые преобразования не должны зависеть от локали машины (например, турецкой, где `I` и `i` ведут себя иначе).
- **`distinct()`** использует `equals`/`hashCode`. Для строк это сравнение содержимого. В обратном индексе `distinct` применяется после нормализации и до создания пары, поэтому повтор темы внутри одного курса не создаёт дубликат.
- **`TreeSet` и `TreeMap`** дают естественный порядок ключей и детерминированный обход. Это важно, если результат печатается или сравнивается.
- **`Collectors.mapping` + `collectingAndThen`** позволяют преобразовать элементы группы и сразу привести итоговый список к нужному виду. `toList()` возвращает неизменяемый список.

### Важный листинг (фрагмент)

```java
public static Set<String> uniqueTopics(List<Course> courses) {
  return courses.stream()
      .flatMap(course -> course.modules().stream())
      .flatMap(module -> module.topics().stream())
      .map(TopicIndex::normalize)
      .filter(topic -> !topic.isEmpty())
      .collect(Collectors.toCollection(TreeSet::new));
}
```

Обратный индекс:

```java
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
```

### Результат

Темы `" Java "` и `"JAVA"` после нормализации становятся одной темой `java`, а тема `types` встречается в двух курсах и попадает в обратный индекс дважды:

```text
unique topics: [b+ tree, http, index, java, list, map, sql, types]
b+ tree -> [DB-201]
http -> [WEB-301]
index -> [DB-201, WEB-301]
java -> [JAVA-101]
list -> [JAVA-101]
map -> [JAVA-101]
sql -> [DB-201]
types -> [DB-201, JAVA-101]
```

---

## Задание 3. `reduce` с правильной identity

### Формулировка

Посчитайте сумму длительностей и объедините value objects. Покажите неправильную identity, нарушающую результат, и сформулируйте ассоциативность accumulator.

### Алгоритм решения

1. Для суммы чисел использовать `mapToInt(Course::durationHours).reduce(0, Integer::sum)`. Ноль — нейтральный элемент для сложения.
2. Для объединения value objects создать неизменяемый `DurationSummary(totalHours, courseCount)` с методом `plus`.
3. Определить `DurationSummary.empty()` как `(0, 0)` — нейтральный элемент для операции `plus`.
4. Свернуть поток через `map(course -> new DurationSummary(course.durationHours(), 1))` и `reduce(DurationSummary.empty(), DurationSummary::plus)`.
5. Показать неправильную identity `(10, 1)`: она добавляет к результату лишние 10 часов и лишний курс, потому что не является нейтральной.
6. Проверить ту же свёртку в `parallelStream()` с трёхаргументным `reduce`, где третий аргумент — combiner.
7. Проверить ассоциативность на трёх значениях: `(a + b) + c` должно быть равно `a + (b + c)`.

### Основные концепты

- **Identity (нейтральный элемент).** Для операции `⊕` элемент `e` называется identity, если `e ⊕ a = a` и `a ⊕ e = a` для любого `a`. Для сложения чисел это `0`, для умножения — `1`, для конкатенации строк — `""`, для `DurationSummary` — `(0, 0)`.
- **Accumulator.** Функция, которая добавляет один элемент к накопленному значению. В нашем случае она превращает курс в `DurationSummary(hours, 1)` и складывает с текущей суммой.
- **Ассоциативность.** Операция ассоциативна, если `(a ⊕ b) ⊕ c = a ⊕ (b ⊕ c)`. Это свойство критично для параллельной свёртки: поток может разбиться на части, и порядок объединения частичных результатов не должен влиять на итог. Сложение чисел и сложение пар `(часы, количество)` ассоциативны (при отсутствии переполнения).
- **Коммутативность не требуется.** Для `reduce` важна именно ассоциативность. Коммутативность (`a ⊕ b = b ⊕ a`) была бы дополнительным удобством, но не обязательным условием корректности.
- **Три формы `reduce`:**
  1. `reduce(BinaryOperator)` — возвращает `Optional`, потому что поток может быть пустым.
  2. `reduce(identity, accumulator)` — возвращает значение, identity используется как стартовое.
  3. `reduce(identity, accumulator, combiner)` — нужна для параллельных потоков; combiner объединяет частичные результаты.
- **Почему неправильная identity опасна.** В последовательном потоке она сдвигает результат один раз. В параллельном потоке identity может применяться к каждой части, поэтому ошибка усиливается. Кроме того, `reduce` с identity предполагает, что identity действительно нейтральна; иначе результат зависит от количества разбиений.
- **Переполнение.** `Math.addExact` в `plus` превращает незаметное переполнение `int` в явное исключение. Это лучше, чем молча получить отрицательную сумму.

### Важный листинг (фрагмент)

```java
public static DurationSummary summary(List<Course> courses) {
  return courses.stream()
      .map(course -> new DurationSummary(course.durationHours(), 1))
      .reduce(DurationSummary.empty(), DurationSummary::plus);
}

public static DurationSummary summaryWithWrongIdentity(List<Course> courses) {
  return courses.stream()
      .map(course -> new DurationSummary(course.durationHours(), 1))
      .reduce(new DurationSummary(10, 1), DurationSummary::plus);
}

public static boolean associative(DurationSummary a, DurationSummary b, DurationSummary c) {
  return a.plus(b).plus(c).equals(a.plus(b.plus(c)));
}
```

### Результат

```text
sum hours: 36
correct identity: DurationSummary[totalHours=36, courseCount=3]
parallel: DurationSummary[totalHours=36, courseCount=3]
parallel equals sequential: true
wrong identity: DurationSummary[totalHours=46, courseCount=4]
associative: true
```

Неправильная identity `(10, 1)` дала `46` часов и `4` курса вместо `36` часов и `3` курсов. Это наглядно показывает, что identity обязана быть нейтральным элементом, а не просто «начальным значением».

---

## Задание 4. Collectors

### Формулировка

Сгруппируйте курсы по `level`, посчитайте количество и среднюю длительность, соберите summary object. Не выполняйте три полных прохода без объяснения.

### Алгоритм решения

1. Создать неизменяемый `LevelSummary(count, totalHours, averageDuration)`.
2. Использовать `Collectors.groupingBy(Course::level, TreeMap::new, downstream)`.
3. В качестве downstream применить `Collectors.teeing`:
   - первый коллектор — `Collectors.counting()`;
   - второй коллектор — `Collectors.summingLong(Course::durationHours)`;
   - merger получает `(count, total)` и создаёт `LevelSummary` с вычисленной средней длительностью.
4. Вернуть `Map<Level, LevelSummary>` с детерминированным порядком ключей (`TreeMap`).
5. В демонстрации вывести количество, сумму и среднюю длительность по каждому уровню.

### Основные концепты

- **`Collectors.groupingBy`** — терминальная операция, которая раскладывает элементы по группам. Форма с тремя аргументами позволяет задать фабрику карты и downstream-коллектор.
- **Downstream-коллектор** обрабатывает элементы каждой группы. Вместо `List<Course>` можно сразу получить число, сумму, среднее, строку и т.д.
- **`Collectors.counting()`** считает количество элементов группы, **`Collectors.summingLong`** суммирует длительности.
- **`Collectors.teeing`** (Java 12+) запускает два коллектора параллельно на одном и том же потоке элементов и объединяет их результаты функцией-merger. Это не два прохода по исходной коллекции: каждый элемент в рамках одного обхода передаётся обоим downstream-коллекторам.
- **Почему не три прохода.** Можно было бы сделать `count()` для каждого уровня, затем `sum()`, затем `average()`, но это три полных обхода коллекции (а с фильтрацией по уровню — ещё больше). `groupingBy` + `teeing` делает один обход исходных данных и аккумулирует всё нужное по группам. Сложность остаётся `O(n)`.
- **Средняя длительность** вычисляется в finisher-функции `teeing` как `total / count`. Это дешевле, чем третий проход.
- **`TreeMap`** нужен для детерминированного порядка уровней. `HashMap` тоже корректен по смыслу, но порядок обхода не гарантирован.
- **Summary object** вместо трёх отдельных чисел делает результат самодокументируемым и неизменяемым. `record` автоматически даёт `equals`, `hashCode`, `toString` и accessor-методы.

### Важный листинг (фрагмент)

```java
public static Map<Level, LevelSummary> summarize(List<Course> courses) {
  return courses.stream().collect(Collectors.groupingBy(
      Course::level,
      TreeMap::new,
      Collectors.teeing(
          Collectors.counting(),
          Collectors.summingLong(Course::durationHours),
          (count, total) -> LevelSummary.of(count, total))));
}
```

### Результат

```text
BASIC: count=2, total=20, avg=10.00
INTERMEDIATE: count=1, total=16, avg=16.00
ADVANCED: count=2, total=30, avg=15.00
```

Для `BASIC` два курса с длительностями 8 и 12: сумма 20, среднее 10. Для `ADVANCED` два курса с длительностями 10 и 20: сумма 30, среднее 15.

---

## Задание 5. `Optional` как результат

### Формулировка

Найдите самый длинный курс, преобразуйте результат и задайте fallback. Не используйте `Optional.get` без доказанного наличия и не храните `Optional` в поле сущности.

### Алгоритм решения

1. Построить `Comparator<Course>` по `durationHours`, при равенстве — по `code` для детерминизма.
2. Вернуть `Optional<Course>` из метода `longest`: `courses.stream().max(LONGEST)`.
3. Преобразовать результат через `map(Course::title)` в `Optional<String>`, не вызывая `get`.
4. Для отсутствующего результата использовать `orElse(fallback)` или `orElseGet(supplier)`.
5. В демонстрации показать непустой результат через `ifPresent`, пустой результат с обычным fallback и ленивый вычисляемый fallback.
6. Не хранить `Optional` в полях доменных объектов: `Optional` предназначен для возврата из методов, где отсутствие значения является нормальным ожидаемым случаем.

### Основные концепты

- **Зачем нужен `Optional`.** Он делает отсутствие результата частью типа метода. Вызывающий видит `Optional<Course>` и понимает, что курс может не найтись, без чтения документации и без риска неожиданного `null`.
- **`max` возвращает `Optional`.** Для пустого потока результата нет, поэтому `max` не может вернуть «какой-то» элемент. Это честный контракт.
- **`map` на `Optional`.** Если значение есть, применяется функция; если значения нет, остаётся `Optional.empty()`. Это позволяет строить цепочки преобразований без проверок на `null`.
- **`ifPresent`** выполняет действие только при наличии значения. **`orElse`** возвращает значение по умолчанию, причём аргумент вычисляется всегда. **`orElseGet`** принимает `Supplier` и вычисляет fallback только при пустом `Optional`; это предпочтительно, если fallback дорогой.
- **Почему не `get`.** `Optional.get()` бросает `NoSuchElementException`, если значения нет. Он допустим только тогда, когда наличие значения доказано предыдущей проверкой, но в большинстве случаев есть более выразительные `map`, `ifPresent`, `orElse`, `orElseThrow`.
- **Почему `Optional` не поле сущности.** `Optional` не предназначен для сериализации, не реализует `Serializable`, добавляет лишний слой вложенности и усложняет API. В поле сущности лучше использовать `null` (если отсутствие допустимо) или отдельный доменный тип/состояние. `Optional` уместен в возвращаемом типе метода-поиска.
- **Детерминизм при равной длительности.** `max` сам по себе вернёт первый или последний равный элемент в зависимости от порядка. Дополнительный ключ `code` делает выбор предсказуемым.

### Важный листинг (фрагмент)

```java
private static final Comparator<Course> LONGEST =
    Comparator.comparingInt(Course::durationHours)
        .thenComparing(Course::code);

public static Optional<Course> longest(List<Course> courses) {
  return courses.stream().max(LONGEST);
}

public static Optional<String> longestTitle(List<Course> courses) {
  return longest(courses).map(Course::title);
}

public static String longestTitleOrDefault(List<Course> courses, String fallback) {
  return longestTitle(courses).orElse(fallback);
}

public static String longestTitleOrComputed(List<Course> courses, Supplier<String> fallback) {
  return longestTitle(courses).orElseGet(fallback);
}
```

### Результат

```text
longest: Databases
fallback: no courses
computed: empty catalog
```

Для непустого списка найден курс `Databases` длительностью 16 часов. Для пустого списка срабатывают оба варианта fallback, при этом нигде не вызывается `Optional.get()`.

---

## Задание 6. Собственный collector

### Формулировка

Создайте статистику `count/sum/min/max` без mutable static state. Combiner корректен для разбиения, finisher возвращает immutable result. Сравните с готовыми summarizing collectors.

### Алгоритм решения

1. Создать неизменяемый `DurationStats(count, sum, min, max)` с методом `average()` и фабрикой `empty()`.
2. Создать изменяемый `DurationStatsAccumulator` с полями экземпляра: `count`, `sum`, `min`, `max`. Никаких `static` изменяемых полей.
3. Реализовать `add(Course)` — добавить один курс; `combine(other)` — объединить два аккумулятора; `finish()` — получить неизменяемый `DurationStats`.
4. Реализовать `DurationStatsCollector implements Collector<Course, DurationStatsAccumulator, DurationStats>`:
   - `supplier()` создаёт новый аккумулятор;
   - `accumulator()` вызывает `add`;
   - `combiner()` вызывает `combine`;
   - `finisher()` вызывает `finish`;
   - `characteristics()` возвращает `UNORDERED`, потому что статистика не зависит от порядка.
5. Проверить последовательный и параллельный сбор, а также ручное объединение двух частей.
6. Сравнить результат с `Collectors.summarizingInt(Course::durationHours)`.

### Основные концепты

- **Контракт `Collector<T, A, R>`.** `T` — тип элемента, `A` — тип изменяемого аккумулятора, `R` — тип результата. Collector описывает четыре функции: создание аккумулятора, добавление элемента, объединение аккумуляторов и финальное преобразование.
- **Почему аккумулятор изменяемый.** Collector может обрабатывать миллионы элементов; создавать новый immutable-объект на каждый элемент дорого. Изменяемость локализована внутри одного `collect`: каждый поток получает свой аккумулятор через `supplier()`.
- **Почему нет mutable static state.** Если бы аккумулятор был `static`, параллельные вызовы `collect` начали бы мешать друг другу, а результат зависел бы от истории предыдущих вызовов. Поля экземпляра изолируют состояние.
- **Корректный combiner.** При параллельном выполнении поток разбивается на части. `combine` должен объединять частичные статистики так же, как если бы элементы обработали последовательно. Пустая часть не должна менять результат; `min` и `max` объединяются через `Math.min`/`Math.max`.
- **Finisher возвращает immutable result.** `DurationStats` — `record`, все поля `final`, нет сеттеров. После завершения `collect` результат нельзя случайно изменить.
- **`Characteristics.UNORDERED`.** Статистика не зависит от порядка элементов, поэтому collector может сообщить об этом среде выполнения. Это не влияет на корректность, но даёт больше свободы для параллельной обработки.
- **Сравнение с `summarizingInt`.** Готовый `Collectors.summarizingInt` возвращает `IntSummaryStatistics` — изменяемый объект с `count`, `sum`, `min`, `max`, `average`. Для непустого потока результаты совпадают. Для пустого потока есть важное отличие: у `IntSummaryStatistics` `min = Integer.MAX_VALUE`, `max = Integer.MIN_VALUE`, а среднее `0.0`; наш `DurationStats.empty()` использует `0/0/0/0`. Это осознанное решение: для доменной статистики длительностей ноль понятнее, чем технические границы `int`.
- **Среднее.** В `DurationStats` среднее вычисляется методом `average()` из `sum / count`, а не хранится отдельным полем. Это исключает рассогласование между суммой, количеством и средним.

### Важный листинг (фрагмент)

```java
final class DurationStatsAccumulator {
  private long count;
  private long sum;
  private int min = Integer.MAX_VALUE;
  private int max = Integer.MIN_VALUE;

  void add(Course course) {
    int hours = course.durationHours();
    count++;
    sum += hours;
    min = Math.min(min, hours);
    max = Math.max(max, hours);
  }

  DurationStatsAccumulator combine(DurationStatsAccumulator other) {
    if (other.count == 0) return this;
    count += other.count;
    sum += other.sum;
    min = Math.min(min, other.min);
    max = Math.max(max, other.max);
    return this;
  }

  DurationStats finish() {
    return count == 0 ? DurationStats.empty()
        : new DurationStats(count, sum, min, max);
  }
}
```

Сам collector:

```java
public final class DurationStatsCollector
    implements Collector<Course, DurationStatsAccumulator, DurationStats> {

  @Override public Supplier<DurationStatsAccumulator> supplier() {
    return DurationStatsAccumulator::new;
  }

  @Override public BiConsumer<DurationStatsAccumulator, Course> accumulator() {
    return DurationStatsAccumulator::add;
  }

  @Override public BinaryOperator<DurationStatsAccumulator> combiner() {
    return DurationStatsAccumulator::combine;
  }

  @Override public Function<DurationStatsAccumulator, DurationStats> finisher() {
    return DurationStatsAccumulator::finish;
  }

  @Override public Set<Characteristics> characteristics() {
    return Set.of(Characteristics.UNORDERED);
  }
}
```

### Результат

```text
custom sequential: DurationStats[count=6, sum=80, min=8, max=20], avg=13.333333333333334
custom parallel:   DurationStats[count=6, sum=80, min=8, max=20], avg=13.333333333333334
combined parts:    DurationStats[count=6, sum=80, min=8, max=20]
built-in:          count=6, sum=80, min=8, max=20, avg=13.33
custom empty:      DurationStats[count=0, sum=0, min=0, max=0]
```

Последовательный, параллельный и вручную объединённый результаты совпадают. Это подтверждает корректность `combiner` и отсутствие зависимости от разбиения потока.

---

## Задание 7. Однопроходный отчёт CourseHub

### Формулировка

Из большого потока enrollment-событий сформируйте уникальных студентов, completion rate по курсам, top-3, распределение по `level` и список аномалий. Данные могут поступать только один раз. Результат детерминирован, память ограничена требованиями, side effects отсутствуют. Объясните, где Stream API уместен, а где обычный цикл яснее.

### Алгоритм решения

1. Определить событие `EnrollmentEvent(studentId, courseCode, level, type)`, где `type` — `ENROLLED`, `COMPLETED` или `DROPPED`.
2. Создать аккумулятор, который за один проход поддерживает:
   - `Set<Long> students` — уникальные студенты, у которых было хотя бы одно зачисление;
   - `TreeMap<String, CourseState>` — состояние по каждому курсу;
   - `Set<Long> enrolled` и `Set<Long> completed` внутри `CourseState`;
   - `EnumMap<Level, Long>` — количество зачислений по уровням;
   - ограниченный список аномалий и их общее количество;
   - счётчик событий для нумерации аномалий.
3. В `accept(event)` обработать событие по типу:
   - `ENROLLED` — создать состояние курса при первом событии, проверить несовпадение `level`, добавить студента; повторное зачисление считать аномалией;
   - `COMPLETED` — засчитать завершение только если студент был зачислен; повторное завершение считать аномалией;
   - `DROPPED` — проверить, что студент был зачислен и ещё не завершил курс.
4. Для `null`-события зафиксировать аномалию и продолжить обработку.
5. После единственного прохода вычислить `completionRate = completed / enrolled` по каждому курсу.
6. Построить top-3: сортировка по `completionRate` по убыванию, при равенстве — по `courseCode` по возрастанию, затем `limit(3)`.
7. Вернуть неизменяемый `CourseHubReport`: карты копируются в `TreeMap`/`EnumMap`, списки — через `List.copyOf`.
8. В демонстрации создать ленивый поток из 20 000 базовых событий и нескольких намеренных аномалий, передать его в анализатор ровно один раз.

### Основные концепты

- **Однопроходность.** Поток нельзя «перемотать». Если после первого `collect` попытаться снова обойти тот же `Stream`, получим `IllegalStateException`. Поэтому все метрики нужно накапливать за один проход.
- **Ограниченная память.** Мы не храним все события. Память зависит от количества уникальных студентов, курсов и уникальных пар «студент–курс», а не от длины потока. Список аномалий ограничен `anomalyLimit`; общее число аномалий хранится отдельным счётчиком.
- **Уникальные студенты.** `Set<Long>` автоматически убирает повторных студентов. Студент добавляется только при успешном зачислении, поэтому «завершение без зачисления» не создаёт фиктивного студента.
- **Completion rate.** Это доля уникальных завершивших от уникальных зачисленных: `completed.size() / enrolled.size()`. Если курс не встречался, его нет в отчёте; если зачислений нет, деление не выполняется.
- **Top-3.** Сортировка должна иметь полный набор tie-breakers. Иначе при равных rates порядок зависел бы от обхода `TreeMap`. Здесь второй ключ — `courseCode`.
- **Распределение по level.** `EnumMap<Level, Long>` хранит количество успешных зачислений по уровням. `EnumMap` компактен и обходит ключи в порядке объявления `enum`.
- **Аномалии.** В отчёте фиксируются: `null`-событие, повторное зачисление, завершение без зачисления, повторное завершение, удаление без зачисления, удаление после завершения, несовпадение уровня курса. Список ограничен, но `totalAnomalies` показывает реальное количество.
- **Детерминизм.** `TreeMap` для курсов и rates, `EnumMap` для уровней, явный tie-breaker в top-3, нумерация аномалий по позиции в потоке. При одном и том же входном потоке результат одинаков.
- **Side effects.** Анализатор ничего не печатает, не пишет в файлы и не меняет статические поля. Изменяется только локальный аккумулятор внутри `analyze`. Печать вынесена в `Task7Demo`.
- **Где Stream API уместен.** `map`, `filter`, `flatMap`, `sorted`, `limit`, `collect` хорошо подходят для декларативных преобразований и агрегаций без сложного ветвления. В задаче 2 и задаче 4 поток делает код короче и яснее.
- **Где обычный цикл яснее.** В задаче 7 внутри `accept` много ветвлений, проверок и обновлений нескольких структур. Обычный `switch` по типу события читается проще, чем цепочка `filter`/`peek` с побочными эффектами. Поэтому здесь Stream API используется для единственного обхода (`forEachOrdered`), а накопление состояния реализовано явно. Это осознанный компромисс: не «всё должно быть стримом», а «стрим там, где он улучшает читаемость».

### Важный листинг (фрагмент)

Обработка события:

```java
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
```

Единственный проход и построение top-3:

```java
Accumulator accumulator = new Accumulator(anomalyLimit);
events.forEachOrdered(accumulator::accept);
return accumulator.toReport();

// внутри toReport():
List<CourseHubReport.CourseCompletion> top3 = completions.stream()
    .sorted(Comparator.comparingDouble(CourseHubReport.CourseCompletion::completionRate)
        .reversed()
        .thenComparing(CourseHubReport.CourseCompletion::courseCode))
    .limit(3)
    .toList();
```

### Результат

```text
unique students: 2000
completion rate:
  ALGO-201 0.667
  ARCH-501 0.667
  DB-301 0.667
  JAVA-101 0.667
  WEB-401 0.667
top-3:
  ALGO-201 0.667 (1334/2000)
  DB-301 0.667 (1334/2000)
  ARCH-501 0.667 (1333/2000)
enrollments by level: {BASIC=4000, INTERMEDIATE=4000, ADVANCED=2000}
anomalies: 7 (shown 7)
  #20001 JAVA-101 student=1: duplicate enrollment
  #20003 ALGO-201 student=2: duplicate completion
  #20004 DB-301 student=3: level mismatch
  #20004 DB-301 student=3: duplicate enrollment
  #20005 JAVA-101 student=9999: completion without enrollment
  #20006 JAVA-101 student=9999: drop without enrollment
  #20007  student=0: null event
```

Отчёт показывает 2000 уникальных студентов, completion rate по каждому курсу, top-3 с детерминированным разрешением ничьих, распределение зачислений по уровням и все обнаруженные аномалии. При увеличении потока список событий не хранится, поэтому память растёт только за счёт уникальных студентов и пар «студент–курс».

---

## Итоги

| Задание | Ключевые API | Главная идея |
|---|---|---|
| 1 | `Comparator`, `thenComparing`, `sort` | Полный порядок и явные tie-breakers дают детерминизм |
| 2 | `flatMap`, `distinct`, `groupingBy` | Вложенные структуры разворачиваются без ручных списков |
| 3 | `reduce` | Identity должна быть нейтральной, accumulator — ассоциативным |
| 4 | `groupingBy`, `teeing`, `counting`, `summingLong` | Один обход и один summary object вместо трёх проходов |
| 5 | `Optional`, `map`, `orElse`, `orElseGet` | Отсутствие результата выражается типом, а не `null`/`get` |
| 6 | `Collector`, `supplier`/`accumulator`/`combiner`/`finisher` | Локальный mutable accumulator и immutable result |
| 7 | `Stream`, `forEachOrdered`, `TreeMap`, `EnumMap` | Однопроходная агрегация с ограниченной памятью и без side effects |

Все задания вместе показывают базовый набор инструментов функциональной обработки коллекций: преобразование, фильтрацию, сортировку, свёртку, группировку, поиск с отсутствующим результатом и собственную агрегацию. При этом Stream API не является самоцелью: в местах со сложным ветвлением и несколькими счётчиками обычный `switch`/цикл остаётся более читаемым и явным.
