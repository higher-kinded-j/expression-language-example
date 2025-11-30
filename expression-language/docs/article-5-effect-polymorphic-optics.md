# Building an Expression Language: Part 3, Effect-Polymorphic Optics

*Part 5 of the Functional Optics for Modern Java series*

In Article 4, we built traversals that visit every node in our expression tree. We implemented constant folding, identity simplification, and dead branch elimination. But all our transformations were pure: they took an expression and returned a new expression, with no side effects.

Real compilers and interpreters need more. Type checking should report *all* errors, not just the first one. Interpretation must track variable bindings as it descends through the tree. Logging might help debug complex transformations. These are *effects*, and they change everything about how we structure our code.

This is where Higher-Kinded-J reveals its full potential. The same traversals we wrote in Article 4 will work unchanged with effectful operations. We simply swap the effect type.

---

## The Problem with Effects

Consider type checking. A naive approach fails on the first error:

```java
public Type typeCheck(Expr expr, Environment env) throws TypeError {
    return switch (expr) {
        case Literal(Integer _) -> Type.INT;
        case Literal(Boolean _) -> Type.BOOL;
        case Variable(var name) -> {
            Type t = env.lookup(name);
            if (t == null) throw new TypeError("Undefined variable: " + name);
            yield t;
        }
        case Binary(var left, var op, var right) -> {
            Type leftType = typeCheck(left, env);   // Might throw
            Type rightType = typeCheck(right, env); // Never reached if left fails
            yield checkBinaryOp(op, leftType, rightType);
        }
        // ...
    };
}
```

The problem: if `left` has an error, we never check `right`. Users see one error, fix it, recompile, see another error, and repeat. This frustrating cycle is avoidable.

We want *error accumulation*: collect all type errors in a single pass, then report them together. But this requires threading an error collection through every recursive call. The code becomes cluttered with accumulator parameters.

Similarly, interpretation needs environment threading:

```java
public Object interpret(Expr expr, Environment env) {
    return switch (expr) {
        case Literal(var v) -> v;
        case Variable(var name) -> env.lookup(name);
        case Binary(var left, var op, var right) -> {
            Object leftVal = interpret(left, env);
            Object rightVal = interpret(right, env);
            return applyOp(op, leftVal, rightVal);
        }
        case Conditional(var cond, var then_, var else_) -> {
            Object condVal = interpret(cond, env);
            if ((Boolean) condVal) {
                return interpret(then_, env);
            } else {
                return interpret(else_, env);
            }
        }
    };
}
```

This looks clean, but what if we add `let` bindings that extend the environment? Or mutable references? The environment threading becomes explicit and error-prone.

---

## Effect Polymorphism: The Core Idea

Effect polymorphism means writing code once that works with many different effects. Instead of hardcoding error handling or state threading, we abstract over the *computational context*.

In Higher-Kinded-J, this abstraction is the `Kind<F, A>` type: a value of type `A` wrapped in some effect `F`. Different choices of `F` give different behaviours:

| Effect Type | `Kind<F, A>` represents | Behaviour |
|-------------|------------------------|-----------|
| `Identity` | Just `A` | Pure computation, no effects |
| `Optional` | `A` or nothing | Computation that might fail |
| `Either<E, ?>` | `A` or error `E` | Fail-fast error handling |
| `Validated<E, ?>` | `A` or accumulated errors | Error accumulation |
| `State<S, ?>` | `A` with state `S` | Stateful computation |
| `IO` | Deferred `A` | Side effects |

The key insight: if we write our traversals to work with any `Kind<F, A>`, we get all these behaviours for free.

---

## The modifyF Operation

Every optic in Higher-Kinded-J supports `modifyF`, which lifts a transformation into an effectful context:

```java
public interface Traversal<S, A> {
    /**
     * Apply an effectful transformation to all focused elements.
     *
     * @param f the effectful transformation
     * @param source the structure to transform
     * @param applicative the Applicative instance for effect F
     * @return the transformed structure wrapped in effect F
     */
    <F> Kind<F, S> modifyF(
        Function<A, Kind<F, A>> f,
        S source,
        Applicative<F> applicative
    );
}
```

The `Applicative<F>` parameter provides two essential operations:

1. **`of(a)`**: Wrap a pure value in the effect (also called `pure`)
2. **`map2(fa, fb, combine)`**: Combine two effectful values

