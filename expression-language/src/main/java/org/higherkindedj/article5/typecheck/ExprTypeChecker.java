// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article5.typecheck;

import java.util.List;
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
    if (value instanceof Integer) {
      return Validated.valid(Type.INT);
    } else if (value instanceof Boolean) {
      return Validated.valid(Type.BOOL);
    } else if (value instanceof String) {
      return Validated.valid(Type.STRING);
    } else {
      return Validated.invalid(TypeError.single("Unknown literal type: " + value.getClass()));
    }
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

    // Use Higher-Kinded-J's Validated.ap to combine both validations with error accumulation
    // ap applies a function wrapped in Validated to a value wrapped in Validated
    // The semigroup combines errors from both sides
    Validated<List<TypeError>, java.util.function.Function<Type, Type>> partialCheck =
        leftType.map(lt -> rt -> checkBinaryTypesResult(op, lt, rt));

    Validated<List<TypeError>, Type> combinedTypes = rightType.ap(partialCheck, ERROR_SEMIGROUP);

    // Now check if the combined result itself has errors from the type check
    return combinedTypes.flatMap(type -> {
      if (type == null) {
        // checkBinaryTypesResult returned null to signal an error was found
        // We need to re-check to get the actual error
        return leftType.flatMap(lt ->
            rightType.flatMap(rt -> checkBinaryTypes(op, lt, rt)));
      }
      return Validated.valid(type);
    });
  }

  // Returns null if types don't match (used for ap pattern), actual type otherwise
  private static Type checkBinaryTypesResult(BinaryOp op, Type left, Type right) {
    return switch (op) {
      case ADD, SUB, MUL, DIV -> (left == Type.INT && right == Type.INT) ? Type.INT : null;
      case AND, OR -> (left == Type.BOOL && right == Type.BOOL) ? Type.BOOL : null;
      case EQ, NE -> (left == right) ? Type.BOOL : null;
      case LT, LE, GT, GE -> (left == Type.INT && right == Type.INT) ? Type.BOOL : null;
    };
  }

  private static Validated<List<TypeError>, Type> typeCheckConditional(
      Expr cond, Expr then_, Expr else_, TypeEnv env) {
    Validated<List<TypeError>, Type> condType = typeCheck(cond, env);
    Validated<List<TypeError>, Type> thenType = typeCheck(then_, env);
    Validated<List<TypeError>, Type> elseType = typeCheck(else_, env);

    // Check condition first, then check branches
    // Use flatMap for sequential checks after accumulating sub-expression errors
    return condType.flatMap(ct ->
        thenType.flatMap(tt ->
            elseType.flatMap(et ->
                checkConditionalTypes(ct, tt, et))));
  }

  private static Validated<List<TypeError>, Type> checkBinaryTypes(
      BinaryOp op, Type left, Type right) {
    return switch (op) {
      case ADD, SUB, MUL, DIV -> {
        if (left != Type.INT || right != Type.INT) {
          yield Validated.invalid(
              TypeError.single(
                  "Arithmetic operator "
                      + op.symbol()
                      + " requires INT operands, got "
                      + left
                      + " and "
                      + right));
        }
        yield Validated.valid(Type.INT);
      }
      case AND, OR -> {
        if (left != Type.BOOL || right != Type.BOOL) {
          yield Validated.invalid(
              TypeError.single(
                  "Logical operator "
                      + op.symbol()
                      + " requires BOOL operands, got "
                      + left
                      + " and "
                      + right));
        }
        yield Validated.valid(Type.BOOL);
      }
      case EQ, NE -> {
        if (left != right) {
          yield Validated.invalid(
              TypeError.single(
                  "Equality operator "
                      + op.symbol()
                      + " requires matching types, got "
                      + left
                      + " and "
                      + right));
        }
        yield Validated.valid(Type.BOOL);
      }
      case LT, LE, GT, GE -> {
        if (left != Type.INT || right != Type.INT) {
          yield Validated.invalid(
              TypeError.single(
                  "Comparison operator "
                      + op.symbol()
                      + " requires INT operands, got "
                      + left
                      + " and "
                      + right));
        }
        yield Validated.valid(Type.BOOL);
      }
    };
  }

  private static Validated<List<TypeError>, Type> checkConditionalTypes(
      Type cond, Type then_, Type else_) {
    List<TypeError> errors = List.of();

    if (cond != Type.BOOL) {
      errors =
          ERROR_SEMIGROUP.combine(
              errors, TypeError.single("Conditional requires BOOL condition, got " + cond));
    }

    if (then_ != else_) {
      errors =
          ERROR_SEMIGROUP.combine(
              errors,
              TypeError.single(
                  "Conditional branches must have same type, got " + then_ + " and " + else_));
    }

    if (!errors.isEmpty()) {
      return Validated.invalid(errors);
    }
    return Validated.valid(then_);
  }
}
