

## Reactive Programming

Reactive programming is a programming approach for working with
asynchronous operations, events, and streams of data.

With non-blocking I/O, a thread does not need to remain occupied while that operation is pending; it can handle
other available work.

Reactive programming is useful for workloads with many concurrent I/O
operations, streaming data, and real-time updates. It does not
automatically make database queries or CPU-intensive work faster.

## Synchronous, asynchronous, and reactive

-   **Synchronous:** Operations generally execute in sequence. A caller
    waits for an operation to finish before continuing.
-   **Asynchronous:** An operation can be started and its result handled
    later. The current thread can do other work while the operation is
    pending.
-   **Reactive:** Uses abstractions for data streams, event propagation,
    transformations, error handling, and backpressure.

These terms are related, but they do not mean the same thing.


The event loop does not need to sit and wait for every I/O operation to
finish.

## Event-loop threads, worker threads, and the main thread

These are different roles:

-   **Main thread:** Starts the application and performs startup work.
    It is not the same as a worker thread.
-   **I/O (event-loop) thread:** Handles non-blocking network operations
    and reactive request processing.
-   **Worker thread:** Used for work that may block, such as synchronous
    or blocking operations.


## Quarkus REST execution

Quarkus REST chooses an execution model for endpoint methods. When an HTTP request hits, Vert.x accepts tcp connect on an Event Loop thread (named usually vert.x.eventloop-thread-0). How quarkus treats each method depends on the REST endpoint definition
### Reactive endpoint returning `Uni`

``` java
@GET
@Path("/reactive")
public Uni<String> reactive() {
    return Uni.createFrom().item("Hello Reactive");
}
```

A REST endpoint returning `Uni` a nd doesn't have a `@Blocking` annotation means it is writing a reactive non-blocking code. so it will be ran directly
on the even-loop thread without switching to a separate worker thread. But event loops are meant to run multiple requests at a time so putting it to sleep using `Thread.sleep()` freezes the thread completely. <br>

As Vert.x has a background watcher `BlockedThreadChecker` checks the event loop and raises a warning in the log if *any event-loop thread has been stuck for more thatn 2000ms*

### Blocking endpoint using `@Blocking`

``` java
@GET
@Path("/blocking")
@Blocking
public String blocking() throws InterruptedException {
    Thread.sleep(5000);
    return "Completed";
}
```

`@Blocking` tells Quarkus REST to dispatch the endpoint method to a
separate worker thread (executor-thread-0)

Important: `@Blocking` does not make the operation asynchronous. It
moves blocking work off the event-loop thread.

### The main thread is not involved in an automatic switch

Quarkus does not switch an endpoint to the application's main thread
when a request arrives or when a `Uni` completes. Quarkus manages
request dispatching and response completion.


## Blocking inside a `Uni`

This example is still blocking:

``` java
public Uni<String> reactive() {
    return Uni.createFrom().item(() -> {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return "Hello Reactive";
    });
}
```

The supplier runs when the `Uni` is subscribed to. If Quarkus subscribes
on an I/O thread, `Thread.sleep()` blocks that I/O thread.

Wrapping blocking code in a `Uni` does not automatically make the code
non-blocking.

A blocked event loop can cause warnings such as:

``` text
Thread ... has been blocked for ...
```

This indicates that an event-loop thread has been blocked longer than
the configured threshold.


## Reactive system principles

The Reactive Manifesto describes four characteristics of reactive
systems:

-   **Responsive:** Respond in a timely and consistent manner.
-   **Resilient:** Remain responsive when components fail.
-   **Elastic:** Adapt resource usage to changing workload.
-   **Message-driven:** Components communicate through asynchronous
    message passing.

These describe system characteristics; they are not simply a list of
Mutiny operators.

## Backpressure

Backpressure is a way for a consumer to control how much data it
receives from a producer.

Example: - Producer emits 10,000 messages per second. - Consumer can
process 1,000 messages per second.

Without flow control, data can accumulate, increasing memory usage and
latency. A backpressure strategy manages the rate or amount of data
flowing downstream. Depending on the system, this can involve requesting
fewer items, buffering, or dropping items according to a defined policy.

## Fundamental Mutiny Concepts

1.  `Uni<T>` versus `Multi<T>`.
2.  Lazy execution and subscription.
3.  `map()` for synchronous transformations.
4.  `chain()` when the next step returns a `Uni`.
5.  `invoke()` for synchronous side effects such as logging.
6.  `call()` for an asynchronous side effect.
7.  `onItem().ifNull()` for handling a null item.
8.  Failure handling with `onFailure()`.
9.  `recoverWithItem()` and `recoverWithUni()`.
10. `retry()` and timeouts.
11. Combining independent operations with
    `Uni.combine().all().unis(a, b).asTuple()`.
12. Reactive database access and reactive REST resources.

## 

-   An event loop coordinates asynchronous work; it is not the same as a
    worker thread.
-   Quarkus decides how REST endpoint methods are dispatched.
-   `@Blocking` moves endpoint execution to a worker thread.
-   `Uni` represents an asynchronous result; it does not automatically
    create a thread.
-   A `Uni` is lazy and runs when subscribed to.
-   Blocking code inside a `Uni` can still block the event-loop thread.
-   Non-blocking I/O improves resource utilization; it does not make the
    underlying I/O operation itself faster.
-   Reactive programming is useful when the workload benefits from
    asynchronous I/O and stream processing. It is not automatically the
    best choice for every application.