With just these two operations, we can sequence independent computations while accumulating their effects. For dependent computations (where the result of one affects what we do next), we need `Monad` and its `flatMap`.

### Example: Pure Transformation

For pure transformations, we can use the optics library's `Traversals.modify` utility, which handles the `Id` effect internally:

```java
import org.higherkindedj.optics.Traversal;
import org.higherkindedj.optics.util.Traversals;

Traversal<Expr, Expr> children = ExprTraversal.children();

// Pure transformation: double all literals
Expr result = Traversals.modify(children, e -> {
    if (e instanceof Literal(Integer i)) {
        return new Literal(i * 2);
    }
    return e;
}, expression);
```

### Example: Stateful Transformation

Using Higher-Kinded-J's `State` monad, transformations can track state:

```java
import org.higherkindedj.hkt.Kind;
import org.higherkindedj.hkt.state.State;
import org.higherkindedj.hkt.state.StateKind;
import org.higherkindedj.hkt.state.StateKindHelper;
import org.higherkindedj.hkt.state.StateMonad;
import org.higherkindedj.hkt.state.StateTuple;

StateMonad<Integer> stateMonad = new StateMonad<>();

// Count and transform literals
Kind<StateKind.Witness<Integer>, Expr> stateKind = children.modifyF(
    e -> {
        if (e instanceof Literal(Integer i)) {
            State<Integer, Expr> countAndTransform =
                State.<Integer>modify(count -> count + 1)
                     .map(v -> new Literal(i * 10));
            return StateKindHelper.STATE.widen(countAndTransform);
        }
        return StateKindHelper.STATE.widen(State.pure(e));
    },
    expression,
    stateMonad
);

StateTuple<Integer, Expr> result =
    StateKindHelper.STATE.<Integer, Expr>narrow(stateKind).run(0);
Expr transformed = result.value();
int count = result.state();
```

---

## Type Checking with Validated

`Validated<E, A>` is the key to error accumulation. Unlike `Either`, which short-circuits on the first error, `Validated` collects all errors before failing.

### The Validated Type

```java
public sealed interface Validated<E, A> {
    record Valid<E, A>(A value) implements Validated<E, A> {}
    record Invalid<E, A>(E errors) implements Validated<E, A> {}
}
```

The crucial difference from `Either`: `Validated` forms an `Applicative` but *not* a `Monad`. This isn't a limitation; it's the feature. Without `flatMap`, independent validations run in parallel (logically), accumulating all their errors.

### Building a Type Checker

First, define our type and error types:

```java
public enum Type { INT, BOOL, STRING }

public record TypeError(String message, SourceLocation location) {}

public record TypeErrors(List<TypeError> errors) {
    public static TypeErrors single(String message, SourceLocation loc) {
        return new TypeErrors(List.of(new TypeError(message, loc)));
    }

    public TypeErrors combine(TypeErrors other) {
        return new TypeErrors(
            Stream.concat(errors.stream(), other.errors.stream()).toList()
        );
    }
}
```

Now the type checker. We use Java 21+ pattern matching on `Valid`/`Invalid` to accumulate errors:

