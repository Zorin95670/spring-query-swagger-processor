# Spring Query Swagger Processor

This library is an annotation processor designed to work alongside **spring-query-filter**.

It automatically generates **DTO classes enriched with Swagger / OpenAPI annotations**, making it easy to document and expose your filtering system in API documentation tools like Swagger UI.

---

## Overview

While **spring-query-filter** provides powerful runtime filtering capabilities based on HTTP query parameters, this library focuses on **developer experience and API documentation**.

It allows you to:

* Automatically generate filter DTOs from your domain models
* Expose filtering capabilities in Swagger / OpenAPI
* Provide clear examples of supported filtering operators
* Avoid manually writing and maintaining filter documentation

---

## How It Works

You annotate your model (or a dedicated class) with:

* `@QueryFilter` (class-level)
* `@QueryFilterField` (field-level)

The processor will generate a DTO:

* Containing all filterable fields
* Using `List<String>` to support multiple filter expressions
* Enriched with Swagger annotations (`@Schema`, `@Parameter`, `@ExampleObject`)
* Including examples for each supported operator

---

## Dependency

### Maven

```xml
<dependency>
    <groupId>io.github.zorin95670</groupId>
    <artifactId>spring-query-swagger-processor</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
dependencies {
    annotationProcessor("io.github.zorin95670:spring-query-swagger-processor:1.0.0")
}
```

---

## Usage

### 1. Annotate Your Class

```java
import io.github.zorin95670.processor.annotation.QueryFilter;
import io.github.zorin95670.processor.annotation.QueryFilterField;

@QueryFilter
public class UserFilter {

    @QueryFilterField(type = String.class, description = "Filter by username")
    private String username;

    @QueryFilterField(type = Integer.class, description = "Filter by age")
    private Integer age;

    @QueryFilterField(type = Date.class, description = "Filter by creation date")
    private Date createdAt;
}
```

---

### 2. Generated DTO

The processor will generate a class like:

```java
public class UserFilterQueryFilterDto {

    @Parameter(...)
    private List<String> username;

    @Parameter(...)
    private List<String> age;

    @Parameter(...)
    private List<String> createdAt;

    @Parameter(...)
    private String dateFormat;

    // getters & setters
}
```

---

### 3. Use It in Your Controller

```java
@GetMapping("/users")
public Page<User> findUsers(
    UserFilterQueryFilterDto filters,
    Pageable pageable
) {
    return userService.find(filters, pageable);
}
```

You can then convert this DTO into the `Map<String, List<String>>` expected by **spring-query-filter**, or adapt your service accordingly.

---

## Swagger / OpenAPI Integration

Each generated field includes:

* `@Parameter(description = ...)`
* `required` flag
* Multiple `@ExampleObject` illustrating filtering operators

### Example in Swagger UI

For a `String` field:

* `john` → equals
* `not_john` → not equals
* `lk_*john*` → like
* `not_lk_*john*` → not like
* `john|doe` → OR condition

For numeric fields:

* `gt_10` → greater than
* `lt_20` → less than
* `10_bt_20` → between

---

## Supported Filtering Operators

* `eq_` or no prefix → equals
* `gt_` → greater than
* `lt_` → less than
* `_bt_` → between
* `lk_` → like (`*` or `%` supported)
* `not_` → negation
* `|` → OR

---

## Date Handling

If at least one field is of type `Date`, the generated DTO will include:

```java
private String dateFormat;
```

This allows API consumers to define the expected date format.

### Example

```
GET /users?dateFormat=yyyyMMdd&createdAt=20240101
```

---

## Why Use This Library?

Without this processor:

* You must manually document filtering logic
* Swagger does not reflect real filtering capabilities
* Consumers struggle to understand query syntax

With this processor:

* Documentation is **generated automatically**
* Swagger becomes **self-explanatory**
* Filtering usage becomes **discoverable and consistent**

---

## Relationship with spring-query-filter

| Feature               | spring-query-filter | swagger-processor |
| --------------------- | ------------------- | ----------------- |
| Runtime filtering     | ✔                   | ✘                 |
| Predicate generation  | ✔                   | ✘                 |
| Swagger documentation | ✘                   | ✔                 |
| DTO generation        | ✘                   | ✔                 |

👉 Both libraries are designed to be used **together**.

---

## Advanced Usage

* Customize DTO name via `@QueryFilter(name = "...")`
* Customize Swagger description via `description`
* Extend generator behavior if needed

---

## Conclusion

**spring-query-swagger-processor** bridges the gap between powerful backend filtering and clear API documentation.

It ensures that your filtering system is not only functional—but also **understandable, discoverable, and maintainable**.
