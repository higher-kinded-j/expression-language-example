# Article 3: AST and Basic Optics

**Branch**: `article-3-ast-basic-optics`

This branch contains the companion code for Article 3 of the "Functional Optics for Modern Java" series. It builds on Article 2 by introducing the Expression Language domain and applying optics to AST manipulation.

## What's New in This Branch

Building on the foundation from Articles 1 and 2, this branch adds:

- **Expression Language AST** — A complete sealed interface hierarchy for expressions
- **Auto-generated-style optics** — Lenses and prisms for the AST (simulating higher-kinded-j generation)
- **Expression transformations** — Bottom-up and top-down tree traversal utilities
- **Expression optimiser** — Constant folding and identity simplification
- **Optic composition** — Prism-to-lens composition for deep AST access

## Running the Demos

```bash
gradle run
```

This runs all Article 3 demonstrations:

1. **ExprDemo** — Building expressions, using prisms and lenses, pattern matching
2. **OptimiserDemo** — Constant folding, identity simplification, conditional elimination

## Key Concepts Introduced

### Expression Language AST

A sealed interface hierarchy representing expressions:

```java
sealed interface Expr {
  record Literal(Object value) implements Expr {}
  record Variable(String name) implements Expr {}
  record Binary(Expr left, BinaryOp op, Expr right) implements Expr {}
  record Conditional(Expr cond, Expr then_, Expr else_) implements Expr {}
}
```

### Optics for the AST

**Prisms** for each Expr variant:
- `ExprPrisms.literal()` — Focus on Literal expressions
- `ExprPrisms.variable()` — Focus on Variable expressions
- `ExprPrisms.binary()` — Focus on Binary expressions
- `ExprPrisms.conditional()` — Focus on Conditional expressions

**Lenses** for each record field:
- `LiteralLenses.value()` — Access the value in a Literal
- `VariableLenses.name()` — Access the name in a Variable
- `BinaryLenses.left()`, `op()`, `right()` — Access Binary components
- `ConditionalLenses.cond()`, `then_()`, `else_()` — Access Conditional components

### Expression Transformations

Recursive transformation patterns for tree operations:

```java
// Transform all nodes from leaves to root
ExprTransform.transformBottomUp(expr, transform);

// Transform all nodes from root to leaves
ExprTransform.transformTopDown(expr, transform);
```

### Expression Optimiser

Three optimisation passes:

1. **Constant folding** — Evaluate constant expressions at compile time
   - `1 + 2` → `3`
   - `true && false` → `false`

2. **Identity simplification** — Remove redundant operations
   - `x + 0` → `x`
   - `x * 1` → `x`
   - `x * 0` → `0`
   - `x && true` → `x`

3. **Conditional simplification** — Eliminate branches with constant conditions
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
    │   ├── Expr.java            # Sealed interface + records
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
    │   ├── ExprTransform.java   # Bottom-up/top-down traversal
    │   └── ExprOptimiser.java   # Constant folding, simplification
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

Features used:
- Unnamed variables (`_`) in lambda expressions and patterns
- Record patterns in switch expressions
- Sealed interfaces
- Pattern matching for instanceof

The optics implementations simulate what higher-kinded-j auto-generates with `@GenerateLenses` and `@GeneratePrisms`.

## What's Next

Article 4 will introduce traversals for recursive structures:
- Implementing Traversal for expression trees
- Folding and collecting over AST nodes
- Building queries with traversal composition
- More sophisticated transformations

## Previous Articles

- [Article 1: The Immutability Gap](docs/article-1-the-immutability-gap.md) — Problem and basic solution
- [Article 2: Optics Fundamentals](docs/article-2-optics-fundamentals.md) — Lens, Prism, Traversal
