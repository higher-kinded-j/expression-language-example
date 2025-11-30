// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article5.typecheck;

import java.util.List;
import java.util.function.Function;
import org.higherkindedj.article4.ast.BinaryOp;
import org.higherkindedj.article4.ast.Expr;
import org.higherkindedj.article4.ast.Expr.Binary;
import org.higherkindedj.article4.ast.Expr.Conditional;
import org.higherkindedj.article4.ast.Expr.Literal;
import org.higherkindedj.article4.ast.Expr.Variable;
import org.higherkindedj.hkt.Semigroup;
import org.higherkindedj.hkt.validated.Validated;

/**
 * Type checker for the expression language using Higher-Kinded-J's Validated for error
 * accumulation.
 *
 * <p>This type checker uses {@link Validated} from Higher-Kinded-J to accumulate ALL type errors in
 * a single pass, rather than failing on the first error. This provides a better user experience:
 * users see all errors at once instead of fixing them one at a time.
 *
 * <p>Key insight: type checking sub-expressions is INDEPENDENT. The type of the left operand
 * doesn't affect whether we should check the right operand. This makes {@code Validated} (which is
 * Applicative) perfect for the job. We use Validated's {@code ap} method with a Semigroup for error
 * accumulation.
 */
public final class ExprTypeChecker {

  /** Semigroup for accumulating type errors. */
  private static final Semigroup<List<TypeError>> ERROR_SEMIGROUP = TypeError.semigroup();

  private ExprTypeChecker() {}

  /** A pair of types, used for applicative combination of sub-expression types. */
  private record TypePair(Type left, Type right) {}

  /**
   * Type check an expression in the given type environment.
   *
   * <p>Uses Higher-Kinded-J's Validated to accumulate all type errors in a single pass.
   *
   * @param expr the expression to type check
   * @param env the type environment
   * @return Valid(type) if well-typed, Invalid(errors) if there are type errors
   */
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
      default -> Validated.invalid(
          TypeError.single("Unknown literal type: %s".formatted(value.getClass().getSimpleName())));
    };
  }

  private static Validated<List<TypeError>, Type> typeCheckVariable(String name, TypeEnv env) {
    return env.lookup(name)
        .map(Validated::<List<TypeError>, Type>valid)
        .orElseGet(() -> Validated.invalid(TypeError.single("Undefined variable: " + name)));
  }

  private static Validated<List<TypeError>, Type> typeCheckBinary(
      Expr left, BinaryOp op, Expr right, TypeEnv env) {
    Validated<List<TypeError>, Type> leftType = typeCheck(left, env);
    Validated<List<TypeError>, Type> rightType = typeCheck(right, env);

    // Use Higher-Kinded-J's Validated.ap to combine both validations with error accumulation.
    // Strategy:
    // 1. Use ap to combine leftType and rightType into a TypePair (accumulates sub-expression errors)
    // 2. Then flatMap to check if the types are compatible for this operator
    //
    // This ensures we see ALL errors: both sub-expression errors AND type compatibility errors.
    Validated<List<TypeError>, Function<? super Type, ? extends TypePair>> makePair =
        leftType.map(lt -> (Function<? super Type, ? extends TypePair>) (rt -> new TypePair(lt, rt)));

    Validated<List<TypeError>, TypePair> combined = rightType.ap(makePair, ERROR_SEMIGROUP);

    // Now check type compatibility - this may produce additional errors
    return combined.flatMap(pair -> checkBinaryTypes(op, pair.left(), pair.right()));
  }

  private static Validated<List<TypeError>, Type> typeCheckConditional(
      Expr cond, Expr then_, Expr else_, TypeEnv env) {
    Validated<List<TypeError>, Type> condType = typeCheck(cond, env);
    Validated<List<TypeError>, Type> thenType = typeCheck(then_, env);
    Validated<List<TypeError>, Type> elseType = typeCheck(else_, env);

    // Use ap to combine all three validations with error accumulation
    // First combine cond and then into a pair
    Validated<List<TypeError>, Function<? super Type, ? extends TypePair>> makeCondThenPair =
        condType.map(ct -> (Function<? super Type, ? extends TypePair>) (tt -> new TypePair(ct, tt)));
    Validated<List<TypeError>, TypePair> condThenPair = thenType.ap(makeCondThenPair, ERROR_SEMIGROUP);

    // Then combine with else type into a triple (represented as nested record)
    record TypeTriple(Type cond, Type then_, Type else_) {}

    Validated<List<TypeError>, Function<? super Type, ? extends TypeTriple>> makeTriple =
        condThenPair.map(
            pair -> (Function<? super Type, ? extends TypeTriple>) (et -> new TypeTriple(pair.left(), pair.right(), et)));
    Validated<List<TypeError>, TypeTriple> allTypes = elseType.ap(makeTriple, ERROR_SEMIGROUP);

    // Now check all the conditional constraints
    return allTypes.flatMap(triple -> checkConditionalTypes(triple.cond(), triple.then_(), triple.else_()));
  }

  private static Validated<List<TypeError>, Type> checkBinaryTypes(
      BinaryOp op, Type left, Type right) {
    return switch (op) {
      case ADD, SUB, MUL, DIV -> {
        if (left != Type.INT || right != Type.INT) {
          yield Validated.invalid(TypeError.single(
              "Arithmetic operator '%s' requires INT operands, got %s and %s"
                  .formatted(op.symbol(), left, right)));
        }
        yield Validated.valid(Type.INT);
      }
      case AND, OR -> {
        if (left != Type.BOOL || right != Type.BOOL) {
          yield Validated.invalid(TypeError.single(
              "Logical operator '%s' requires BOOL operands, got %s and %s"
                  .formatted(op.symbol(), left, right)));
        }
        yield Validated.valid(Type.BOOL);
      }
      case EQ, NE -> {
        if (left != right) {
          yield Validated.invalid(TypeError.single(
              "Equality operator '%s' requires matching types, got %s and %s"
                  .formatted(op.symbol(), left, right)));
        }
        yield Validated.valid(Type.BOOL);
      }
      case LT, LE, GT, GE -> {
        if (left != Type.INT || right != Type.INT) {
          yield Validated.invalid(TypeError.single(
              "Comparison operator '%s' requires INT operands, got %s and %s"
                  .formatted(op.symbol(), left, right)));
        }
        yield Validated.valid(Type.BOOL);
      }
    };
  }

  private static Validated<List<TypeError>, Type> checkConditionalTypes(
      Type cond, Type then_, Type else_) {
    List<TypeError> errors = List.of();

    if (cond != Type.BOOL) {
      errors = ERROR_SEMIGROUP.combine(
          errors,
          TypeError.single("Conditional requires BOOL condition, got %s".formatted(cond)));
    }

    if (then_ != else_) {
      errors = ERROR_SEMIGROUP.combine(
          errors,
          TypeError.single(
              "Conditional branches must have same type, got %s and %s".formatted(then_, else_)));
    }

    if (!errors.isEmpty()) {
      return Validated.invalid(errors);
    }
    return Validated.valid(then_);
  }
}
