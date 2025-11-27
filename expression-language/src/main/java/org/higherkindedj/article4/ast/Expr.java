// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article4.ast;

/**
 * The expression AST for the expression language.
 *
 * <p>This is a minimal 4-variant AST that covers:
 *
 * <ul>
 *   <li>{@link Literal} — constant values (integers, booleans, strings)
 *   <li>{@link Variable} — variable references
 *   <li>{@link Binary} — binary operations (arithmetic, comparison, logical)
 *   <li>{@link Conditional} — if-then-else expressions
 * </ul>
 *
 * <p>In production with higher-kinded-j, you would annotate this with {@code @GeneratePrisms} and
 * each record with {@code @GenerateLenses}.
 */
public sealed interface Expr {

  /** A literal value (integer, boolean, or string). */
  record Literal(Object value) implements Expr {}

  /** A variable reference. */
  record Variable(String name) implements Expr {}

  /** A binary operation. */
  record Binary(Expr left, BinaryOp op, Expr right) implements Expr {}

  /** A conditional (if-then-else) expression. */
  record Conditional(Expr cond, Expr then_, Expr else_) implements Expr {}

  // ========== Formatting ==========

  /** Format this expression as a string. */
  default String format() {
    return switch (this) {
      case Literal(var v) -> formatLiteral(v);
      case Variable(var n) -> n;
      case Binary(var l, var op, var r) ->
          "(" + l.format() + " " + op.symbol() + " " + r.format() + ")";
      case Conditional(var c, var t, var e) ->
          "(if " + c.format() + " then " + t.format() + " else " + e.format() + ")";
    };
  }

  private static String formatLiteral(Object value) {
    if (value instanceof String s) {
      return "\"" + s + "\"";
    }
    return String.valueOf(value);
  }
}
