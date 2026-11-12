# Orders Lab

A hands-on Quarkus microservice project built with Java 21 to explore Quarkus core architecture, build-time Context and Dependency Injection (ArC), configuration management, and reactive enterprise patterns.



## Implemented Concepts & Technical Reference

### 1. Modern Domain Modeling with Java Records

In `Order.java`:

```java
public record Order(String id, String item, int quantity) {}
```

- **Concept**: A `record` is an immutable data carrier introduced in modern Java.
- **Why use it**:
  - Automatically generates final fields, canonical constructor, accessors (`id()`, `item()`, `quantity()`), `equals()`, `hashCode()`, and `toString()`.
  - Eliminates boilerplate code and prevents accidental state mutation.
  - Acts as an optimal Data Transfer Object (DTO) for JSON serialization/deserialization with Jackson.

---

### 2. Contexts and Dependency Injection (CDI) via ArC

Quarkus uses **ArC**, a build-time CDI implementation based on Jakarta CDI specifications. Unlike traditional frameworks that rely heavily on runtime reflection and classpath scanning, ArC performs dependency resolution and proxy generation during compilation, leading to instant startup and minimal memory overhead.

#### Bean Scopes
```java
@ApplicationScoped
public class OrderService { ... }
```

- **`@ApplicationScoped`**:
  - Creates a single shared instance for the entire application.
  - Uses a **client proxy** — classes calling `OrderService` interact with a synthetic proxy wrapper that lazily delegates calls to the underlying instance.
  - Ideal for stateless or centrally managed stateful services.
- **`@Singleton` vs `@ApplicationScoped`**:
  - `@Singleton` does not use a client proxy; the client holds a direct reference to the instance. It is slightly faster to invoke but does not support certain CDI interception features or lazy loading like `@ApplicationScoped`.
- **`@RequestScoped`**:
  - Instantiates a fresh bean per HTTP request and destroys it when the request terminates. Useful for request-specific state (e.g., authentication context, request traces).

#### Injection Strategies: Constructor vs Field Injection

```java
private final int maxItems;

public OrderService(@ConfigProperty(name = "orders.max-items") int maxItems) {
    this.maxItems = maxItems;
}
```

- **Why Constructor Injection is preferred**:
  - Enforces field immutability (`final`).
  - Guarantees the object cannot be instantiated in an invalid/incomplete state.
  - Simplifies unit testing because dependencies can be passed directly into the constructor without requiring reflection or mock injectors.
  - In Quarkus/ArC, if a bean has a single constructor, the `@Inject` annotation is optional.

#### Application Lifecycle Observation

```java
void onStart(@Observes Startup event) {
    LOG.infof("OrderService started. Maximum items allowed per order: %d", maxItems);
}
```

- **Concept**: `@Observes` registers an event listener for CDI lifecycle events.
- **Execution Timing**: When Quarkus completes dependency wiring and the runtime boots, a `Startup` event is fired.
---

### 3. MicroProfile Configuration & Profiles
in `src/main/resources/application.properties`.

#### Profile Prefixes
```properties
# Default / Production value
orders.max-items=10

# Development mode override
%dev.orders.max-items=100
```

- **Concept**: Quarkus provides built-in configuration profiles:
  - `%dev`: Applied automatically when running `./mvnw quarkus:dev`.
  - `%test`: Applied automatically during `mvn test`.
  - `%prod`: Default profile applied when packaged and run as a jar or container image.
- **Advantage**: Different runtime behaviors and thresholds can be set without modifying source code or managing separate property files.

#### Property Injection (`@ConfigProperty`)
- Config values are injected directly into constructors or fields using `@ConfigProperty(name = "...")`.
- Used in `OrderService` to dynamically enforce the maximum allowed quantity on orders based on the active profile.

---

### 4. Thread-Safe In-Memory Persistence & Guard Logic


```java
private final Map<String, Order> database = new ConcurrentHashMap<>();

public void saveOrder(Order order) {
    if (order.quantity() > maxItems) {
        throw new IllegalArgumentException("Order exceeds maximum items limit of " + maxItems);
    }
    database.put(order.id(), order);
    LOG.infof("Order %s saved.", order.id());
}
```

- **`ConcurrentHashMap`**: Uses bucket-level locking and lock-free reads to handle multi-threaded concurrent requests safely without blocking the entire data structure.
- **Domain Guard**: Business constraints are validated against the injected config limit before persisting records.

---

### 5. Logging with JBoss Logger

```java
private static final Logger LOG = Logger.getLogger(OrderService.class);
LOG.infof("Order %s saved.", order.id());
```

- Uses `org.jboss.logging.Logger` (standard in Quarkus).
- Supports formatted string logging (`infof`, `debugf`, `errorf`) without manual string concatenation, reducing object allocations when logging is disabled.


## Tooling & Development Workflow

### Dev Mode (Live Coding)
```bash
mvn quarkus:dev
```
- Listens on port `8080`.
- Code changes in Java classes or `application.properties` are recompiled on the fly on the next HTTP request (live reload).
- Terminal shortcuts:
  - `h`: Help / list shortcuts.
  - `r`: Re-run automated tests.
  - `d`: Open the Quarkus Dev UI in your browser (`http://localhost:8080/q/dev-ui`).

### Packaging & Running the Artifact
```bash
# Package into fast-jar format
mvn package

# Run the packaged application
java -jar target/quarkus-app/quarkus-run.jar
```
- Quarkus produces a thin runnable jar inside `target/quarkus-app/` with dependencies organized in `lib/` for faster container layer caching.

---

## Implementation Checklist

Target requirements for completing `orders-lab`:

- [x] **Project initialization**: Maven layout, Java 21, Quarkus 3.40.1.2 BOM.
- [x] **Domain definition**: `Order` record with `id`, `item`, `quantity`.
- [x] **CDI Service**: `@ApplicationScoped` `OrderService` with constructor injection.
- [x] **Startup event observer**: `@Observes Startup` for startup initialization and logging.
- [x] **Config profiles**: `orders.max-items` with `%dev` override injected via `@ConfigProperty`.
- [x] **In-memory data store**: Thread-safe `ConcurrentHashMap` with validation logic.
- [ ] **REST Endpoints (`OrderResource`)**:
  - `POST /orders`: Accepts JSON payload, returns HTTP `201 Created`.
  - `GET /orders`: Returns collection of all orders.
  - `GET /orders/{id}`: Returns order details, or HTTP `404 Not Found` if missing.
- [ ] **Payload Validation**: Integrate `quarkus-hibernate-validator` (`@Valid`, `@NotBlank`, `@Positive`).
- [ ] **Observability**: Health checks via `/q/health` (`quarkus-smallrye-health`) and Swagger UI via `/q/swagger-ui`.
- [ ] **Automated Tests**: Unit and integration tests using `@QuarkusTest` and RestAssured (`mvn test`).
- [ ] **Data Persistence with Panache**: Transition from in-memory map to PostgreSQL database using Hibernate ORM with Panache.
