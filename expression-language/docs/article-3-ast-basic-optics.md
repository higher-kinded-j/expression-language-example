# Building an Expression Language — Part 1: The AST and Basic Optics

*Part 3 of the Functional Optics for Modern Java series*

In Articles 1 and 2, we established why optics matter and how they work. Now it's time to apply them to a real domain: an expression language interpreter. This is the canonical showcase for optics—the domain where they truly shine.

Over the next three articles, we'll build a complete expression language with parsing, type checking, optimisation, and interpretation. Along the way, you'll see how optics transform what would otherwise be tedious tree manipulation into elegant, composable operations.

---

## The Expression Language Domain

What exactly are we building? A small but powerful expression language suitable for:

- **Configuration expressions**: `if (env == "prod") then timeout * 2 else timeout`
- **Rule engines**: `price > 100 && customer.tier == "gold"`
- **Template systems**: `"Hello, " + user.name + "!"`
- **Domain-specific calculations**: `principal * (1 + rate) ^ years`

The language will support:
- Literal values (integers, booleans, strings)
- Variables with lexical scoping
- Binary operations (arithmetic, comparison, logical)
- Conditional expressions (if-then-else)

Our design goals are:
1. **Type-safe**: The compiler catches structural errors
2. **Immutable**: Expressions never change; transformations produce new trees
3. **Transformable**: Easy to analyse, optimise, and rewrite

This third goal is where optics become essential. An expression tree is a recursive structure where any node might contain arbitrarily nested sub-expressions. Transforming such trees manually—with pattern matching and reconstruction—quickly becomes unwieldy. Optics provide a disciplined approach.

---

## Designing the AST

An Abstract Syntax Tree (AST) represents the structure of code as a tree of nodes. Each node type corresponds to a language construct.

### Start Simple

We'll begin with a minimal four-variant AST:

```java
public sealed interface Expr {
    record Literal(Object value) implements Expr {}
    record Variable(String name) implements Expr {}
    record Binary(Expr left, BinaryOp op, Expr right) implements Expr {}
    record Conditional(Expr cond, Expr then_, Expr else_) implements Expr {}
}

public enum BinaryOp {
    ADD, SUB, MUL, DIV,    // Arithmetic
    EQ, NE, LT, LE, GT, GE, // Comparison
    AND, OR                 // Logical
}
```

This covers more than you might expect:
- `Literal(42)` — integer constant
- `Literal(true)` — boolean constant
- `Variable("x")` — variable reference
- `Binary(Variable("a"), ADD, Literal(1))` — `a + 1`
- `Conditional(Variable("flag"), Literal(1), Literal(0))` — `if flag then 1 else 0`

The recursive nature is already apparent: `Binary` contains two `Expr` children, and `Conditional` contains three. Any transformation must handle this recursion.

### Why Sealed Interfaces?

The `sealed` keyword ensures exhaustive matching. When we write a switch over `Expr`, the compiler verifies we handle all variants:

```java
String describe(Expr expr) {
    return switch (expr) {
        case Literal(var v) -> "Literal: " + v;
        case Variable(var n) -> "Variable: " + n;
        case Binary(var l, var op, var r) -> "Binary: " + op;
        case Conditional(_, _, _) -> "Conditional";
    };
}
```

If we later add a new variant, the compiler flags every incomplete switch. This is precisely what we want for a language implementation.

### Why Records?

Records give us:
- Immutability by default
- Automatic `equals()`, `hashCode()`, `toString()`
- Pattern matching with deconstruction
- A natural fit for optics (each component becomes a lens target)

The combination of sealed interfaces and records creates what functional programmers call an *algebraic data type* (ADT)—a sum of products that's both type-safe and pattern-matchable.

---

## Generating Optics for the AST

With higher-kinded-j, we can annotate our AST to generate optics automatically:

```java
@GeneratePrisms
public sealed interface Expr {
    @GenerateLenses record Literal(Object value) implements Expr {}
    @GenerateLenses record Variable(String name) implements Expr {}
    @GenerateLenses record Binary(Expr left, BinaryOp op, Expr right) implements Expr {}
    @GenerateLenses record Conditional(Expr cond, Expr then_, Expr else_) implements Expr {}
}
```

The annotation processor generates:

### Prisms for Each Variant

```java
// Generated: ExprPrisms.java
public final class ExprPrisms {
    public static Prism<Expr, Literal> literal() { ... }
    public static Prism<Expr, Variable> variable() { ... }
    public static Prism<Expr, Binary> binary() { ... }
    public static Prism<Expr, Conditional> conditional() { ... }
}
```

Each prism lets us:
- Check if an `Expr` is a specific variant
- Extract the variant if it matches
- Transform just that variant, leaving others unchanged

### Lenses for Each Field

