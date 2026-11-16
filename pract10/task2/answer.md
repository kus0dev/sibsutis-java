# Задание 2. Последовательное преобразование

## Цепочка стадий

`supplyAsync` запускает загрузку курса в указанном executor'е. `thenApply`
преобразует результат, `thenAccept` — конечный потребитель, который ничего не
возвращает. Итоговый `CompletableFuture<Void>` ждём через `join()`.

В демонстрации используется пул `io-1`, `io-2`, ...: это отдельный executor для
блокирующих операций. Caller — `main` — только собирает цепочку и ждёт результат.

```text
card chain (caller=main):
  [io-1] supplyAsync: load course
  [io-1] thenApply: format card
  [io-1] thenAccept: print card
  JAVA-101 · Основы Java — Иван Петров, 720 мин
```

`thenApply` и `thenAccept` без суффикса `Async` выполняются в том потоке, который
завершил предыдущую стадию. Здесь это `io-1`, потому что `supplyAsync` уже занял
его. Если future к моменту подписки уже завершён, стадия выполнится в потоке,
который вызвал `thenApply`/`thenAccept`. `thenApplyAsync`/`thenAcceptAsync` явно
отправляют стадию в executor и не дают ей случайно выполниться в caller'е.

`Thread.sleep` в демонстрации изображает ожидание сети или диска. Это не
CPU-работа: поток в это время заблокирован.

## `thenApply` и `thenCompose`

`thenApply(Function<T, U>)` строит `CompletableFuture<U>`. Если сама функция
возвращает `CompletableFuture`, получается вложенная структура
`CompletableFuture<CompletableFuture<...>>`.

`thenCompose(Function<T, CompletionStage<U>>)` делает то же самое, но
разворачивает результат: получается плоский `CompletableFuture<U>`. Поэтому
загрузка записей после пользователя оформлена так:

```java
CompletableFuture<User> user = CompletableFuture.supplyAsync(..., io);
CompletableFuture<List<Enrollment>> enrollments = user.thenCompose(value ->
    CompletableFuture.supplyAsync(() -> loadEnrollments(value.id()), io));
```

Тип `CompletableFuture<List<Enrollment>>` плоский: одна стадия — один список
записей. `thenCompose` вызывается в потоке, завершившем `user` (`io-2`), а
внутренний `supplyAsync` уходит в executor и выполняется в `io-3`.

```text
enrollment chain (caller=main):
  [io-2] supplyAsync: load user
  [io-2] thenCompose: switch to enrollments
  [io-3] supplyAsync: load enrollments
  user=Денис, enrollments=2
    JAVA-101 progress=40%
    DB-301 progress=15%
```

## Правило выбора

- Нужно преобразовать значение без асинхронной операции — `thenApply`.
- Нужно запустить ещё одну асинхронную загрузку и не плодить вложенные future —
  `thenCompose`.
- Нужно просто выполнить побочное действие с результатом — `thenAccept`.

Ошибка на любой стадии автоматически завершает всю цепочку исключительно;
`join()` бросит `CompletionException` с исходной причиной.
