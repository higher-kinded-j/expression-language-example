# Article 5: Effect-Polymorphic Optics with Higher-Kinded-J

**Branch**: `article-5-effect-polymorphic`

This branch contains the companion code for Article 5 of the "Functional Optics for Modern Java" series. It builds on Article 4 by introducing effect-polymorphic operations using Higher-Kinded-J's real effect types: the same optics working with different computational contexts.

## What's New in This Branch

Building on the foundation from Articles 1-4, this branch demonstrates:

- **Effect polymorphism**: Abstract over computational context with Higher-Kinded-J's `Kind<F, A>`
- **Validated type**: Use `org.higherkindedj.hkt.validated.Validated` to accumulate all errors
- **State monad**: Use `org.higherkindedj.hkt.state.State` for implicit context threading
- **Semigroup**: Use `org.higherkindedj.hkt.Semigroups.list()` for error accumulation
- **Type checker**: Expression type checking using HKJ's `ValidatedMonad`
- **Interpreter**: Expression evaluation using HKJ's `StateMonad`
- **modifyF operations**: Bridge between optics and Higher-Kinded-J effects

## Running the Demos

```bash
gradle run
```

This runs all Article 5 demonstrations:

1. **TypeCheckerDemo**: Type checking with error accumulation using HKJ Validated
2. **InterpreterDemo**: Expression evaluation with HKJ State monad
3. **EffectPolymorphicDemo**: Same traversal, different HKJ effects

## Key Concepts: Using Higher-Kinded-J

### Effect Polymorphism with Kind<F, A>

Higher-Kinded-J's `Kind<F, A>` enables writing code once that works with many different effects:

```java
// The same traversal signature works with any effect F
<F> Kind<F, S> modifyF(
    Function<A, Kind<F, A>> f,
    S source,
    Applicative<F> applicative
);
```

### Higher-Kinded-J's Validated Type

From `org.higherkindedj.hkt.validated`:

```java
// Create validations
Validated<List<TypeError>, Type> success = Validated.valid(Type.INT);
Validated<List<TypeError>, Type> failure = Validated.invalid(TypeError.single("error"));

// Get the ValidatedMonad with a Semigroup for error accumulation
ValidatedMonad<List<TypeError>> monad = ValidatedMonad.instance(Semigroups.list());

// Combine validations, collecting ALL errors using Applicative.map2
Kind<ValidatedKind.Witness<List<TypeError>>, Type> result = monad.map2(
    ValidatedKindHelper.INSTANCE.widen(leftValidation),
    ValidatedKindHelper.INSTANCE.widen(rightValidation),
    (left, right) -> combineTypes(left, right)
);
```

### Higher-Kinded-J's State Monad

From `org.higherkindedj.hkt.state`:

```java
// Create state actions
State<Environment, Object> pure = State.pure(value);
State<Environment, Environment> getEnv = State.get();
State<Environment, Unit> setEnv = State.set(newEnv);
State<Environment, Unit> modifyEnv = State.modify(env -> env.bind("x", 42));

// Compose with flatMap
State<Environment, Object> interpret(Expr expr) {
    return switch (expr) {
        case Variable(var name) ->
            State.<Environment>get().map(env -> env.lookup(name));
        case Binary(var left, var op, var right) ->
            interpret(left).flatMap(l ->
                interpret(right).map(r ->
                    applyOp(op, l, r)));
        // ...
    };
}

// Run the computation
StateTuple<Environment, Object> result = interpret(expr).run(initialEnv);
Object value = result._1();
Environment finalEnv = result._2();
```

### Effect Types and Their Behaviours

All from Higher-Kinded-J:

| Effect Type | Package | Behaviour |
|-------------|---------|-----------|
| `Id` | `org.higherkindedj.hkt.id` | Pure computation |
| `Maybe` | `org.higherkindedj.hkt.maybe` | Might fail |
| `Either<E, ?>` | `org.higherkindedj.hkt.either` | Fail-fast errors |
| `Validated<E, ?>` | `org.higherkindedj.hkt.validated` | Error accumulation |
| `State<S, ?>` | `org.higherkindedj.hkt.state` | Stateful computation |
| `IO` | `org.higherkindedj.hkt.io` | Side effects |

### Applicative vs Monad

| Abstraction | Key Operation | Use Case |
|-------------|---------------|----------|
| Applicative | `map2(fa, fb, combine)` | Independent computations |
| Monad | `flatMap(fa, f)` | Dependent computations |

Higher-Kinded-J's `Validated` uses Applicative semantics for `ap` (error accumulation) and Monad semantics for `flatMap` (fail-fast). This intentional deviation from monad laws enables error accumulation in applicative contexts.

## Code Structure

```
src/main/java/org/higherkindedj/
├── article1-4/                  # From previous articles
│
└── article5/                    # NEW: Article 5 code
    ├── typecheck/               # Type checking using HKJ Validated
    │   ├── Type.java            # Expression types (INT, BOOL, STRING)
    │   ├── TypeError.java       # Errors with Semigroups.list()
    │   ├── TypeEnv.java         # Type environment
    │   └── ExprTypeChecker.java # Uses ValidatedMonad for accumulation
    │
    ├── interpret/               # Interpretation using HKJ State
    │   ├── Environment.java     # Variable bindings
    │   └── ExprInterpreter.java # Uses State monad for threading
    │
    └── demo/                    # Runnable demonstrations
        ├── Article5Demo.java    # Main entry point
        ├── TypeCheckerDemo.java # Validated examples
        ├── InterpreterDemo.java # State examples
        └── EffectPolymorphicDemo.java  # modifyF with HKJ effects
```

Note: This article uses Higher-Kinded-J's real effect types directly. There is no separate `effect/` package because we leverage the library's production-ready implementations.

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
