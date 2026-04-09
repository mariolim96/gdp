# Project Guide: Contract-First Development & DTO Management

This project follows a **Contract-First** architecture. The source of truth for all API interactions is the OpenAPI specification (`openapi.yaml`).

## 🚀 The Philosophy
Never create DTOs or REST Controllers manually. The build process generates these for you, ensuring that the backend remains in sync with the Frontend/BFF requirements.

---

## 🛠️ 1. How to Generate the Code
Whenever the `openapi.yaml` is modified, you must run the Maven generator:

```bash
./mvnw clean generate-sources
```

**Where are the files?**
The generated source code will be located in:
- `gdporch/target/generated-sources/openapi/src/gen/java`

**Packages:**
- **DTOs**: `it.csipiemonte.gdp.gdporch.dto`
- **Interfaces**: `it.csipiemonte.gdp.gdporch.api`

---

## 🏗️ 2. Implementing a Controller
The generator creates a JAX-RS interface for every API tag. You should not write your own `@Path` manually. Instead, **implement the generated interface**.

### Example (F18):
If `openapi.yaml` defines a tag `bo-testate`, the plugin generates an interface (e.g., `BoTestateApi.java`).

```java
package it.csipiemonte.gdp.gdporch.controller;

import it.csipiemonte.gdp.gdporch.api.BoTestateApi; // Generated!
import it.csipiemonte.gdp.gdporch.dto.DateAtteseList; // Generated!
import it.csipiemonte.gdp.gdporch.service.GdpVerifDateAtteseService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;

public class BoTestateController implements BoTestateApi {

    @Inject
    GdpVerifDateAtteseService service;

    @Override
    public Response getBoDateAttese(Integer idTestata, String dataInizio, String dataFine) {
        // Parse dates if necessary or pass them directly as String/LocalDate
        DateAtteseList result = service.execute(idTestata, LocalDate.parse(dataInizio), LocalDate.parse(dataFine));
        return Response.ok(result).build();
    }
}
```

---

## 🔄 3. Mapping Entities to DTOs
Do not return your `@Entity` objects directly through the API. Always map them to the generated DTOs.

### Option A: Using MapStruct (Recommended)
Add mappings to an interface and let MapStruct generate the boilerplate.

```java
@Mapper(componentModel = "jakarta")
public interface TestataMapper {
    @Mapping(target = "idTestata", source = "id")
    TestataSummary toSummary(GdpTestata entity);
}
```

### Option B: Manual Mapping in Service
```java
public DateAtteseList execute(...) {
    List<GdpDataUscita> rows = repository.find(...);
    
    // Transform Entities into Generated DTOs
    List<DataAttesa> items = rows.stream().map(row -> {
        DataAttesa dto = new DataAttesa();
        dto.setData(row.dataAttesa);
        dto.setSospesa(row.sospesa);
        return dto;
    }).toList();

    DateAtteseList response = new DateAtteseList();
    // Set fields...
    return response;
}
```

## 💉 4. Managing Beans & Dependency Injection

To keep the application decoupled and testable, use **Jakarta CDI** (Contexts and Dependency Injection).

### Mappers as Beans

Always set `componentModel = "jakarta"` in your MapStruct interfaces. This allows the CDI container to manage them.

```java
@Mapper(componentModel = "jakarta")
public interface MyMapper { ... }
```

### Injection Styles

We recommend **Constructor Injection**. It makes dependencies explicit and simplifies unit testing.

```java
@ApplicationScoped
public class MyService {
    private final MyMapper mapper;

    // @Inject is optional in Quarkus if there's only one constructor
    public MyService(MyMapper mapper) {
        this.mapper = mapper;
    }
}
```

### Mocking Beans in Tests

Use `@InjectMock` to replace a CDI bean with a Mockito mock during integration tests.

```java
@QuarkusTest
class MyServiceTest {
    @InjectMock
    MyMapper mapper; 

    @Test
    void testExecution() {
        Mockito.when(mapper.toDto(any())).thenReturn(new MyDto());
        // ...
    }
}
```

## ✅ 5. Triggering Validation

In a Contract-First approach, the validation annotations (like `@NotNull`, `@Size`, `@Pattern`) are automatically added to the generated DTOs. However, the validation process must be explicitly triggered.

### In Controllers (JAX-RS)

To validate an incoming request body or parameter, add the **`@Valid`** annotation to the method argument in your controller implementation.

```java
@Override
public Response postBoDateAttese(Integer idTestata, @Valid DateRangeRequest request) {
    // If 'request' is invalid, Quarkus returns 400 Bad Request automatically.
    service.schedule(idTestata, request);
    return Response.accepted().build();
}
```

### In Services

You can also trigger validation at the service level. Ensure the service is a CDI bean (e.g., `@ApplicationScoped`) and use the `@Valid` annotation.

```java
@ApplicationScoped
public class MyService {
    public void process(@Valid MyDto data) {
         // ...
    }
}
```

### Why not `@Validated`?

While Spring uses `@Validated` for group validation, **Quarkus (and Jakarta EE)** primarily uses the standard **`@Valid`** annotation. As long as you have the `quarkus-hibernate-validator` extension, validation is automatically enforced on any CDI bean method marked with `@Valid`.

---

## ⚠️ Common Mistakes to Avoid

1.  **❌ Manual DTOs**: Never write classes like `GdpVerifDateAtteseRequest.java`. If it's in the contract, it already exists in the `dto` package.
2.  **❌ Manual Paths**: Do not use `@Path("/custom-path")` if it doesn't match the `openapi.yaml`. If you implement the generated interface, the paths and method constraints are inherited automatically.
3.  **❌ Grouping Failures**: If the contract expects a nested structure (e.g., `List<Testata> -> List<Date>`), do not flatten it into a single list. Use Java Streams (`groupingBy`) to build the correct response tree.
4.  **❌ Wrong Field Names**: The DB column is `INVIO_EDIZIONE` (singular) and it's a `Boolean`. Using `invioEdizioni` or `Integer` will cause errors with Hibernate.

---

## 🧪 Testing with DTOs
When writing integration tests (`@QuarkusTest`), always use the generated DTOs in your `rest-assured` calls:

```java
DateRangeRequest req = new DateRangeRequest();
req.setDataInizio(LocalDate.now());

given()
  .contentType(ContentType.JSON)
  .body(req)
  .when().post("/bo/testate/1/date-attese")
  .then().statusCode(202);
```
