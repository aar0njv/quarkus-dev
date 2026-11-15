

## Java Records (Immutable Data Carriers)
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

## Thread Safety & In-Memory Concurrency
In `OrderService.java`:
```java
private final Map<String, Order> database = new ConcurrentHashMap<>();
```

#### Why `ConcurrentHashMap` instead of `HashMap`?
Uses fine-grained bucket/segment locking and lock-free reads via volatile memory reads and Compare-And-Swap (CAS) instructions. Multiple reader and writer threads can access different buckets concurrently without blocking one another.








## Dependency Injection: Constructor Injection vs. Field Injection

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
1. **Enforces Immutability**: Injected dependencies can be marked `final`. They cannot be changed after construction.
2. **Prevents Incomplete State**: The compiler guarantees an object cannot be instantiated without supplying its dependencies.
3. **Pure Unit Testing**: The class can be tested in isolation with plain `new OrderService(50)` without spinning up a CDI container or using reflection mocks.
4. **No `@Inject` Annotation Required**: In ArC and modern CDI, if a class has a single constructor, `@Inject` is implicit and optional.

---

## Application Lifecycle Observers (`@Observes Startup`)
In `OrderService.java`:
```java
void onStart(@Observes Startup event) {
    LOG.infof("OrderService started. Maximum items allowed per order: %d", maxItems);
}
```

- In CDI, `@Observes` registers an event listener method.
- When Quarkus finishes wiring dependencies and boots the runtime, it fires the `jakarta.enterprise.event.Startup` event.
- This allows deterministic startup tasks (cache warmups, sanity checks, configuration logging) without relying on traditional `main` method hacks.



## MicroProfile Configuration & Profile Overrides

### Dynamic Property Injection (`@ConfigProperty`)
In `OrderService.java`:
```java
public OrderService(@ConfigProperty(name = "orders.max-items") int maxItems) { ... }
```
- Injects values directly from `src/main/resources/application.properties`, environment variables, or system properties.
- Strongly typed: Strings in configuration are converted to primitives (`int`, `boolean`), lists, or custom types automatically.

---

### Configuration Profile Prefixes (`%dev`, `%test`, `%prod`)
In `application.properties`:
```properties
# Default (Production) value
orders.max-items=10

# Development mode override
%dev.orders.max-items=100
```

#### Built-in Profiles in Quarkus:
- **`%dev`**: Active automatically when running `./mvnw quarkus:dev`.
- **`%test`**: Active automatically during `./mvnw test`. Falls back to the root property if `%test` is omitted (hence `maxItems = 10` during tests).
- **`%prod`**: Default active profile when packaged into a runnable JAR or native container image.

---

## RESTful API Design & JAX-RS (RESTEasy)

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

- `@Path("/orders")`: Establishes the URI prefix for all endpoint methods in this class.
- `@Produces(MediaType.APPLICATION_JSON)`: Jackson serializes return values into JSON.
- `@Consumes(MediaType.APPLICATION_JSON)`: Jackson parses incoming HTTP request bodies into Java objects.

---

### Complete CRUD Mapping Matrix

| Operation | HTTP Verb | Path | Request Body | Response Status | Purpose / Business Rule |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Create** | `POST` | `/orders` | `Order` JSON | `201 Created` | Auto-generates UUID if omitted; returns `Location: /orders/{id}` |
| **Read All** | `GET` | `/orders` | *None* | `200 OK` | Returns JSON array of all active orders |
| **Read One** | `GET` | `/orders/{id}` | *None* | `200 OK` / `404 Not Found` | Fetches single order by ID |
| **Update** | `PUT` | `/orders/{id}` | `Order` JSON | `200 OK` / `404 Not Found` | Replaces order state; enforces path ID over body ID |
| **Delete One** | `DELETE` | `/orders/{id}` | *None* | `204 No Content` / `404` | Removes item from map; returns empty body on success |
| **Delete All** | `DELETE` | `/orders` | *None* | `204 No Content` | Clears all items in the database |

---

### The `Response` Builder & HTTP Status Semantics
Using `jakarta.ws.rs.core.Response` allows complete control over status codes, headers, and entities:

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
4. **`400 Bad Request`**:
   ```java
   return Response.status(Response.Status.BAD_REQUEST)
                  .entity(Map.of("error", errorMsg))
                  .build();
   ```

---

## Bean Validation (`quarkus-hibernate-validator`)

### Declarative Validation Annotations
In `Order.java`:
```java
public record Order(
    String id,
    @NotBlank(message = "Item name cannot be blank") String item,
    @Positive(message = "Quantity must be greater than 0") int quantity
) {}
```

- **`@NotBlank`**: Ensures the string is not `null` and has at least one non-whitespace character (`trimmed().length() > 0`).
- **`@Positive`**: Ensures numeric values are strictly greater than zero (`> 0`). Rejects zero and negative numbers.

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
- Adding `@Valid` tells the JAX-RS container to validate the object **before** executing the method body.
- If validation fails:
  - Quarkus intercepts the call immediately.
  - Returns `HTTP 400 Bad Request`.
  - Sets the `validation-exception: true` HTTP response header.
  - The resource and service methods are never executed, protecting the system from corrupt data.

---

### Handling Optional vs. Mandatory Attributes
- **Design Decision**: Notice `id` is **not** annotated with `@NotBlank`.
- **Reasoning**: If a client POSTs `{"item": "Mouse", "quantity": 2}`, our server generates a random UUID (`UUID.randomUUID().toString()`). If `id` were annotated with `@NotBlank`, the validation filter would reject this valid payload before our UUID generation logic could execute.




## Next: Data Persistence with Panache

