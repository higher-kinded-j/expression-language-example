# Article 5: Effect-Polymorphic Optics

**Branch**: `article-5-effect-polymorphic`

This branch contains the companion code for Article 5 of the "Functional Optics for Modern Java" series. It builds on Article 4 by introducing effect-polymorphic operations: the same optics working with different computational contexts.

## What's New in This Branch

Building on the foundation from Articles 1-4, this branch adds:

- **Effect polymorphism**: Abstract over computational context with `Kind<F, A>`
- **Validated type**: Accumulate all errors instead of failing on the first
- **State monad**: Thread context through computations implicitly
- **Type checker**: Expression type checking with error accumulation
- **Interpreter**: Expression evaluation with environment threading
- **modifyF operations**: Bridge between optics and effects

## Running the Demos

```bash
gradle run
```

This runs all Article 5 demonstrations:

1. **TypeCheckerDemo**: Type checking with error accumulation
2. **InterpreterDemo**: Expression evaluation with State monad
3. **EffectPolymorphicDemo**: Same traversal, different effects

## Key Concepts Introduced

### Effect Polymorphism

Write code once that works with many different effects:

```java
// The same traversal signature works with any effect F
<F> Kind<F, S> modifyF(
    Function<A, Kind<F, A>> f,
    S source,
    Applicative<F> applicative
);
```

### The Validated Type

Accumulate errors instead of short-circuiting:

```java
public sealed interface Validated<E, A> {
    record Valid<E, A>(A value) implements Validated<E, A> {}
    record Invalid<E, A>(E errors) implements Validated<E, A> {}
}

// Combine validations, collecting all errors
Validated<TypeErrors, Type> result = ValidatedKind.APPLICATIVE.map2(
    leftValidation,
    rightValidation,
    (left, right) -> combineTypes(left, right)
);
```

### The State Monad

Thread state through computations implicitly:

```java
public record State<S, A>(Function<S, Pair<A, S>> runState) {
    public static <S, A> State<S, A> of(A value);
    public static <S> State<S, S> get();
    public static <S> State<S, Void> put(S newState);
    public <B> State<S, B> flatMap(Function<A, State<S, B>> f);
}

// Interpret with environment threading
State<Environment, Object> interpret(Expr expr) {
    return switch (expr) {
        case Variable(var name) ->
            State.<Environment>get().map(env -> env.lookup(name));
        // ...
    };
}
```

### Effect Types and Their Behaviours

| Effect Type | `Kind<F, A>` represents | Behaviour |
|-------------|------------------------|-----------|
| `Identity` | Just `A` | Pure computation |
| `Optional` | `A` or nothing | Might fail |
| `Either<E, ?>` | `A` or error `E` | Fail-fast errors |
| `Validated<E, ?>` | `A` or accumulated errors | Error accumulation |
| `State<S, ?>` | `A` with state `S` | Stateful computation |

### Applicative vs Monad

| Abstraction | Key Operation | Use Case |
|-------------|---------------|----------|
| Applicative | `map2(fa, fb, combine)` | Independent computations |
| Monad | `flatMap(fa, f)` | Dependent computations |

`Validated` is Applicative but not Monad: this enables error accumulation.

## Code Structure

```
src/main/java/org/higherkindedj/
├── article1-4/                  # From previous articles
│
└── article5/                    # NEW: Article 5 code
    ├── effect/                  # Effect type definitions
    │   ├── Validated.java       # Error-accumulating validation
    │   ├── ValidatedKind.java   # Higher-kinded encoding
    │   ├── State.java           # State monad
    │   └── StateKind.java       # Higher-kinded encoding
    │
    ├── typecheck/               # Type checking infrastructure
    │   ├── Type.java            # Expression types
    │   ├── TypeError.java       # Type error representation
    │   ├── TypeEnv.java         # Type environment
    │   └── ExprTypeChecker.java # The type checker
    │
    ├── interpret/               # Interpretation infrastructure
    │   ├── Environment.java     # Variable bindings
    │   └── ExprInterpreter.java # The interpreter
    │
    └── demo/                    # Runnable demonstrations
        ├── Article5Demo.java    # Main entry point
        ├── TypeCheckerDemo.java # Type checking examples
        ├── InterpreterDemo.java # Interpretation examples
        └── EffectPolymorphicDemo.java  # Effect polymorphism examples
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
- **Sealed interfaces**: Exhaustiveness checking for sum types
- **Records**: Immutable data carriers

## Spotless Configuration

The project uses Spotless for code formatting matching Higher-Kinded-J's style:
- Google Java Format
- MIT License headers
- Unix line endings

## What's Next

Article 6 will complete the expression language with parsing:
- Parser combinators using Higher-Kinded-J's applicative style
- Error recovery for malformed input
- Source location tracking through the pipeline
- End-to-end demonstration from source text to result

Higher-Kinded-J's `Alternative` type class will enable choice and repetition in parsers, whilst the same applicative patterns we used for type checking will structure the parser combinators themselves.

## Previous Articles

- [Article 1: The Immutability Gap](docs/article-1-the-immutability-gap.md): Problem and basic solution
- [Article 2: Optics Fundamentals](docs/article-2-optics-fundamentals.md): Lens, Prism, Traversal
- [Article 3: AST and Basic Optics](docs/article-3-ast-basic-optics.md): Expression language domain
- [Article 4: Tree Traversals](docs/article-4-traversals-rewrites.md): Recursive AST manipulation
