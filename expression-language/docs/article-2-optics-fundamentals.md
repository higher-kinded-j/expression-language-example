# Optics Fundamentals: Lenses, Prisms, and Traversals in Practice

*Part 2 of the Functional Optics for Modern Java series*

In Article 1, we identified the immutability gap: modern Java excels at reading nested data through pattern matching, but provides no elegant solution for writing. We introduced optics as the missing piece—composable abstractions that treat access paths as first-class values.

Now it's time to get practical. This article dives deep into the three core optic types: lenses for product types, prisms for sum types, and traversals for collections. By the end, you'll understand not just how to use each, but when and why.

---

## Setting Up higher-kinded-j

Before we explore optics in depth, let's configure our project to use higher-kinded-j's annotation-driven generation.

### Gradle Configuration

```kotlin
plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    // Core optics library
    implementation("org.higherkindedj:hkj-optics:2.0.0")

    // Annotation processor for lens/prism generation
    annotationProcessor("org.higherkindedj:hkj-optics-processor:2.0.0")
    compileOnly("org.higherkindedj:hkj-optics-annotations:2.0.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}
```

### Maven Configuration

```xml
<dependencies>
    <dependency>
        <groupId>org.higherkindedj</groupId>
        <artifactId>hkj-optics</artifactId>
        <version>2.0.0</version>
    </dependency>
    <dependency>
        <groupId>org.higherkindedj</groupId>
        <artifactId>hkj-optics-annotations</artifactId>
        <version>2.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.higherkindedj</groupId>
                        <artifactId>hkj-optics-processor</artifactId>
                        <version>2.0.0</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

With the dependencies in place, we're ready to explore each optic type in depth.

---

## Lenses: The Foundation

A lens focuses on exactly one value within a larger structure. It represents a "has-a" relationship: an `Employee` *has an* `Address`; an `Address` *has a* `street`. Lenses always succeed—the focused value is guaranteed to exist.

### Generating Lenses

The `@GenerateLenses` annotation instructs higher-kinded-j to generate lens accessors for each record component:

```java
import org.higherkindedj.optics.annotations.GenerateLenses;

@GenerateLenses
public record Address(String street, String city, String postcode) {}

@GenerateLenses
public record Employee(String id, String name, Address address) {}

@GenerateLenses
public record Department(String name, Employee manager, List<Employee> staff) {}
```

The annotation processor generates a companion class with lens factories:

```java
// Generated: AddressLenses.java
public final class AddressLenses {
    public static Lens<Address, String> street() { ... }
    public static Lens<Address, String> city() { ... }
    public static Lens<Address, String> postcode() { ... }
}

// Generated: EmployeeLenses.java
public final class EmployeeLenses {
    public static Lens<Employee, String> id() { ... }
    public static Lens<Employee, String> name() { ... }
    public static Lens<Employee, Address> address() { ... }
}
```

### Using Lenses

Each lens provides three core operations:

```java
Lens<Address, String> streetLens = AddressLenses.street();

// Get: extract the focused value
String street = streetLens.get(address);

// Set: return a new structure with the focused value replaced
Address updated = streetLens.set("100 New Street", address);

// Modify: apply a function to the focused value
Address uppercased = streetLens.modify(String::toUpperCase, address);
```

The `modify` operation is particularly powerful—it combines get and set in a single traversal, ensuring the transformation is applied consistently.

### Lens Composition

The real power emerges when you compose lenses. The `andThen` method chains lenses to reach deeper into nested structures:

```java
// Compose: Employee → Address → String
Lens<Employee, String> employeeStreet =
    EmployeeLenses.address().andThen(AddressLenses.street());