```java
import org.higherkindedj.hkt.Semigroup;
import org.higherkindedj.hkt.validated.Invalid;
import org.higherkindedj.hkt.validated.Valid;
import org.higherkindedj.hkt.validated.Validated;

public class ExprTypeChecker {

    private static final Semigroup<List<TypeError>> ERROR_SEMIGROUP = TypeError.semigroup();

    public static Validated<List<TypeError>, Type> typeCheck(Expr expr, TypeEnv env) {
        return switch (expr) {
            case Literal(var value) -> typeCheckLiteral(value);
            case Variable(var name) -> typeCheckVariable(name, env);
            case Binary(var left, var op, var right) -> typeCheckBinary(left, op, right, env);
            case Conditional(var cond, var then_, var else_) ->
                typeCheckConditional(cond, then_, else_, env);
        };
    }

    private static Validated<List<TypeError>, Type> typeCheckLiteral(Object value) {
        return switch (value) {
            case Integer _ -> Validated.valid(Type.INT);
            case Boolean _ -> Validated.valid(Type.BOOL);
            case String _ -> Validated.valid(Type.STRING);
            default -> Validated.invalid(TypeError.single("Unknown literal type"));
        };
    }

    private static Validated<List<TypeError>, Type> typeCheckBinary(
            Expr left, BinaryOp op, Expr right, TypeEnv env) {
        Validated<List<TypeError>, Type> leftResult = typeCheck(left, env);
        Validated<List<TypeError>, Type> rightResult = typeCheck(right, env);

        // Use Java 21+ pattern matching on Valid/Invalid to accumulate errors
        return switch (leftResult) {
            case Valid(var lt) -> switch (rightResult) {
                case Valid(var rt) -> checkBinaryTypes(op, lt, rt);
                case Invalid(var errors) -> Validated.invalid(errors);
            };
            case Invalid(var leftErrors) -> switch (rightResult) {
                case Valid(_) -> Validated.invalid(leftErrors);
                case Invalid(var rightErrors) ->
                    Validated.invalid(ERROR_SEMIGROUP.combine(leftErrors, rightErrors));
            };
        };
    }

    private static Validated<List<TypeError>, Type> checkBinaryTypes(
            BinaryOp op, Type left, Type right) {
        return switch (op) {
            case ADD, SUB, MUL, DIV -> (left == Type.INT && right == Type.INT)
                ? Validated.valid(Type.INT)
                : Validated.invalid(TypeError.single(
                    "Arithmetic operator '%s' requires INT operands, got %s and %s"
                        .formatted(op.symbol(), left, right)));
            case AND, OR -> (left == Type.BOOL && right == Type.BOOL)
                ? Validated.valid(Type.BOOL)
                : Validated.invalid(TypeError.single(
                    "Logical operator '%s' requires BOOL operands, got %s and %s"
                        .formatted(op.symbol(), left, right)));
            case EQ, NE -> (left == right)
                ? Validated.valid(Type.BOOL)
                : Validated.invalid(TypeError.single(
                    "Equality operator '%s' requires matching types, got %s and %s"
                        .formatted(op.symbol(), left, right)));
            case LT, LE, GT, GE -> (left == Type.INT && right == Type.INT)
                ? Validated.valid(Type.BOOL)
                : Validated.invalid(TypeError.single(
                    "Comparison operator '%s' requires INT operands, got %s and %s"
                        .formatted(op.symbol(), left, right)));
        };
    }
}
```

### Running the Type Checker

```java
Expr expr = parse("(x + true) * (y && 42)");
Validated<TypeErrors, Type> result = ExprTypeChecker.typeCheck(expr, emptyEnv);

switch (result) {
    case Valid(var type) -> System.out.println("Type: " + type);
    case Invalid(var errors) -> {
        System.out.println("Type errors:");
        errors.errors().forEach(e ->
            System.out.println("  " + e.location() + ": " + e.message())
        );
    }
}
```

Output:
```
Type errors:
  1:5: Arithmetic requires INT operands, got INT and BOOL
  1:14: Logical operators require BOOL operands, got BOOL and INT
```

Both errors are reported in a single pass. This is the power of `Validated` with `Applicative`.

---

## Interpretation with State

For interpretation, we need to thread an environment through the computation. The `State` monad captures this pattern.

### The State Type

```java
public record State<S, A>(Function<S, Pair<A, S>> runState) {

    public static <S, A> State<S, A> of(A value) {
        return new State<>(s -> new Pair<>(value, s));
    }

    public static <S> State<S, S> get() {
        return new State<>(s -> new Pair<>(s, s));
    }

    public static <S> State<S, Void> put(S newState) {
        return new State<>(s -> new Pair<>(null, newState));
    }

    public static <S> State<S, Void> modify(Function<S, S> f) {
        return new State<>(s -> new Pair<>(null, f.apply(s)));
    }

    public <B> State<S, B> flatMap(Function<A, State<S, B>> f) {
        return new State<>(s -> {
            Pair<A, S> result = runState.apply(s);
            return f.apply(result.first()).runState().apply(result.second());
        });
    }

    public <B> State<S, B> map(Function<A, B> f) {
        return flatMap(a -> of(f.apply(a)));
    }

    public A eval(S initial) {
        return runState.apply(initial).first();
    }
}
```

### Building an Interpreter

