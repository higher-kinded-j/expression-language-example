// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article3.transform;

import org.higherkindedj.article3.ast.BinaryOp;
import org.higherkindedj.article3.ast.Expr;
import org.higherkindedj.article3.ast.Expr.Binary;
import org.higherkindedj.article3.ast.Expr.Conditional;
import org.higherkindedj.article3.ast.Expr.Literal;

/**
 * Expression optimiser implementing constant folding and identity simplification.
 *
 * <p>This demonstrates the data-oriented programming approach to AST optimisation. Rather than
 * using the Visitor pattern, we use Java 25's enhanced pattern matching with guards to express
 * optimisation rules declaratively.
 *
 * <p>Key Java 25 features demonstrated:
 * <ul>
 *   <li>Switch expressions with record patterns
 *   <li>Pattern guards with {@code when} clauses
 *   <li>Nested record patterns for deep matching
 *   <li>Unnamed patterns ({@code _}) for ignored components
 * </ul>
 */
public final class ExprOptimiser {
  private ExprOptimiser() {}

  /**
   * Optimise an expression by repeatedly applying all optimisations until a fixed point.
   *
   * @param expr the expression to optimise
   * @return the optimised expression
   */
  public static Expr optimise(Expr expr) {
    Expr result = expr;
    Expr previous;

    // Run until fixed point (no more changes)
    do {
      previous = result;
      result = ExprTransform.transformBottomUp(result, ExprOptimiser::optimiseNode);
    } while (!result.equals(previous));

    return result;
  }

  /** Apply all optimisations to a single node. */
  private static Expr optimiseNode(Expr expr) {
    Expr result = expr;
    result = foldConstants(result);
    result = simplifyIdentities(result);
    result = simplifyConditionals(result);
    return result;
  }

  /**
   * Constant folding: evaluate constant expressions at compile time.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>{@code 1 + 2} → {@code 3}
   *   <li>{@code true && false} → {@code false}
   * </ul>
   *
   * <p>Uses Java 25 switch expression with nested record patterns.
   */
  public static Expr foldConstants(Expr expr) {
    // Java 25: Switch with nested record patterns
    return switch (expr) {
      case Binary(Literal(Object lv), BinaryOp op, Literal(Object rv)) -> {
        Object result = evaluate(lv, op, rv);
        yield result != null ? new Literal(result) : expr;
      }
      default -> expr;
    };
  }

  /**
   * Simplify identity operations.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>{@code x + 0} → {@code x}
   *   <li>{@code x * 1} → {@code x}
   *   <li>{@code x * 0} → {@code 0}
   *   <li>{@code x && true} → {@code x}
   *   <li>{@code x || false} → {@code x}
   * </ul>
   *
   * <p>Uses Java 25 pattern matching with guards for declarative rule expression.
   */
  public static Expr simplifyIdentities(Expr expr) {
    // Java 25: Switch expression with 'when' guards for clean rule expression
    return switch (expr) {
      // Arithmetic identities with right literal
      case Binary(var left, BinaryOp.ADD, Literal(Integer v)) when v == 0 -> left;
      case Binary(var left, BinaryOp.SUB, Literal(Integer v)) when v == 0 -> left;
      case Binary(var left, BinaryOp.MUL, Literal(Integer v)) when v == 1 -> left;
      case Binary(var left, BinaryOp.DIV, Literal(Integer v)) when v == 1 -> left;
      case Binary(_, BinaryOp.MUL, Literal(Integer v)) when v == 0 -> new Literal(0);

      // Arithmetic identities with left literal
      case Binary(Literal(Integer v), BinaryOp.ADD, var right) when v == 0 -> right;
      case Binary(Literal(Integer v), BinaryOp.MUL, var right) when v == 1 -> right;
      case Binary(Literal(Integer v), BinaryOp.MUL, _) when v == 0 -> new Literal(0);

      // Boolean identities with right literal
      case Binary(var left, BinaryOp.AND, Literal(Boolean v)) when v -> left;
      case Binary(var left, BinaryOp.OR, Literal(Boolean v)) when !v -> left;
      case Binary(_, BinaryOp.AND, Literal(Boolean v)) when !v -> new Literal(false);
      case Binary(_, BinaryOp.OR, Literal(Boolean v)) when v -> new Literal(true);

      // Boolean identities with left literal
      case Binary(Literal(Boolean v), BinaryOp.AND, var right) when v -> right;
      case Binary(Literal(Boolean v), BinaryOp.OR, var right) when !v -> right;
      case Binary(Literal(Boolean v), BinaryOp.AND, _) when !v -> new Literal(false);
      case Binary(Literal(Boolean v), BinaryOp.OR, _) when v -> new Literal(true);

      default -> expr;
    };
  }

  /**
   * Simplify conditional expressions with constant conditions.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>{@code if true then a else b} → {@code a}
   *   <li>{@code if false then a else b} → {@code b}
   * </ul>
   *
   * <p>Uses Java 25 pattern matching with guards for declarative rule expression.
   */
  public static Expr simplifyConditionals(Expr expr) {
    // Java 25: Switch with pattern guards for boolean conditions
    return switch (expr) {
      case Conditional(Literal(Boolean cv), var then_, _) when cv -> then_;
      case Conditional(Literal(Boolean cv), _, var else_) when !cv -> else_;
      default -> expr;
    };
  }

  /**
   * Evaluate a binary operation on literal values.
   *
   * <p>Uses Java 25's type pattern matching with 'when' guards for concise type dispatch.
   */
  private static Object evaluate(Object left, BinaryOp op, Object right) {
    // Java 25: Switch on the left operand with type patterns and guards
    return switch (left) {
      case Integer l when right instanceof Integer r -> switch (op) {
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
      case Boolean l when right instanceof Boolean r -> switch (op) {
        case AND -> l && r;
        case OR -> l || r;
        case EQ -> l.equals(r);
        case NE -> !l.equals(r);
        default -> null;
      };
      case String l when right instanceof String r && op == BinaryOp.ADD -> l + r;
      default -> switch (op) {
        case EQ -> left.equals(right);
        case NE -> !left.equals(right);
        default -> null;
      };
    };
  }
}