// Now we can get/set/modify the street directly on an Employee
String street = employeeStreet.get(employee);
Employee updated = employeeStreet.set("200 Oak Avenue", employee);
Employee transformed = employeeStreet.modify(s -> s + " (verified)", employee);
```

Each composed lens handles all the intermediate reconstruction automatically. That twenty-five-line copy-constructor cascade from Article 1? It's now implicit in the lens composition.

### Lens Laws

Well-behaved lenses satisfy three laws that ensure predictable behaviour:

1. **Get-Set**: If you get a value and then set it back, the structure is unchanged.
   ```java
   lens.set(lens.get(s), s) == s
   ```

2. **Set-Get**: If you set a value, getting it returns what you set.
   ```java
   lens.get(lens.set(a, s)) == a
   ```

3. **Set-Set**: Setting twice is the same as setting once with the final value.
   ```java
   lens.set(a2, lens.set(a1, s)) == lens.set(a2, s)
   ```

These laws aren't just theoretical—they guarantee that lenses behave like mathematical getters and setters. The annotation processor generates lenses that satisfy these laws automatically.

### Pattern: Lens as Structural Path

Think of a lens as a "structural JSON pointer" for your objects. Just as `/employee/address/street` navigates a JSON document, `employeeAddress.andThen(addressStreet)` navigates a Java object graph. The difference: it's type-safe, and it works bidirectionally.

---

## Prisms: Sum Type Access

Where lenses focus on fields that always exist (product types), prisms focus on variants that might exist (sum types). A prism represents an "is-a" relationship: a `Shape` *might be a* `Circle`; an `Expr` *might be a* `Binary`.

### The Optional Nature of Prisms

Consider a sealed interface:

```java
public sealed interface Shape permits Circle, Rectangle, Triangle {}

@GenerateLenses
public record Circle(double radius) implements Shape {}

@GenerateLenses
public record Rectangle(double width, double height) implements Shape {}

@GenerateLenses
public record Triangle(double a, double b, double c) implements Shape {}
```

Given a `Shape`, we don't know which variant it is. A prism for `Circle` must handle the possibility that the shape isn't a circle at all.

### Generating Prisms

The `@GeneratePrisms` annotation on a sealed interface generates prisms for each permitted subtype:

```java
import org.higherkindedj.optics.annotations.GeneratePrisms;

@GeneratePrisms
public sealed interface Shape permits Circle, Rectangle, Triangle {}
```

This generates:

```java
// Generated: ShapePrisms.java
public final class ShapePrisms {
    public static Prism<Shape, Circle> circle() { ... }
    public static Prism<Shape, Rectangle> rectangle() { ... }
    public static Prism<Shape, Triangle> triangle() { ... }
}
```

### Using Prisms

Prisms provide different operations than lenses, reflecting their optional nature:

```java
Prism<Shape, Circle> circlePrism = ShapePrisms.circle();

// getOptional: extract the variant if it matches
Optional<Circle> maybeCircle = circlePrism.getOptional(shape);

// build: construct the sum type from the variant (always succeeds)
Shape shape = circlePrism.build(new Circle(5.0));

// matches: check if the prism matches
boolean isCircle = circlePrism.matches(shape);

// modify: transform if it matches, leave unchanged otherwise
Shape doubled = circlePrism.modify(c -> new Circle(c.radius() * 2), shape);
```

The `modify` on a prism is particularly elegant: it applies the transformation only if the prism matches, otherwise returning the original value unchanged. No explicit pattern matching required.

### Composing Prisms with Lenses

Prisms compose with lenses to reach into variant-specific fields:

```java
// Prism: Shape → Circle, then Lens: Circle → radius
Prism<Shape, Circle> circlePrism = ShapePrisms.circle();
Lens<Circle, Double> radiusLens = CircleLenses.radius();

// Compose into an Optional (affine)
Optional<Shape, Double> shapeRadius = circlePrism.andThen(radiusLens);

// Get the radius if it's a circle
Optional<Double> radius = shapeRadius.getOptional(shape);

// Double the radius if it's a circle
Shape modified = shapeRadius.modify(r -> r * 2, shape);
```

Notice the type: composing a `Prism` with a `Lens` yields an `Optional` (sometimes called an affine traversal). This reflects the reality: we might not find anything to focus on.

### Pattern: Type-Safe Downcasting

Prisms provide type-safe downcasting without explicit `instanceof` checks:

```java
// Traditional approach
if (shape instanceof Circle circle) {
    return new Circle(circle.radius() * 2);
}
return shape;

