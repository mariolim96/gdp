---
paths:
paths:
  - '**/*.java'
---

# Java Testing

> This file extends [common/testing.md](../common/testing.md) with Java, Quarkus, and Spring Boot specific content.

## Test Frameworks

- **JUnit 5** (Jupiter): Primary testing framework (`@Test`, `@ParameterizedTest`, `@Nested`).
- **AssertJ**: Fluent assertions (`assertThat(result).isEqualTo(expected)`).
- **Mockito**: Mocking dependencies.
- **REST Assured**: API/Endpoint testing (Standard in Quarkus).
- **Testcontainers**: Integration tests requiring real databases or services.

## Ecosystem Testing

### Quarkus Testing

- Use **@QuarkusTest** for integration tests requiring the CDI container.
- Use **@InjectMock** (from `quarkus-junit5-mockito`) to mock CDI beans.
- Use **@TestProfile** for environment-specific configurations.

### Spring Boot Testing

- Use **@SpringBootTest** for full integration tests.
- Use **@WebMvcTest** for controller slicing.
- Use **@DataJpaTest** for repository slicing.
- Use **@MockBean** to mock Spring beans.

## Test Organization

```
src/test/java/com/example/app/
  service/           # Unit tests for service layer (Mockito)
  controller/        # API / Resource layer tests (RestAssured / MockMvc)
  repository/        # Data access tests (Testcontainers)
  integration/       # Full container-based tests (@QuarkusTest / @SpringBootTest)
```

## Unit Test Pattern (JUnit 5 + Mockito)

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository);
    }

    @Test
    @DisplayName("findById returns order when exists")
    void findById_existingOrder_returnsOrder() {
        var order = new Order(1L, "Alice", BigDecimal.TEN);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        var result = orderService.findById(1L);

        assertThat(result.customerName()).isEqualTo("Alice");
    }
}
```

## Parameterized Tests

```java
@ParameterizedTest
@CsvSource({
    "100.00, 10, 90.00",
    "50.00, 0, 50.00"
})
@DisplayName("discount applied correctly")
void applyDiscount(BigDecimal price, int pct, BigDecimal expected) {
    assertThat(PricingUtils.discount(price, pct)).isEqualByComparingTo(expected);
}
```

## Coverage

- **Target: 80%+** line/branch coverage.
- Use **JaCoCo** for coverage reporting.
- Focus on business and domain logic; skip trivial boilerplate.

## References

- See skill: `springboot-tdd` for Spring Boot TDD patterns (MockMvc, Testcontainers).
- See skill: `quarkus-tdd` for Quarkus TDD patterns (RestAssured, InjectMock, Testcontainers).
- See skill: `quarkus-verification` for full build and native image verification pipeline.