```java
public class ExprInterpreter {

    public static State<Environment, Object> interpret(Expr expr) {
        return switch (expr) {
            case Literal(var value) -> State.of(value);

            case Variable(var name) ->
                State.<Environment>get().map(env -> env.lookup(name));

            case Binary(var left, var op, var right) ->
                interpret(left).flatMap(leftVal ->
                    interpret(right).map(rightVal ->
                        applyBinaryOp(op, leftVal, rightVal)
                    )
                );

            case Conditional(var cond, var then_, var else_) ->
                interpret(cond).flatMap(condVal -> {
                    if ((Boolean) condVal) {
                        return interpret(then_);
                    } else {
                        return interpret(else_);
                    }
                });
        };
    }

    private static Object applyBinaryOp(BinaryOp op, Object left, Object right) {
        return switch (op) {
            case ADD -> (Integer) left + (Integer) right;
            case SUB -> (Integer) left - (Integer) right;
            case MUL -> (Integer) left * (Integer) right;
            case DIV -> (Integer) left / (Integer) right;
            case AND -> (Boolean) left && (Boolean) right;
            case OR -> (Boolean) left || (Boolean) right;
            case EQ -> left.equals(right);
            case NE -> !left.equals(right);
            case LT -> (Integer) left < (Integer) right;
            case LE -> (Integer) left <= (Integer) right;
            case GT -> (Integer) left > (Integer) right;
            case GE -> (Integer) left >= (Integer) right;
        };
    }
}
```

### Running the Interpreter

```java
Expr expr = parse("(x + 1) * 2");
Environment env = Environment.of("x", 10);

Object result = ExprInterpreter.interpret(expr).eval(env);
// result = 22
```

The environment is threaded implicitly through `flatMap`. We never pass it explicitly after the initial `eval` call.

---

## Combining Effects

Real applications often need multiple effects. Type checking might need both error accumulation *and* access to a type environment. Higher-Kinded-J supports effect composition through monad transformers.

### StateT: State with Another Effect

```java
// StateT<S, F, A> = S -> F<(A, S)>
// Combines State with any other effect F

public record StateT<S, F, A>(Function<S, Kind<F, Pair<A, S>>> runStateT) {

    public static <S, F, A> StateT<S, F, A> lift(Kind<F, A> fa, Monad<F> monad) {
        return new StateT<>(s -> monad.map(fa, a -> new Pair<>(a, s)));
    }

    public <B> StateT<S, F, B> flatMap(Function<A, StateT<S, F, B>> f, Monad<F> monad) {
        return new StateT<>(s ->
            monad.flatMap(runStateT.apply(s), pair ->
                f.apply(pair.first()).runStateT().apply(pair.second())
            )
        );
    }
}
```

### Example: Type Checking with Environment Access

```java
// Combines Validated (for error accumulation) with Reader (for environment access)
public static ValidatedT<TypeErrors, Reader<TypeEnv, ?>, Type>
        typeCheckWithEnv(Expr expr) {
    // Implementation uses both effects
}
```

---

## Optics and Effects Together

The real power comes when we combine our traversals with effects. Remember our `children()` traversal from Article 4:

```java
public static Traversal<Expr, Expr> children() {
    return new Traversal<>() {
        @Override
        public <F> Kind<F, Expr> modifyF(
                Function<Expr, Kind<F, Expr>> f,
                Expr source,
                Applicative<F> applicative) {
            return switch (source) {
                case Literal _ -> applicative.of(source);
                case Variable _ -> applicative.of(source);
                case Binary(var l, var op, var r) ->
                    applicative.map2(f.apply(l), f.apply(r),
                        (newL, newR) -> new Binary(newL, op, newR));
                case Conditional(var c, var t, var e) ->
                    applicative.map3(f.apply(c), f.apply(t), f.apply(e),
                        (newC, newT, newE) -> new Conditional(newC, newT, newE));
            };
        }
    };
}
```

This same traversal works with:

1. **Identity**: Pure transformations (our Article 4 optimiser)
2. **Optional**: Transformations that might fail
3. **Validated**: Transformations that accumulate errors
4. **State**: Transformations that need context
5. **IO**: Transformations with side effects

We wrote the traversal once. Higher-Kinded-J's abstraction gives us all these behaviours.

### Example: Collecting Variables with State

