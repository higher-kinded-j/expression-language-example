# Article 1: The Immutability Gap

**Branch**: `article-1-immutability-gap`

This branch contains the companion code for Article 1 of the "Functional Optics for Modern Java" series.

## What's in This Branch

This branch demonstrates **the problem** that optics solve and provides a **quick win** showing how optics elegantly solve it.

### The Problem (`problem` package)

The `org.higherkindedj.article1.problem` package shows the painful reality of updating deeply nested immutable records in Java:

- **20+ lines of code** to change a single street address
- **Error-prone** reconstruction of every record in the path
- **No help from pattern matching** — which only aids reading, not writing

Run the problem demonstration:
```bash
java -cp build/classes/java/main org.higherkindedj.article1.problem.NestedUpdateProblem
```

### The Solution (`solution` package)

The `org.higherkindedj.article1.solution` package shows how optics solve this elegantly:

- **1 line of code** for the same update
- **Composable** lenses that combine naturally
- **Type-safe** with compile-time checking

Run the solution demonstration:
```bash
java -cp build/classes/java/main org.higherkindedj.article1.solution.OpticsSolution
```

## Key Concepts Introduced

1. **Lens** — Focuses on exactly one field within a structure
2. **Composition** — Lenses combine with `andThen()` to focus deeper
3. **Traversal** — Focuses on multiple values (e.g., all elements in a list)

## Code Structure

```
src/main/java/org/higherkindedj/article1/
├── problem/
│   ├── Address.java          # Simple record
│   ├── Employee.java         # Nested record
│   ├── Department.java       # Contains list
│   ├── Company.java          # Top-level
│   └── NestedUpdateProblem.java  # THE PROBLEM
│
└── solution/
    ├── Lens.java             # Core optic type
    ├── Traversal.java        # For collections
    ├── Address.java          # Record + Lenses
    ├── Employee.java         # Record + Lenses
    ├── Department.java       # Record + Lenses
    ├── Company.java          # Record + Lenses
    └── OpticsSolution.java   # THE SOLUTION
```

## Building

```bash
export JAVA_HOME=/opt/jdk24  # or wherever JDK 24 is installed
gradle compileJava
```

## JDK Requirements

This project uses **JDK 24** (Temurin/Adoptium). The manual lens implementations
demonstrate the concepts that higher-kinded-j would auto-generate with `@GenerateLenses`.

When Maven Central is accessible, uncomment the dependencies in `build.gradle.kts`
to use the full higher-kinded-j library with annotation processing.

## Spotless Configuration

The project includes Spotless configuration matching higher-kinded-j's style:
- Google Java Format
- MIT License headers
- Unix line endings

Enable by uncommenting the Spotless plugin in `build.gradle.kts` when the
Gradle Plugin Portal is accessible.

## What's Next

Article 2 will dive deeper into optics fundamentals:
- Lens laws and guarantees
- Prisms for sum types
- More sophisticated traversals
- Full higher-kinded-j integration