// Prism approach
return circlePrism.modify(c -> new Circle(c.radius() * 2), shape);
```

The prism version is more composable. You can store it, pass it around, and combine it with other optics—something you can't do with an `instanceof` expression.

---

## Traversals: Bulk Operations

Traversals generalise lenses to focus on zero or more values simultaneously. They're the optic for "has-many" relationships: a `Department` *has many* `Employee`s; an `Order` *has many* `LineItem`s.

### Basic List Traversal

Higher-kinded-j provides a built-in traversal for lists:

```java
Traversal<List<String>, String> listTraversal = Traversals.list();

List<String> names = List.of("alice", "bob", "charlie");

// Modify all elements
List<String> uppercased = listTraversal.modify(String::toUpperCase, names);
// ["ALICE", "BOB", "CHARLIE"]

// Get all elements (as a list)
List<String> all = listTraversal.getAll(names);
// ["alice", "bob", "charlie"]
```

### Composing Traversals for Nested Collections

The real power comes when composing traversals with lenses to reach into nested structures:

```java
// Lens: Department → List<Employee>
Lens<Department, List<Employee>> staffLens = DepartmentLenses.staff();

// Traversal: List<Employee> → Employee
Traversal<List<Employee>, Employee> eachEmployee = Traversals.list();

// Lens: Employee → Address
Lens<Employee, Address> addressLens = EmployeeLenses.address();

// Lens: Address → String
Lens<Address, String> streetLens = AddressLenses.street();

// Compose them all: Department → each employee's street
Traversal<Department, String> allStaffStreets =
    staffLens
        .andThen(eachEmployee)
        .andThen(addressLens)
        .andThen(streetLens);

// Update every staff member's street
Department updated = allStaffStreets.modify(s -> s + " (relocated)", dept);

// Collect all streets
List<String> streets = allStaffStreets.getAll(dept);
```

One composed traversal replaces what would otherwise be nested loops with manual reconstruction at each level.

### Filtered Traversals

Sometimes you want to focus on only a subset of elements. The `filtered` method creates a traversal that only matches elements satisfying a predicate:

```java
// Only employees in London
Traversal<List<Employee>, Employee> londonStaff =
    Traversals.<Employee>list()
        .filtered(e -> e.address().city().equals("London"));

// Give London staff a raise
List<Employee> updated = londonStaff.modify(
    e -> new Employee(e.id(), e.name(), e.address(), e.salary() * 1.1),
    employees
);
```

Filters compose naturally with other optics, enabling precise targeting deep within structures.

### Aggregation with Folds

Traversals support folding—aggregating all focused values into a single result:

```java
// Sum all salaries in a department
double totalSalary = allStaffSalaries.foldMap(
    Double::sum,
    0.0,
    department
);

// Count employees
int count = eachEmployee.foldMap(
    (a, b) -> a + b,
    0,
    e -> 1,
    department.staff()
);

// Collect into a set
Set<String> uniqueCities = allStaffCities.foldMap(
    Sets::union,
    Set.of(),
    Set::of,
    department
);
```

The `foldMap` operation generalises collection: any monoid (a type with an associative binary operation and identity) works.

---

## Composition Patterns

Understanding how optics compose is essential for effective use. Here's the composition table:

| First | Second | Result |
|-------|--------|--------|
| Lens | Lens | Lens |
| Lens | Prism | Optional |
| Lens | Traversal | Traversal |
| Prism | Lens | Optional |
| Prism | Prism | Prism |
| Prism | Traversal | Traversal |
| Traversal | Lens | Traversal |
| Traversal | Prism | Traversal |
| Traversal | Traversal | Traversal |

The pattern: composing with something "weaker" (that might not find anything, or might find many things) yields the weaker type.

### Building Deep Paths

In practice, you'll build paths incrementally:

```java
// Company → departments (lens to list)
// → each department (traversal over list)
// → manager (lens to employee)
// → address (lens to address)
// → city (lens to string)

Traversal<Company, String> allManagerCities =
    CompanyLenses.departments()
        .andThen(Traversals.list())
        .andThen(DepartmentLenses.manager())
        .andThen(EmployeeLenses.address())
        .andThen(AddressLenses.city());

// Relocate all managers to Manchester
Company relocated = allManagerCities.modify(_ -> "Manchester", company);
```

### Real-World Example: Updating Nested Orders

Consider an e-commerce domain:

```java
@GenerateLenses
public record Customer(String id, String name, List<Order> orders) {}

