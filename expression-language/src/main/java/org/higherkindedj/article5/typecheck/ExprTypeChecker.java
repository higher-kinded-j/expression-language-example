// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article5.typecheck;

import org.higherkindedj.article5.effect.Validated;
import org.higherkindedj.article5.effect.ValidatedKind;
import org.higherkindedj.article4.ast.BinaryOp;
import org.higherkindedj.article4.ast.Expr;
import org.higherkindedj.article4.ast.Expr.Binary;
import org.higherkindedj.article4.ast.Expr.Conditional;
import org.higherkindedj.article4.ast.Expr.Literal;
import org.higherkindedj.article4.ast.Expr.Variable;
import org.higherkindedj.hkt.Applicative;

/**
 * Type checker for the expression language using error accumulation.
 *
 * <p>This type checker uses {@link Validated} to accumulate ALL type errors in a single pass,
 * rather than failing on the first error. This provides a better user experience: users see all
 * errors at once instead of fixing them one at a time.
 *
 * <p>The key insight: type checking sub-expressions is INDEPENDENT. The type of the left operand
 * doesn't affect whether we should check the right operand. This makes {@code Validated} (which is
 * Applicative but not Monad) perfect for the job.
 */
public final class ExprTypeChecker {

  private static final Applicative<ValidatedKind.Mu<TypeErrors>> APPLICATIVE =
      ValidatedKind.applicative(TypeErrors::combine);

  private ExprTypeChecker() {}

  /**
   * Type check an expression in the given type environment.
   *
   * @param expr the expression to type check
   * @param env the type environment
   * @return Valid(type) if well-typed, Invalid(errors) if there are type errors
   */
  public static Validated<TypeErrors, Type> typeCheck(Expr expr, TypeEnv env) {
    return switch (expr) {
      case Literal(var value) -> typeCheckLiteral(value);
      case Variable(var name) -> typeCheckVariable(name, env);
      case Binary(var left, var op, var right) -> typeCheckBinary(left, op, right, env);
      case Conditional(var cond, var then_, var else_) ->
          typeCheckConditional(cond, then_, else_, env);
    };
  }

  private static Validated<TypeErrors, Type> typeCheckLiteral(Object value) {
    if (value instanceof Integer) {
      return Validated.valid(Type.INT);
    } else if (value instanceof Boolean) {
      return Validated.valid(Type.BOOL);
    } else if (value instanceof String) {
      return Validated.valid(Type.STRING);
    } else {
      return Validated.invalid(TypeErrors.single("Unknown literal type: " + value.getClass()));
    }
  }

  private static Validated<TypeErrors, Type> typeCheckVariable(String name, TypeEnv env) {
    return env.lookup(name)
        .map(Validated::<TypeErrors, Type>valid)
        .orElseGet(() -> Validated.invalid(TypeErrors.single("Undefined variable: " + name)));
  }

  private static Validated<TypeErrors, Type> typeCheckBinary(
      Expr left, BinaryOp op, Expr right, TypeEnv env) {
    Validated<TypeErrors, Type> leftType = typeCheck(left, env);
    Validated<TypeErrors, Type> rightType = typeCheck(right, env);

    // Use Applicative.map2 to combine both validations
    // This accumulates errors from BOTH sides before checking the operation
    var combined =
        APPLICATIVE.map2(
            ValidatedKind.widen(leftType),
            ValidatedKind.widen(rightType),
            (lt, rt) -> checkBinaryTypes(op, lt, rt));

    // The result is Validated<TypeErrors, Validated<TypeErrors, Type>>
    // We need to flatten it
    return flattenValidated(ValidatedKind.narrow(combined));
  }

  private static Validated<TypeErrors, Type> typeCheckConditional(
      Expr cond, Expr then_, Expr else_, TypeEnv env) {
    Validated<TypeErrors, Type> condType = typeCheck(cond, env);
    Validated<TypeErrors, Type> thenType = typeCheck(then_, env);
    Validated<TypeErrors, Type> elseType = typeCheck(else_, env);

    // Use Applicative.map3 to combine all three validations
    var combined =
        APPLICATIVE.map3(
            ValidatedKind.widen(condType),
            ValidatedKind.widen(thenType),
            ValidatedKind.widen(elseType),
            ExprTypeChecker::checkConditionalTypes);

    return flattenValidated(ValidatedKind.narrow(combined));
  }

  private static Validated<TypeErrors, Type> checkBinaryTypes(BinaryOp op, Type left, Type right) {
    return switch (op) {
      case ADD, SUB, MUL, DIV -> {
        if (left != Type.INT || right != Type.INT) {
          yield Validated.invalid(
              TypeErrors.single(
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
              TypeErrors.single(
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
              TypeErrors.single(
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
              TypeErrors.single(
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

  private static Validated<TypeErrors, Type> checkConditionalTypes(
      Type cond, Type then_, Type else_) {
    TypeErrors errors = new TypeErrors(java.util.List.of());

    if (cond != Type.BOOL) {
      errors =
          errors.combine(
              TypeErrors.single("Conditional requires BOOL condition, got " + cond));
    }

    if (then_ != else_) {
      errors =
          errors.combine(
              TypeErrors.single(
                  "Conditional branches must have same type, got " + then_ + " and " + else_));
    }

    if (errors.hasErrors()) {
      return Validated.invalid(errors);
    }
    return Validated.valid(then_);
  }

  private static <E, A> Validated<E, A> flattenValidated(Validated<E, Validated<E, A>> nested) {
    return switch (nested) {
      case Validated.Valid(var inner) -> inner;
      case Validated.Invalid(var errors) -> Validated.invalid(errors);
    };
  }
}
