# Article 2: Optics Fundamentals

**Branch**: `article-2-optics-fundamentals`

This branch contains the companion code for Article 2 of the "Functional Optics for Modern Java" series. It builds on Article 1 by providing a comprehensive exploration of all three core optic types.

## What's New in This Branch

Building on the foundation from Article 1, this branch adds:

- **Complete optics implementation**: Lens, Prism, and Traversal
- **Prisms for sum types**: Type-safe access to sealed interface variants
- **Traversals for collections**: Bulk operations with filtering and aggregation
- **Composition patterns**: Deep path building through multiple optic types
- **Expression Language preview**: AST structure for Articles 3-5

## Running the Demos

```bash
gradle run
```

This runs all Article 2 demonstrations:

1. **LensDemo**: Basic lens operations, composition, and lens laws
2. **PrismDemo**: Sum type access, type-safe downcasting
3. **TraversalDemo**: Collection traversals, filtering, aggregation
4. **CompositionDemo**: Deep path composition, manual vs optics comparison
5. **ExpressionPreviewDemo**: Preview of the AST for the expression language

## Key Concepts Introduced

### Lenses (Product Types)
- Focus on exactly one field that always exists
- Composition with `andThen()` for deep access
- Laws: get-set, set-get, set-set

### Prisms (Sum Types)
- Focus on one variant of a sealed interface
- `getOptional()` returns `Optional` (might not match)
- `build()` constructs the sum type (always succeeds)
- `modify()` transforms only if it matches

### Traversals (Collections)
- Focus on zero or more elements
- `modify()` transforms all focused elements
- `filtered()` targets only matching elements
- `foldMap()` aggregates into a single result

### Composition Table

| First | Second | Result |
|-------|--------|--------|
| Lens | Lens | Lens |
| Lens | Prism | Traversal |
| Lens | Traversal | Traversal |
| Prism | Lens | Traversal |
| Prism | Prism | Prism |
| Traversal | * | Traversal |

## Code Structure

```
src/main/java/org/higherkindedj/
├── article1/                    # From Article 1
│   ├── problem/                 # The nested update problem
│   └── solution/                # Basic lens solution
│
└── article2/                    # NEW: Article 2 code
    ├── optics/                  # Core optic types
    │   ├── Lens.java            # Product type access
    │   ├── Prism.java           # Sum type access
    │   ├── Optionall.java       # Affine traversal
    │   └── Traversal.java       # Collection operations
    │
    ├── domain/                  # Example domain types
    │   ├── Address.java         # With lens factories
    │   ├── Employee.java        # With lens factories
    │   ├── Department.java      # With lens factories
    │   ├── Company.java         # With lens factories
    │   ├── Shape.java           # Sealed interface + prisms
    │   ├── Customer.java        # E-commerce domain
    │   ├── Order.java           # E-commerce domain
    │   ├── LineItem.java        # E-commerce domain
    │   └── OrderStatus.java     # Order status enum
    │
    └── demo/                    # Runnable demonstrations
        ├── Article2Demo.java    # Main entry point
        ├── LensDemo.java        # Lens operations
        ├── PrismDemo.java       # Prism operations
        ├── TraversalDemo.java   # Traversal operations
        ├── CompositionDemo.java # Composition patterns
        └── ExpressionPreviewDemo.java  # AST preview
```

## Building

```bash
gradle compileJava
```

## JDK Requirements

This project uses **JDK 25** with the higher-kinded-j library (version 0.2.2-SNAPSHOT).

Features used:
- Unnamed variables (`_`) in lambda expressions
- Record patterns in switch expressions
- Sealed interfaces

The manual optics implementations demonstrate the concepts that Higher-Kinded-J auto-generates with `@GenerateLenses` and `@GeneratePrisms`.

## Spotless Configuration

The project uses Spotless for code formatting matching Higher-Kinded-J's style:
- Google Java Format
- MIT License headers
- Unix line endings

## What's Next

Article 3 will begin building the Expression Language interpreter:
- Full AST definition with sealed interfaces
- Recursive traversals for tree operations
- Variable renaming and constant folding
- Pattern-based transformations

Higher-Kinded-J's optics generation will be central to this work, enabling us to navigate and transform the AST with minimal boilerplate. The `@GeneratePrisms` annotation on our sealed `Expr` interface will generate type-safe accessors for each variant, whilst `@GenerateLenses` on the records will provide field access. Together, they compose into powerful traversals that can reach any part of the expression tree.

## Previous Article

- [Article 1: The Immutability Gap](docs/article-1-the-immutability-gap.md): Problem and basic solution