@GenerateLenses
public record Order(String orderId, List<LineItem> items, OrderStatus status) {}

@GenerateLenses
public record LineItem(String productId, int quantity, BigDecimal price) {}
```

To apply a 10% discount to all items across all orders for a customer:

```java
// Manual approach: ~20 lines of nested loops and reconstruction

// Optics approach: define the path once
Traversal<Customer, BigDecimal> allItemPrices =
    CustomerLenses.orders()
        .andThen(Traversals.list())
        .andThen(OrderLenses.items())
        .andThen(Traversals.list())
        .andThen(LineItemLenses.price());

// Apply discount
Customer discounted = allItemPrices.modify(
    price -> price.multiply(new BigDecimal("0.90")),
    customer
);
```

The path is declarative and reusable. Need to calculate the total value? Use the same path with `foldMap`.

---

## Effect-Polymorphic Operations: A Preview

So far, our optics have performed pure transformations. But real applications need effects: validation that might fail, state that accumulates, logging for debugging.

Higher-kinded-j's optics support *effect-polymorphic* operations through `modifyF`:

```java
// Pure modification
Employee updated = streetLens.modify(String::toUpperCase, employee);

// Effectful modification with Optional (might fail)
Optional<Employee> validated = streetLens.modifyF(
    OptionalKind.INSTANCE,
    street -> street.isBlank() ? Optional.empty() : Optional.of(street.trim()),
    employee
);

// Effectful modification with Either (might fail with error)
Either<ValidationError, Employee> checked = streetLens.modifyF(
    EitherKind.INSTANCE,
    street -> validateStreet(street),
    employee
);
```

The same optic—the same composed path—works with any effect. This is the power of higher-kinded types: abstracting over the computational context.

We'll explore `modifyF` fully in Article 5, where we'll use it for type-checking with error accumulation and interpretation with state. For now, know that the optics you're learning aren't limited to pure transformations.

---

## Introducing the Expression Language

Starting in Article 3, we'll build an expression language interpreter—the canonical showcase for optics. Here's a preview of the domain:

```java
@GeneratePrisms
public sealed interface Expr {
    @GenerateLenses record Literal(Object value) implements Expr {}
    @GenerateLenses record Variable(String name) implements Expr {}
    @GenerateLenses record Binary(Expr left, BinaryOp op, Expr right) implements Expr {}
    @GenerateLenses record Conditional(Expr cond, Expr then_, Expr else_) implements Expr {}
}

public enum BinaryOp { ADD, SUB, MUL, DIV, EQ, LT, GT, AND, OR }
```

This domain showcases every optic type:

- **Lenses** for accessing expression fields (`Binary.left`, `Conditional.cond`)
- **Prisms** for matching expression variants (is this a `Literal`? a `Binary`?)
- **Traversals** for visiting all sub-expressions recursively

We'll implement:
- Variable renaming across an entire expression tree
- Constant folding (evaluating `1 + 2` to `3` at compile time)
- Dead code elimination (removing unreachable branches)
- A complete interpreter using stateful evaluation

The expression language is small enough to understand completely, yet rich enough to demonstrate every optics pattern you'll need for real-world tree manipulation.

---

## Summary

This article covered the three fundamental optic types:

- **Lenses** focus on exactly one value (product types, "has-a")
- **Prisms** focus on one variant that might not match (sum types, "is-a")
- **Traversals** focus on zero or more values (collections, "has-many")

Key takeaways:

1. **Composition is the superpower**: Small, focused optics combine into powerful paths
2. **The type tells you what to expect**: Lens always succeeds; Prism might not; Traversal might find many
3. **Annotation-driven generation eliminates boilerplate**: `@GenerateLenses` and `@GeneratePrisms` do the mechanical work
4. **Effects come later**: The same optics work with pure transformations and effectful ones

In Article 3, we'll apply these fundamentals to build the expression language AST, demonstrating how lenses and prisms work together for real-world tree manipulation.

---

*Next: [Article 3: Building an Expression Language—The AST and Basic Optics](article-3-ast-basic-optics.md)*