```java
// Generated: LiteralLenses.java
public final class LiteralLenses {
    public static Lens<Literal, Object> value() { ... }
}

// Generated: BinaryLenses.java
public final class BinaryLenses {
    public static Lens<Binary, Expr> left() { ... }
    public static Lens<Binary, BinaryOp> op() { ... }
    public static Lens<Binary, Expr> right() { ... }
}
```

Each lens lets us:
- Get a field from a node
- Set a field, producing a new node
- Modify a field with a function

### The Power of Composition

The real magic happens when we compose these optics:

```java
// Focus on the left operand's value (if it's a literal)
Optionall<Binary, Object> leftLiteralValue =
    BinaryLenses.left()
        .andThen(ExprPrisms.literal())
        .andThen(LiteralLenses.value());

// Check if a binary expression has a literal on the left
Binary expr = new Binary(new Literal(5), ADD, new Variable("x"));
Optional<Object> value = leftLiteralValue.getOptional(expr);
// Optional[5]
```

We've navigated from `Binary` → `left` (Expr) → as `Literal` → `value`, all type-safe and composable.

---

## Basic Transformations

Let's implement some fundamental AST transformations using optics.

### Transforming Literals

Suppose we want to increment all integer literals by one:

```java
public static Expr incrementLiterals(Expr expr) {
    Prism<Expr, Literal> literalPrism = ExprPrisms.literal();

    return literalPrism.modify(lit -> {
        if (lit.value() instanceof Integer i) {
            return new Literal(i + 1);
        }
        return lit;
    }, expr);
}
```

But wait—this only transforms the top-level expression. If the literal is nested inside a `Binary`, it won't be touched. We need recursion.

### The Recursive Challenge

Here's the manual approach to transforming all literals in a tree:

```java
public static Expr incrementAllLiterals(Expr expr) {
    return switch (expr) {
        case Literal(var v) ->
            v instanceof Integer i ? new Literal(i + 1) : expr;
        case Variable(_) -> expr;
        case Binary(var l, var op, var r) ->
            new Binary(incrementAllLiterals(l), op, incrementAllLiterals(r));
        case Conditional(var c, var t, var e) ->
            new Conditional(
                incrementAllLiterals(c),
                incrementAllLiterals(t),
                incrementAllLiterals(e));
    };
}
```

This works, but it's tedious. Every transformation requires the same recursive boilerplate. We're manually threading the transformation through every node type.

### A Reusable Transformation Pattern

We can extract the recursion into a reusable function:

```java
public static Expr transformExpr(Expr expr, Function<Expr, Expr> transform) {
    // First, recursively transform children
    Expr transformed = switch (expr) {
        case Literal(_) -> expr;
        case Variable(_) -> expr;
        case Binary(var l, var op, var r) ->
            new Binary(transformExpr(l, transform), op, transformExpr(r, transform));
        case Conditional(var c, var t, var e) ->
            new Conditional(
                transformExpr(c, transform),
                transformExpr(t, transform),
                transformExpr(e, transform));
    };

    // Then apply the transformation to this node
    return transform.apply(transformed);
}
```

Now our increment becomes:

```java
Expr result = transformExpr(expr, e ->
    ExprPrisms.literal().modify(lit ->
        lit.value() instanceof Integer i ? new Literal(i + 1) : lit,
        e));
```

In Article 4, we'll see how traversals make this even more elegant. For now, let's work with what we have.

---

## Working with the Sum Type

Prisms shine when working with the variants of our sealed interface.

### Safe Type Checking

Traditional instanceof checks are verbose and error-prone:

```java
if (expr instanceof Binary binary) {
    if (binary.left() instanceof Literal leftLit) {
        // do something with leftLit
    }
}
```

With prisms, we compose the checks:

```java
Optionall<Expr, Object> leftLiteralValue =
    ExprPrisms.binary()
        .andThen(BinaryLenses.left())
        .andThen(ExprPrisms.literal())
        .andThen(LiteralLenses.value());

Optional<Object> value = leftLiteralValue.getOptional(expr);
```

The composed optic handles all the type checking internally.

### Conditional Transformation

Prisms let us transform specific variants while leaving others unchanged:

```java
// Double all integer literals, leave everything else alone
Expr doubled = ExprPrisms.literal().modify(lit -> {
    if (lit.value() instanceof Integer i) {
        return new Literal(i * 2);
    }
    return lit;
}, expr);
```

If `expr` isn't a `Literal`, it's returned unchanged. No explicit instanceof check needed.

### Pattern-Based Matching

We can combine multiple prisms to match complex patterns:

```java
// Match: Binary with ADD operator and Literal(0) on the right
// This is the pattern for "x + 0" which we can simplify to "x"
public static Optional<Expr> matchAddZero(Expr expr) {
    if (expr instanceof Binary(var left, BinaryOp op, Expr right)
        && op == BinaryOp.ADD
        && right instanceof Literal(Object v)
        && v.equals(0)) {
        return Optional.of(left);
    }
    return Optional.empty();
}
```

This pattern matching is where Java's native features work well alongside optics. Use pattern matching for complex structural tests; use optics for transformations and deep access.

---

## Building a Simple Optimiser

Let's put everything together to build a constant folder—an optimiser that evaluates constant expressions at compile time.

### Constant Folding

The idea is simple: if both operands of a binary expression are literals, we can compute the result:

```java
public static Expr foldConstants(Expr expr) {
    return transformExpr(expr, ExprOptimiser::foldBinary);
}

private static Expr foldBinary(Expr expr) {
    if (expr instanceof Binary(
            Literal(Object lv),
            BinaryOp op,
            Literal(Object rv))) {

        Object result = evaluate(lv, op, rv);
        if (result != null) {
            return new Literal(result);
        }
    }
    return expr;
}

private static Object evaluate(Object left, BinaryOp op, Object right) {
    if (left instanceof Integer l && right instanceof Integer r) {
        return switch (op) {
            case ADD -> l + r;
            case SUB -> l - r;
            case MUL -> l * r;
            case DIV -> r != 0 ? l / r : null;
            case EQ -> l.equals(r);
            case NE -> !l.equals(r);
            case LT -> l < r;
            case LE -> l <= r;
            case GT -> l > r;
            case GE -> l >= r;
            default -> null;
        };
    }
    if (left instanceof Boolean l && right instanceof Boolean r) {
        return switch (op) {
            case AND -> l && r;
            case OR -> l || r;
            case EQ -> l.equals(r);
            case NE -> !l.equals(r);
            default -> null;
        };
    }
    return null;
}
```

### Example: Folding `(1 + 2) * 3`

```java
Expr expr = new Binary(
    new Binary(new Literal(1), ADD, new Literal(2)),
    MUL,
    new Literal(3)
);

System.out.println("Before: " + format(expr));
// Before: ((1 + 2) * 3)

Expr optimised = foldConstants(expr);
System.out.println("After: " + format(optimised));
// After: 9
```

The optimiser transforms `((1 + 2) * 3)` to `9` in a single pass. The inner `1 + 2` is folded to `3`, then `3 * 3` is folded to `9`.

### Identity Simplification

We can add more optimisations:

```java
private static Expr simplifyIdentities(Expr expr) {
    if (expr instanceof Binary(var left, BinaryOp op, Literal(Object rv))) {
        // x + 0 = x, x * 1 = x
        if ((op == ADD && rv.equals(0)) || (op == MUL && rv.equals(1))) {
            return left;
        }
        // x * 0 = 0
        if (op == MUL && rv.equals(0)) {
            return new Literal(0);
        }
    }
    if (expr instanceof Binary(Literal(Object lv), BinaryOp op, var right)) {
        // 0 + x = x, 1 * x = x
        if ((op == ADD && lv.equals(0)) || (op == MUL && lv.equals(1))) {
            return right;
        }
        // 0 * x = 0
        if (op == MUL && lv.equals(0)) {
            return new Literal(0);
        }
    }
    return expr;
}
```

### Composing Optimisations

Multiple optimisation passes compose naturally:

```java
public static Expr optimise(Expr expr) {
    Expr result = expr;
    Expr previous;

    // Run until fixed point (no more changes)
    do {
        previous = result;
        result = transformExpr(result, e ->
            simplifyIdentities(foldBinary(e)));
    } while (!result.equals(previous));

    return result;
}
```

This runs both optimisations repeatedly until the expression stops changing. The immutability of our AST makes equality checking trivial—we can use `equals()` directly.

---

## What's Next

We've built the foundation:
- A clean AST using sealed interfaces and records
- Prisms for variant access
- Lenses for field access
- A recursive transformation pattern
- A working constant folder

But there's a limitation: our `transformExpr` function is hand-written boilerplate. Every time we add a new expression type, we must update it.

In Article 4, we'll introduce *traversals*—optics that focus on multiple values simultaneously. With a traversal over all sub-expressions, we can:

- Eliminate the manual recursion
- Collect all variables in an expression
- Find all function calls
- Implement sophisticated rewrite rules

We'll also tackle:
- **Dead code elimination**: Removing unreachable branches
- **Common subexpression elimination**: Identifying duplicate computations
- **More complex pattern rewrites**: Multi-step transformations

The expression language will grow to include let-bindings and lambdas, showcasing how optics scale with complexity.

---

*Next: [Article 4: Tree Traversals and Pattern Rewrites](article-4-traversals-rewrites.md)*
