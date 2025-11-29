# Article 4: Tree Traversals and Pattern Rewrites

**Branch**: `article-4-traversals-rewrites`

This branch contains the companion code for Article 4 of the "Functional Optics for Modern Java" series. It builds on Article 3 by introducing traversals for recursive AST manipulation.

## What's New in This Branch

Building on the foundation from Articles 1-3, this branch adds:

- **Traversal type** — Focus on zero or more elements within a structure
- **Expression traversals** — Visit all children or all descendants of an expression
- **Bottom-up and top-down transforms** — Recursive tree transformations with controlled ordering
- **Fold utilities** — Collect information from the entire tree (variables, operators, counts)
- **Enhanced optimiser** — Multiple passes using traversal-based transformations
- **Common subexpression detection** — Find repeated patterns for potential optimisation

## Running the Demos

```bash
gradle run
```

This runs all Article 4 demonstrations:

1. **TraversalDemo** — Children traversal, bottom-up/top-down transforms, information collection
2. **OptimiserDemo** — Constant folding, identity simplification, cascading optimisation

## Key Concepts Introduced

### Traversals

A `Traversal<S, A>` focuses on zero or more `A` values within an `S` structure:

```java
public interface Traversal<S, A> {
    S modify(Function<A, A> f, S source);
    List<A> getAll(S source);
    S set(A value, S source);
    <B> Traversal<S, B> andThen(Traversal<A, B> inner);
    Traversal<S, A> filtered(Predicate<A> predicate);
}
```

### Expression Traversals

```java
// Visit immediate children only
Traversal<Expr, Expr> children = ExprTraversal.children();

// Visit all nodes in the tree
Traversal<Expr, Expr> all = ExprTraversal.allDescendants();

// Transform from leaves to root
Expr result = ExprTraversal.transformBottomUp(expr, transform);

// Transform from root to leaves
Expr result = ExprTraversal.transformTopDown(expr, transform);
```

### Fold Utilities

```java
// Find all variables
Set<String> vars = ExprFold.findVariables(expr);

// Count nodes by type
NodeCounts counts = ExprFold.countNodes(expr);

// Find operators used
Set<BinaryOp> ops = ExprFold.findOperators(expr);

// Find common subexpressions
Map<Expr, Integer> common = ExprFold.findCommonSubexpressions(expr);

// Check if expression is constant
boolean isConst = ExprFold.isConstant(expr);
```

### Optimisation Passes

The optimiser runs multiple passes to fixed point:

1. **Constant folding** — `1 + 2` → `3`
2. **Identity simplification** — `x + 0` → `x`, `x * 1` → `x`
3. **Dead branch elimination** — `if true then a else b` → `a`

```java
Expr optimised = ExprOptimiser.optimise(expr);
```

### Bottom-Up vs Top-Down

| Order | Description | Use Case |
|-------|-------------|----------|
| Bottom-up | Children first, then parent | Constant folding, evaluation |
| Top-down | Parent first, then children | Macro expansion, early rewriting |

## Code Structure

```
src/main/java/org/higherkindedj/
├── article1/                    # From Article 1
├── article2/                    # From Article 2
├── article3/                    # From Article 3
│
└── article4/                    # NEW: Article 4 code
    ├── ast/                     # Expression Language AST
    │   ├── Expr.java            # Sealed interface + records
    │   └── BinaryOp.java        # Binary operators enum
    │
    ├── traversal/               # Traversal infrastructure
    │   ├── Traversal.java       # Generic traversal interface
    │   ├── ExprTraversal.java   # Expression-specific traversals
    │   ├── ExprFold.java        # Fold utilities for collection
    │   └── NodeCounts.java      # Node counting result type
    │
    ├── transform/               # Transformation utilities
    │   └── ExprOptimiser.java   # Multi-pass optimiser
    │
    └── demo/                    # Runnable demonstrations
        ├── Article4Demo.java    # Main entry point
        ├── TraversalDemo.java   # Traversal examples
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
- Pattern matching with guards (`when` clauses)

## What's Next

Article 5 will introduce effect-polymorphic operations:
- Type checking with `Validated` (accumulating errors)
- Interpretation with `State` (environment management)
- The Free monad DSL for composable transformations
- The same traversals working with different computational effects

## Previous Articles

- [Article 1: The Immutability Gap](docs/article-1-the-immutability-gap.md) — Problem and basic solution
- [Article 2: Optics Fundamentals](docs/article-2-optics-fundamentals.md) — Lens, Prism, Traversal
- [Article 3: AST and Basic Optics](docs/article-3-ast-basic-optics.md) — Expression language domain
