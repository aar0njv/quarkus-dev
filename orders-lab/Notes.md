# Orders Lab — Comprehensive Study Notes & Technical Reference

This document serves as an exhaustive reference guide covering both **modern Java (Java 21) fundamentals** and **Quarkus core architectural patterns** implemented in the `orders-lab` project.

---

## Table of Contents
1. [Modern Java Fundamentals (Java 21)](#1-modern-java-fundamentals-java-21)
   - [Java Records (Immutable Data Carriers)](#java-records-immutable-data-carriers)
   - [Thread Safety & In-Memory Concurrency](#thread-safety--in-memory-concurrency)
   - [Domain Guards & Exception Handling](#domain-guards--exception-handling)
2. [Quarkus Core Architecture & ArC (CDI)](#2-quarkus-core-architecture--arc-cdi)
   - [Build-Time vs. Runtime Dependency Injection](#build-time-vs-runtime-dependency-injection)
   - [Bean Scopes: `@ApplicationScoped` vs. Others](#bean-scopes-applicationscoped-vs-others)
   - [Dependency Injection: Constructor Injection vs. Field Injection](#dependency-injection-constructor-injection-vs-field-injection)
   - [Application Lifecycle Observers (`@Observes Startup`)](#application-lifecycle-observers-observes-startup)
   - [Structured Logging with JBoss Logger](#structured-logging-with-jboss-logger)
3. [Configuration Management: `@ConfigProperty` vs. `@ConfigMapping`](#3-configuration-management-configproperty-vs-configmapping)
   - [Individual Property Injection (`@ConfigProperty`)](#individual-property-injection-configproperty)
   - [Structured Hierarchical Configuration (`@ConfigMapping`)](#structured-hierarchical-configuration-configmapping)
   - [Configuration Profile Overrides (`%dev`, `%test`, `%prod`)](#configuration-profile-overrides-dev-test-prod)
4. [RESTful API Design & HTTP Routing (JAX-RS / Quarkus REST)](#4-restful-api-design--http-routing-jax-rs--quarkus-rest)
   - [REST Resource Architecture](#rest-resource-architecture)
   - [Complete CRUD Mapping Matrix](#complete-crud-mapping-matrix)
   - [The `Response` Builder & HTTP Status Semantics](#the-response-builder--http-status-semantics)
   - [URL Parameters: `@PathParam` vs. `@QueryParam`](#url-parameters-pathparam-vs-queryparam)
   - [Critical JAX-RS Routing Rule (Ambiguous Route Avoidance)](#critical-jax-rs-routing-rule-ambiguous-route-avoidance)
5. [API Architecture: Request & Response DTOs](#5-api-architecture-request--response-dtos)
   - [What are DTOs and Why Separate Them?](#what-are-dtos-and-why-separate-them)
   - [Preventing "Mass Assignment" (Over-Posting) Vulnerabilities](#preventing-mass-assignment-over-posting-vulnerabilities)
   - [Decoupling API Contracts from Persistence Schemas](#decoupling-api-contracts-from-persistence-schemas)
6. [Jakarta Bean Validation (`quarkus-hibernate-validator`)](#6-jakarta-bean-validation-quarkus-hibernate-validator)
   - [Declarative Validation Annotations](#declarative-validation-annotations)
   - [Cascading Validation with `@Valid`](#cascading-validation-with-valid)
   - [Handling Optional vs. Mandatory Attributes](#handling-optional-vs-mandatory-attributes)
7. [Centralized Error Handling with `@ServerExceptionMapper`](#7-centralized-error-handling-with-serverexceptionmapper)
   - [The Problem with Scattered `try-catch` Blocks](#the-problem-with-scattered-try-catch-blocks)
   - [Enforcing One Consistent JSON Error Shape (RFC 7807 Pattern)](#enforcing-one-consistent-json-error-shape-rfc-7807-pattern)
   - [How `@ServerExceptionMapper` Works in Quarkus REST](#how-serverexceptionmapper-works-in-quarkus-rest)
   - [Modern Quarkus REST (`quarkus-rest`) vs. Legacy RESTEasy Classic](#modern-quarkus-rest-quarkus-rest-vs-legacy-resteasy-classic)
8. [Client & Terminal Networking: URL Encoding & Shell Parsing](#8-client--terminal-networking-url-encoding--shell-parsing)
   - [RFC 3986 Space Restriction in URLs](#rfc-3986-space-restriction-in-urls)
   - [Shell Argument Splitting with `curl`](#shell-argument-splitting-with-curl)
9. [Automated Testing with `@QuarkusTest` & REST-assured](#9-automated-testing-with-quarkustest--rest-assured)
   - [Integration Testing Architecture](#integration-testing-architecture)
   - [Given-When-Then Fluent Testing Syntax](#given-when-then-fluent-testing-syntax)
   - [Test Ordering with `@TestMethodOrder`](#test-ordering-with-testmethodorder)
10. [Quarkus Developer Workflow & Packaging](#10-quarkus-developer-workflow--packaging)
    - [Live Coding / Dev Mode](#live-coding--dev-mode)
    - [Packaging & Production Runner Layout](#packaging--production-runner-layout)

---

## 1. Modern Java Fundamentals (Java 21)

### Java Records (Immutable Data Carriers)
In `Order.java`:
```java
public record Order(
    String id,
    @NotBlank(message = "Item name cannot be blank") String item,
    @Positive(message = "Quantity must be greater than 0") int quantity
) {}
```

#### What is a Record?
Introduced in Java 14 and standardized in Java 16, a `record` is a compact class designed to be a transparent, shallowly immutable carrier for data.

#### Key Characteristics & Under-the-Hood Mechanics:
1. **Zero Boilerplate**: The compiler automatically generates:
   - Private `final` fields for each component (`id`, `item`, `quantity`).
   - A public canonical constructor accepting all components.
   - Public accessor methods with modern naming (e.g., `order.item()` and `order.quantity()`, **not** `getItem()` or `getQuantity()`).
   - Correct implementations of `equals()`, `hashCode()`, and `toString()`.
2. **Immutability by Design**: Records prevent accidental state mutation after construction. There are no setters.
3. **DTO Suitability**: Records are ideal Data Transfer Objects (DTOs). Jackson serializes and deserializes them natively into JSON without requiring custom annotations.

---

### Thread Safety & In-Memory Concurrency
In `OrderService.java`:
```java
private final Map<String, Order> database = new ConcurrentHashMap<>();
```

#### Why `ConcurrentHashMap` instead of `HashMap`?
Web applications are inherently multi-threaded. Each incoming HTTP request is handled by an independent worker thread.
- **`HashMap`**: Non-thread-safe. Concurrent writes can corrupt internal data structures or cause race conditions and lost updates.
- **`Collections.synchronizedMap()`**: Thread-safe, but locks the entire map on every operation. When thread A writes, all other threads (even readers) must block and wait.
- **`ConcurrentHashMap`**: Uses fine-grained bucket/segment locking and lock-free reads via volatile memory reads and Compare-And-Swap (CAS) instructions. Multiple reader and writer threads can access different buckets concurrently without blocking one another.

---

### Domain Guards & Exception Handling
In `OrderService.java`:
```java
public void createOrder(Order order) {
    if (order.quantity() > maxItems) {
        throw new IllegalArgumentException("Order exceeds maximum items limit of " + maxItems);
    }
    database.put(order.id(), order);
    LOG.infof("Order %s saved.", order.id());
}
```

- **Guard Clause**: Validates business invariants at the entry of the method before modifying state.
- **Unchecked Exceptions**: `IllegalArgumentException` is an unchecked exception (`RuntimeException`). It signals that a caller passed an illegal argument.
- With modern exception mapping (covered in [Section 7](#7-centralized-error-handling-with-serverexceptionmapper)), we let domain exceptions bubble up naturally rather than cluttering our code with repetitive `try-catch` blocks.

---

## 2. Quarkus Core Architecture & ArC (CDI)

### Build-Time vs. Runtime Dependency Injection
Traditional Java enterprise frameworks perform reflection, classpath scanning, and proxy generation at **runtime** when booting. This causes high memory consumption and slow startups.

**Quarkus's ArC Engine**:
- Evaluates beans, interceptors, and configuration bindings **at build time** (compile time).
- Eliminates reflection by generating synthetic bytecode classes directly.
- Fails fast at build time if a dependency is missing or ambiguous.
- Enables lightning-fast startup and native compilation with GraalVM.

---

### Bean Scopes: `@ApplicationScoped` vs. Others

| Scope | Annotation | Lifecycle & Behavior | Proxy Used? |
| :--- | :--- | :--- | :---: |
| **Application** | `@ApplicationScoped` | Single instance created lazily for the entire life of the application. | **Yes** (Client Proxy) |
| **Singleton** | `@Singleton` | Single instance eagerly created; direct reference held by callers. | **No** |
| **Request** | `@RequestScoped` | Fresh instance created per HTTP request; destroyed when request terminates. | **Yes** (Client Proxy) |
| **Dependent** | `@Dependent` | Default scope when none specified. Lifecycle tied to the parent bean. | **No** |

#### Why does `@ApplicationScoped` use a Client Proxy?
Callers interact with a generated synthetic proxy (`OrderService_ClientProxy`) that lazily delegates calls to the real bean. This enables lazy initialization, CDI method interception, and seamless mocking in tests.

---

### Dependency Injection: Constructor Injection vs. Field Injection

#### Preferred (Constructor Injection):
```java
@ApplicationScoped
public class OrderService {
    private final int maxItems;

    public OrderService(@ConfigProperty(name = "orders.max-items") int maxItems) {
        this.maxItems = maxItems;
    }
}
```

#### Why Constructor Injection is the Industry Best Practice:
1. **Enforces Immutability**: Injected dependencies can be marked `final`.
2. **Prevents Incomplete State**: The compiler guarantees an object cannot be instantiated without supplying its dependencies.
3. **Pure Unit Testing**: The class can be tested in isolation with plain `new OrderService(50)` without spinning up a CDI container or using reflection mocks.
4. **No `@Inject` Annotation Required**: In ArC, if a class has a single constructor, `@Inject` is implicit and optional.

---

### Application Lifecycle Observers (`@Observes Startup`)
In `OrderService.java`:
```java
void onStart(@Observes Startup event) {
    LOG.infof("OrderService started. Maximum items allowed per order: %d", maxItems);
}
```
- In CDI, `@Observes` registers an event listener method.
- When Quarkus finishes wiring dependencies and boots the runtime, it fires the `jakarta.enterprise.event.Startup` event for deterministic startup initialization.

---

### Structured Logging with JBoss Logger
```java
private static final Logger LOG = Logger.getLogger(OrderService.class);
LOG.infof("Order %s saved.", order.id());
```
- Uses `org.jboss.logging.Logger` (standard in Quarkus).
- Formatted log methods (`infof`, `debugf`, `errorf`) avoid manual string concatenation (`"Order " + id + " saved"`), eliminating string allocations when log levels are disabled.

---

## 3. Configuration Management: `@ConfigProperty` vs. `@ConfigMapping`

Quarkus provides two approaches for injecting values from `application.properties`:

### Individual Property Injection (`@ConfigProperty`)
* **Import**: `org.eclipse.microprofile.config.inject.ConfigProperty`
* **Use Case**: Best for 1 or 2 standalone, flat configuration keys.

```java
@ConfigProperty(name = "orders.max-items", defaultValue = "10")
int maxItems;
```

---

### Structured Hierarchical Configuration (`@ConfigMapping`)
* **Import**: `io.smallrye.config.ConfigMapping`
* **Use Case**: Best for grouping 3+ related properties, nested trees, and validating configuration schemas at build time.

```java
@ConfigMapping(prefix = "orders")
public interface OrderConfig {
    @WithDefault("10")
    @Positive
    int maxItems();

    String storeName();

    Retry retry();

    interface Retry {
        @WithDefault("3")
        int maxAttempts();
        @WithDefault("500ms")
        Duration delay();
    }
}
```

#### Injected Cleanly as a Single Bean:
```java
@ApplicationScoped
public class OrderService {
    private final OrderConfig config;

    public OrderService(OrderConfig config) {
        this.config = config;
    }
}
```

#### Comparison Matrix:

| Feature | `@ConfigProperty` | `@ConfigMapping` |
| :--- | :--- | :--- |
| **Standard** | Eclipse MicroProfile Config | SmallRye Config / Quarkus Native |
| **Granularity** | Single flat property per annotation | Entire prefix / hierarchical tree |
| **Constructor Impact** | Clutters constructor when properties grow | Single clean typed object |
| **Bean Validation** | ❌ Not built-in | ✅ Supports `@Positive`, `@NotBlank` on getters |
| **Nesting & Groups** | ❌ Flat keys only | ✅ Supports nested sub-interfaces and Maps |
| **Default Values** | `defaultValue = "..."` attribute | `@WithDefault("...")` annotation |

---

### Configuration Profile Overrides (`%dev`, `%test`, `%prod`)
In `application.properties`:
```properties
# Default (Production) value
orders.max-items=10

# Development mode override
%dev.orders.max-items=100
```
- **`%dev`**: Active automatically when running `./mvnw quarkus:dev`.
- **`%test`**: Active automatically during `./mvnw test`. Falls back to default if `%test` is omitted.
- **`%prod`**: Default active profile when packaged into a runnable JAR or container image.

---

## 4. RESTful API Design & HTTP Routing (JAX-RS / Quarkus REST)

### REST Resource Architecture
In `OrderResource.java`:
```java
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {
    private final OrderService orderService;

    public OrderResource(OrderService orderService) {
        this.orderService = orderService;
    }
}
```

---

### Complete CRUD Mapping Matrix

| Operation | HTTP Verb | Path | Request Body | Response Status | Purpose / Business Rule |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Create** | `POST` | `/orders` | `Order` JSON | `201 Created` | Auto-generates UUID if omitted; returns `Location: /orders/{id}` |
| **Read All / Query** | `GET` | `/orders` | *None* | `200 OK` | Returns all orders or filters by `?item=...` |
| **Read One** | `GET` | `/orders/{id}` | *None* | `200 OK` / `404 Not Found` | Fetches single order by ID |
| **Update** | `PUT` | `/orders/{id}` | `Order` JSON | `200 OK` / `404 Not Found` | Replaces order state; enforces path ID over body ID |
| **Delete One** | `DELETE` | `/orders/{id}` | *None* | `204 No Content` / `404` | Removes item from store; returns empty body on success |
| **Delete All** | `DELETE` | `/orders` | *None* | `204 No Content` | Clears all items in the store |

---

### The `Response` Builder & HTTP Status Semantics
1. **`201 Created` with Location header**:
   ```java
   URI location = URI.create("/orders/" + newOrder.id());
   return Response.created(location).entity(newOrder).build();
   ```
2. **`204 No Content`**:
   Signifies the action was successful, but there is no payload body to return:
   ```java
   return Response.noContent().build();
   ```
3. **`404 Not Found`**:
   ```java
   return Response.status(Response.Status.NOT_FOUND).build();
   ```

---

### URL Parameters: `@PathParam` vs. `@QueryParam`

```text
URL: https://api.store.com/orders/order-101?item=Keyboard
                           └─────┬──────┘  └──────┬──────┘
                              @PathParam       @QueryParam
```

| Feature | `@PathParam` | `@QueryParam` |
| :--- | :--- | :--- |
| **Location** | Part of URI path (`/orders/{id}`) | After `?` (`/orders?item=Keyboard`) |
| **Primary Role** | Identifying a single specific resource | Filtering, sorting, searching, pagination |
| **Is it Required?** | **Yes** (URL structure requires it) | **No** (Optional by nature) |
| **Missing Value Handling**| Path does not match (returns 404) | Injected as `null` or `@DefaultValue` |
| **Example** | `GET /orders/123` | `GET /orders?item=Keyboard` |

---

### Critical JAX-RS Routing Rule (Ambiguous Route Avoidance)
In JAX-RS, **query parameters do NOT distinguish endpoints**.

```java
// ❌ AMBIGUOUS ROUTE CONFLICT! (Will log RESTEASY002142 warning and behave unpredictably)
@GET
public Response getAllOrders() { ... }

@GET
public Response getOrdersByItem(@QueryParam("item") String item) { ... }
```

#### The Correct Design:
A single `@GET` endpoint on `/orders` must handle both parameterized and unparameterized calls:

```java
@GET
public Response getOrders(@QueryParam("item") @DefaultValue("") String item) {
    if (item == null || item.isBlank()) {
        return Response.ok(orderService.getAllOrders()).build();
    }
    return Response.ok(orderService.getOrders(item.trim())).build();
}
```

---

## 5. API Architecture: Request & Response DTOs

### What are DTOs and Why Separate Them?
In production systems, you should not expose your internal domain or JPA database entity directly over the REST API. Instead, separate them into:
1. **Request DTO (`CreateOrderRequest`)**: What the client is allowed to send.
2. **Domain/Entity Model (`Order`)**: What the system stores internally.
3. **Response DTO (`OrderResponse`)**: What the client receives back.

```text
Client (JSON) ──> [CreateOrderRequest] ──> Service/Domain [Order] ──> [OrderResponse] ──> Client (JSON)
                      (Request DTO)              (Entity/Model)           (Response DTO)
```

---

### Preventing "Mass Assignment" (Over-Posting) Vulnerabilities
If clients send raw entities directly, a malicious client could inject fields they shouldn't control:
```json
{
  "item": "Laptop",
  "quantity": 1,
  "id": "admin-override-id",
  "status": "APPROVED_FREE_OF_CHARGE"
}
```
With a dedicated `CreateOrderRequest` record containing **only** `item` and `quantity`, the client physically *cannot* inject internal IDs or status flags.

---

### Decoupling API Contracts from Persistence Schemas
- Prevents database schema changes (e.g. column renames, migrations) from breaking external API clients.
- Prevents lazy-loading Jackson serialization cycles (`LazyInitializationException`) with Hibernate.
- Allows returning computed fields (e.g., `totalPrice`, `orderStatusDescription`) without polluting the database schema.

---

## 6. Jakarta Bean Validation (`quarkus-hibernate-validator`)

### Declarative Validation Annotations
In `Order.java`:
```java
public record Order(
    String id,
    @NotBlank(message = "Item name cannot be blank") String item,
    @Positive(message = "Quantity must be greater than 0") int quantity
) {}
```
- **`@NotBlank`**: Ensures string is not `null` and has at least one non-whitespace character.
- **`@Positive`**: Ensures numbers are strictly greater than zero (`> 0`). Rejects zero and negative numbers.

---

### Cascading Validation with `@Valid`
In `OrderResource.java`:
```java
@POST 
public Response createOrder(@Valid Order order) { ... }

@PUT
@Path("/{id}")
public Response updateOrder(@PathParam("id") String id, @Valid Order order) { ... }
```
- `@Valid` triggers validation **before** entering the method body.
- If invalid, Quarkus immediately rejects the request with `HTTP 400 Bad Request` and `validation-exception: true`, preventing invalid data from reaching business logic.

---

### Handling Optional vs. Mandatory Attributes
- **Design Rule**: `id` is intentionally **not** annotated with `@NotBlank`.
- **Reasoning**: If a client POSTs `{"item": "Mouse", "quantity": 2}`, our server generates a UUID (`UUID.randomUUID().toString()`). If `id` were marked `@NotBlank`, validation would reject the payload before our UUID generation could run.

---

## 7. Centralized Error Handling with `@ServerExceptionMapper`

### The Problem with Scattered `try-catch` Blocks
Without centralized exception mapping, resources end up cluttered:
```java
// ❌ Anti-pattern: Repetitive boilerplate across every endpoint
try {
    orderService.createOrder(newOrder);
} catch (IllegalArgumentException e) {
    return Response.status(400).entity(Map.of("error", e.getMessage())).build();
}
```

---

### Enforcing One Consistent JSON Error Shape (RFC 7807 Pattern)
Every error in the API should return a predictable schema:

```java
package com.zimble;

import java.time.Instant;

public record ErrorResponse(
    int status,
    String title,
    String detail,
    Instant timestamp
) {
    public ErrorResponse(int status, String title, String detail) {
        this(status, title, detail, Instant.now());
    }
}
```

#### Example Output:
```json
{
  "status": 400,
  "title": "Bad Request",
  "detail": "Order exceeds maximum items limit of 100",
  "timestamp": "2026-11-16T10:35:08.123456Z"
}
```

---

### How `@ServerExceptionMapper` Works in Quarkus REST
* **Import**: `org.jboss.resteasy.reactive.server.ServerExceptionMapper`
* Automatically catches exceptions thrown anywhere in the service layer and serializes them into the unified `ErrorResponse`.
* **Zero Boilerplate**: No need to implement the verbose `ExceptionMapper<E>` interface. Multiple exception handlers can be grouped into a single class.

In `GlobalExceptionHandler.java`:
```java
package com.zimble;

import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class GlobalExceptionHandler {

    @ServerExceptionMapper 
    public Response handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorResponse errorResponse = new ErrorResponse(
            Response.Status.BAD_REQUEST.getStatusCode(),
            "Bad Request",
            e.getMessage()
        );
        return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
    }

    @ServerExceptionMapper 
    public Response handleGenericException(Exception e) {
        ErrorResponse errorResponse = new ErrorResponse(
            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            "Internal Server Error",
            "An unexpected error occurred. Please try again later."
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
    }
}
```

#### Result in `OrderResource.java`:
All `try-catch` blocks are completely eliminated! The endpoint methods simply focus on the happy path:
```java
@POST 
public Response createOrder(@Valid Order order) {
    String orderId = (order.id() == null || order.id().isBlank()) ? UUID.randomUUID().toString() : order.id();
    Order newOrder = new Order(orderId, order.item(), order.quantity());

    orderService.createOrder(newOrder); // If this throws, @ServerExceptionMapper handles it automatically!
    URI location = URI.create("/orders/" + newOrder.id());
    return Response.created(location).entity(newOrder).build();
}
```

---

### Modern Quarkus REST (`quarkus-rest`) vs. Legacy RESTEasy Classic
In `pom.xml`, modern Quarkus 3 applications use:
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-jackson</artifactId>
</dependency>
```
- Built on top of Vert.x for non-blocking, reactive HTTP handling.
- Up to 2-3x faster throughput than RESTEasy Classic.
- Native support for `@ServerExceptionMapper`.

---

## 8. Automated Testing with `@QuarkusTest` & REST-assured

### Integration Testing Architecture
In `OrderResourceTest.java`:
```java
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderResourceTest { ... }
```
- **`@QuarkusTest`**: Boots Quarkus in test mode on an ephemeral port (`8081`).
- Tests execute real HTTP requests through the full stack (REST → ArC CDI → Service → In-Memory Store).

---

### Given-When-Then Fluent Testing Syntax
```java
given()
    .contentType(ContentType.JSON)
    .body(newOrderJson)
.when()
    .post("/orders")
.then()
    .statusCode(201)
    .header("Location", notNullValue())
    .body("id", equalTo("order-101"))
    .body("item", equalTo("Mechanical Keyboard"))
    .body("quantity", equalTo(5));
```

#### Key Hamcrest Matchers:
- `equalTo(val)`: Value equality assertion.
- `hasSize(n)`: Collection length verification.
- `notNullValue()`: Asserts that an attribute or header exists.

---

### Test Ordering with `@TestMethodOrder`
- `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` and `@Order(n)` allow testing full stateful user journeys deterministically (Create → Query → Update → Delete → 404 Verification).

---

## 9. Quarkus Developer Workflow & Packaging

### Live Coding / Dev Mode
```bash
./mvnw quarkus:dev
```
- Changes saved in `.java` files or `application.properties` are recompiled on-the-fly on the next incoming HTTP request.
- **Terminal Shortcuts**:
  - `r`: Re-run tests immediately.
  - `d`: Open Quarkus Dev UI in browser (`http://localhost:8080/q/dev-ui`).
  - `h`: Show help and shortcut list.

### Packaging & Production Runner Layout
```bash
# Package fast-jar format
./mvnw package

# Run packaged application
java -jar target/quarkus-app/quarkus-run.jar
```
- Produces a thin runnable fast-jar in `target/quarkus-app/` with isolated dependencies in `lib/` for optimal container layer caching.

---

## Next Milestone: Data Persistence with Panache
In the next session, we transition from our in-memory `ConcurrentHashMap` to PostgreSQL using:
- **`quarkus-hibernate-orm-panache`**: The active record / repository pattern in Quarkus.
- **Quarkus Dev Services**: Zero-config automatic testcontainer-managed PostgreSQL database launched automatically during development and testing.
