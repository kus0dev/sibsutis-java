# Задание 1. `Callable` и `Future`

## `Runnable` и `Callable`

| | `Runnable` | `Callable<V>` |
|---|---|---|
| Результат | нет, `run()` возвращает `void` | есть, `call()` возвращает `V` |
| Checked exception | нельзя пробросить | можно, `call() throws Exception` |
| Запуск через executor | `execute` / `submit` | `submit` |

`submit(Runnable)` возвращает `Future<?>`, у которого `get()` всегда даёт `null`.
`submit(Callable)` возвращает `Future<V>` с настоящим результатом. Checked
exception из `Callable` не теряется: `Future.get()` выбрасывает
`ExecutionException`, внутри которого лежит исходная причина.

## Timeout и ошибка вычисления

Это разные события:

- `TimeoutException` — бюджет ожидания закончился, но задача не обязана быть
  сломанной: она может продолжать работать. В демонстрации после таймаута
  `slow.isDone()` равно `false`.
- `ExecutionException` — вычисление завершилось исключением. Причина доступна
  через `getCause()`.
- `CancellationException` — `Future` отменён, результата не будет.

`Future.get(timeout, unit)` ждёт не дольше указанного времени. Повторный вызов
`get` не продолжает старое ожидание, а начинает новое. Поэтому после таймаута
нельзя просто «подождать ещё»: нужно либо вычислить новый бюджет от монотонных
часов, либо отменить задачу и вернуть ошибку. В демонстрации выбран второй путь.

## Отмена

`cancel(true)` посылает interruption потоку, который выполняет задачу. Это
**запрос**, а не принудительное убийство:

- если поток стоит в `sleep`, `wait`, `take`, `join` или другом блокирующем
  вызове, тот бросит `InterruptedException`;
- если поток крутится в обычном цикле, он сам обязан проверять
  `Thread.currentThread().isInterrupted()`;
- если код игнорирует флаг, JVM не сможет его остановить. Именно поэтому
  `Thread.stop()` удалён: он оставлял данные в несогласованном состоянии.

`InterruptibleTask` ловит `InterruptedException`, восстанавливает флаг через
`Thread.currentThread().interrupt()` и закрывает ресурс в `finally`. Восстановление
флага важно: если исключение нельзя пробросить дальше, следующий уровень кода
должен увидеть, что поток прерывали.

После `cancel(true)` `Future.get()` сразу бросает `CancellationException`, не
дожидаясь фактической остановки задачи. Поэтому демонстрация ждёт отдельный
сигнал `finished` из `finally`: так видно, что ресурс действительно закрыт.

## Результат

```text
Runnable vs Callable:
  runnable.get() = null
  callable.get() = Callable: result is a value
timeout and failure:
  TimeoutException: task still running=true
  ExecutionException: cause=catalog is unavailable
cancellation:
  cancel(true)=true, stopped=true, resourceClosed=true
```

Пул завершается в `finally`: `shutdown`, ограниченное `awaitTermination`, при
неудаче — `shutdownNow` и ещё одно ожидание.