```java
import org.higherkindedj.hkt.Kind;
import org.higherkindedj.hkt.state.State;
import org.higherkindedj.hkt.state.StateKind;
import org.higherkindedj.hkt.state.StateKindHelper;
import org.higherkindedj.hkt.state.StateMonad;
import org.higherkindedj.hkt.state.StateTuple;

// Define a collector function that records variable names in State
Function<Expr, Kind<StateKind.Witness<Set<String>>, Expr>> collectVars = expr -> {
    if (expr instanceof Variable(var name)) {
        State<Set<String>, Expr> addVar = State.<Set<String>>modify(vars -> {
            var newVars = new HashSet<>(vars);
            newVars.add(name);
            return newVars;
        }).map(v -> expr);
        return StateKindHelper.STATE.widen(addVar);
    }
    return StateKindHelper.STATE.widen(State.pure(expr));
};

// Apply to all nodes using our traversal
StateMonad<Set<String>> stateMonad = new StateMonad<>();
Kind<StateKind.Witness<Set<String>>, Expr> result =
    ExprTraversal.children().modifyF(collectVars, expression, stateMonad);

// Run the stateful computation starting with an empty set
StateTuple<Set<String>, Expr> tuple =
    StateKindHelper.STATE.<Set<String>, Expr>narrow(result).run(new HashSet<>());
Set<String> variables = tuple.state();
```

---

## Summary

This article explored effect-polymorphic optics, where the same structural code works with different computational effects:

1. **Effect polymorphism**: Abstract over computational context with `Kind<F, A>`
2. **Applicative and Monad**: The type classes that enable effect composition
3. **Validated**: Accumulate all errors instead of failing fast
4. **State**: Thread context through computations implicitly
5. **modifyF**: The bridge between optics and effects

### Higher-Kinded-J: Unlocking Java's Functional Potential

This article demonstrates something remarkable: Java can express the same effect-polymorphic patterns found in Haskell and Scala. Higher-Kinded-J makes this possible through a carefully designed encoding of higher-kinded types using witness types and defunctionalisation.

What sets Higher-Kinded-J apart is not just that it provides these abstractions, but *how* it provides them:

1. **Genuine abstraction, not simulation**: The `Kind<F, A>` encoding isn't a workaround; it's a principled approach that preserves the full power of higher-kinded polymorphism. When you write `modifyF`, you're writing truly generic code that works with any effect, not code that pattern-matches on a fixed set of cases.

2. **Lawful type classes**: The `Applicative` and `Monad` instances in Higher-Kinded-J satisfy their mathematical laws. This means your intuitions transfer directly from functional programming literature. `map2` on `Validated` accumulates errors because that's what the Applicative laws require for a type that isn't a Monad.

3. **Composition scales**: We've now seen optics compose with optics (Article 2), traversals compose with transformations (Article 4), and effects compose with optics (this article). Each composition multiplies capability without multiplying complexity. This compositional scaling is Higher-Kinded-J's central achievement.

4. **Java remains Java**: Despite these powerful abstractions, the code remains idiomatic Java. Records, sealed interfaces, pattern matching, and switch expressions all work naturally with Higher-Kinded-J's types. You're not fighting the language; you're extending its reach.

The expression language we've built across these articles now has type checking that reports all errors, interpretation that threads state cleanly, and optimisation that composes declaratively. All of this runs on the same traversal infrastructure, demonstrating that effect polymorphism isn't academic abstraction; it's practical engineering.

---

## What's Next

We've built a substantial expression language: AST definition, optics generation, tree traversals, optimisation passes, type checking, and interpretation. The foundation is solid, but there's more to explore.

In Article 6, we'll tackle parsing and complete the picture:

- **Parser combinators**: Build a parser using Higher-Kinded-J's applicative style
- **Error recovery**: Parse malformed input while collecting syntax errors
- **Source locations**: Thread position information through the entire pipeline
- **End-to-end demonstration**: From source text to evaluated result

Higher-Kinded-J's applicative functors prove invaluable for parsing. Parser combinators are inherently applicative: they combine independent parsers for different parts of the input. We'll see how the same `map2` and `map3` operations we used for type checking apply directly to parsing, creating a unified approach to combining computations.

We'll also explore how Higher-Kinded-J's `Alternative` type class enables choice and repetition in parsers, completing the toolkit for building practical expression parsers without external dependencies.

---

*Next: [Article 6: Parser Combinators and the Complete Pipeline](article-6-parser-combinators.md)*
