# Article 3: AST and Basic Optics

**Branch**: `article-3-ast-basic-optics`

This branch contains the companion code for Article 3 of the "Functional Optics for Modern Java" series. It builds on Article 2 by introducing the Expression Language domain and demonstrating how Java 25's data-oriented programming features combine with higher-kinded-j optics to create elegant AST manipulation.

## Data-Oriented Programming in Action

This branch showcases Java 25's data-oriented programming (DOP) approach:

1. **Data as immutable values**: Records provide transparent, immutable data carriers
2. **Behaviour separate from data**: Operations are standalone functions, not embedded methods
3. **Pattern matching for polymorphism**: Switch expressions with guards replace virtual dispatch

Instead of the traditional Visitor pattern, we use sealed interfaces + records + pattern matching for cleaner, more maintainable AST code.

## What's New in This Branch

Building on the foundation from Articles 1 and 2, this branch adds:

- **Expression Language AST**: A complete sealed interface hierarchy using DOP principles
- **Auto-generated-style optics**: Lenses and prisms for the AST (simulating higher-kinded-j generation)
- **Expression transformations**: Bottom-up and top-down tree traversal utilities
- **Expression optimiser**: Constant folding and identity simplification using Java 25 patterns
- **Optic composition**: Prism-to-lens composition for deep AST access

## Java 25 Features Demonstrated

This code showcases modern Java 25 capabilities:

- **Switch expressions with record patterns**: Deconstruct nested records in one expression
- **Pattern guards with `when` clauses**: Add conditions to pattern matches
- **Multi-pattern cases**: Combine related patterns: `case Literal(_), Variable(_) -> ...`
- **Unnamed patterns (`_`)**: Ignore components you don't need
- **Sealed interfaces**: Exhaustiveness checking for sum types

## Running the Demos

```bash
gradle run
```

This runs all Article 3 demonstrations:

1. **ExprDemo**: Building expressions, using prisms and lenses, pattern matching
2. **OptimiserDemo**: Constant folding, identity simplification, conditional elimination

## Key Concepts Introduced

### Expression Language AST (DOP Style)

A sealed interface hierarchy representing expressions as pure data:

```java
sealed interface Expr {
  record Literal(Object value) implements Expr {}
  record Variable(String name) implements Expr {}
  record Binary(Expr left, BinaryOp op, Expr right) implements Expr {}
  record Conditional(Expr cond, Expr then_, Expr else_) implements Expr {}
}
```

### Why Not Visitor Pattern?

Traditional OOP uses Visitor for AST operations. Compare:

```java
// Visitor: scattered logic, boilerplate
class ConstantFoldingVisitor implements ExprVisitor<Expr> {
    @Override public Expr visitBinary(Binary expr) { ... }
    @Override public Expr visitLiteral(Literal expr) { ... }
    // ... more visit methods
}

// DOP: logic in one place, declarative
Expr foldConstants(Expr expr) {
    return switch (expr) {
        case Binary(Literal(var l), var op, Literal(var r)) ->
            evaluate(l, op, r).map(Literal::new).orElse(expr);
        case Binary(var l, var op, var r) ->
            new Binary(foldConstants(l), op, foldConstants(r));
        case Literal _, Variable _ -> expr;
        // ...
    };
}
```

The DOP version is shorter, clearer, and has compiler-checked exhaustiveness.

### Optics for the AST

**Prisms** for each Expr variant:
- `ExprPrisms.literal()`: Focus on Literal expressions
- `ExprPrisms.variable()`: Focus on Variable expressions
- `ExprPrisms.binary()`: Focus on Binary expressions
- `ExprPrisms.conditional()`: Focus on Conditional expressions

**Lenses** for each record field:
- `LiteralLenses.value()`: Access the value in a Literal
- `VariableLenses.name()`: Access the name in a Variable
- `BinaryLenses.left()`, `op()`, `right()`: Access Binary components
- `ConditionalLenses.cond()`, `then_()`, `else_()`: Access Conditional components

### Composition Table

| First | Second | Result |
|-------|--------|--------|
| Lens | Lens | Lens |
| Lens | Prism | Traversal |
| Lens | Traversal | Traversal |
| Prism | Lens | Traversal |
| Prism | Prism | Prism |
| Traversal | * | Traversal |

### Expression Optimiser with Java 25 Pattern Guards

Clean, declarative optimisation rules:

```java
// Java 25: Switch expression with 'when' guards
return switch (expr) {
    case Binary(var left, BinaryOp.ADD, Literal(Integer v)) when v == 0 -> left;
    case Binary(var left, BinaryOp.MUL, Literal(Integer v)) when v == 1 -> left;
    case Binary(_, BinaryOp.MUL, Literal(Integer v)) when v == 0 -> new Literal(0);
    default -> expr;
};
```

Three optimisation passes:

1. **Constant folding**: Evaluate constant expressions at compile time
   - `1 + 2` → `3`
   - `true && false` → `false`

2. **Identity simplification**: Remove redundant operations
   - `x + 0` → `x`
   - `x * 1` → `x`
   - `x * 0` → `0`
   - `x && true` → `x`

3. **Conditional simplification**: Eliminate branches with constant conditions
   - `if true then a else b` → `a`
   - `if false then a else b` → `b`

## Code Structure

```
src/main/java/org/higherkindedj/
├── article1/                    # From Article 1
│   ├── problem/                 # The nested update problem
│   └── solution/                # Basic lens solution
│
├── article2/                    # From Article 2
│   ├── optics/                  # Core optic types
│   ├── domain/                  # Example domain types
│   └── demo/                    # Article 2 demos
│
└── article3/                    # NEW: Article 3 code
    ├── ast/                     # Expression Language AST
    │   ├── Expr.java            # Sealed interface + records (DOP style)
    │   └── BinaryOp.java        # Binary operators enum
    │
    ├── optics/                  # Optics for the AST
    │   ├── Lens.java            # Lens definition
    │   ├── Prism.java           # Prism definition
    │   ├── Optionall.java       # Affine traversal
    │   ├── ExprPrisms.java      # Prisms for Expr variants
    │   ├── LiteralLenses.java   # Lenses for Literal
    │   ├── VariableLenses.java  # Lenses for Variable
    │   ├── BinaryLenses.java    # Lenses for Binary
    │   └── ConditionalLenses.java  # Lenses for Conditional
    │
    ├── transform/               # Tree transformations
    │   ├── ExprTransform.java   # Bottom-up/top-down traversal (DOP style)
    │   └── ExprOptimiser.java   # Constant folding with Java 25 patterns
    │
    └── demo/                    # Runnable demonstrations
        ├── ExprDemo.java        # AST building and optics
        └── OptimiserDemo.java   # Optimisation examples
```

## Building

```bash
gradle compileJava
```

## JDK Requirements

This project uses **JDK 25** with the higher-kinded-j library (version 0.2.2-SNAPSHOT).

Java 25 features used:
- **Switch expressions with record patterns**: Nested deconstruction
- **Pattern guards (`when` clauses)**: Conditional pattern matching
- **Multi-pattern cases**: `case A, B -> ...`
- **Unnamed patterns (`_`)**: Ignore unneeded components
- **Sealed interfaces**: Exhaustiveness checking

The optics implementations simulate what Higher-Kinded-J auto-generates with `@GenerateLenses` and `@GeneratePrisms`.

## Spotless Configuration

The project uses Spotless for code formatting matching Higher-Kinded-J's style:
- Google Java Format
- MIT License headers
- Unix line endings

## What's Next

Article 4 will introduce traversals for recursive structures:
- Implementing Traversal for expression trees
- Folding and collecting over AST nodes
- Building queries with traversal composition
- More sophisticated transformations

Higher-Kinded-J's optics generation will be central to this work, enabling us to navigate and transform the AST with minimal boilerplate. The `@GeneratePrisms` annotation on our sealed `Expr` interface will generate type-safe accessors for each variant, whilst `@GenerateLenses` on the records will provide field access. Together, they compose into powerful traversals that can reach any part of the expression tree.

## Previous Articles

- [Article 1: The Immutability Gap](docs/article-1-the-immutability-gap.md): Problem and basic solution
- [Article 2: Optics Fundamentals](docs/article-2-optics-fundamentals.md): Lens, Prism, Traversal
