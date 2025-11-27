// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article4.traversal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.higherkindedj.article4.ast.Expr;
import org.higherkindedj.article4.ast.Expr.Binary;
import org.higherkindedj.article4.ast.Expr.Conditional;
import org.higherkindedj.article4.ast.Expr.Literal;
import org.higherkindedj.article4.ast.Expr.Variable;
import org.higherkindedj.hkt.Kind;
import org.higherkindedj.optics.Traversal;
import org.higherkindedj.hkt.Applicative;

/**
 * Traversals for the expression AST using higher-kinded-j.
 *
 * <p>This demonstrates the power of effect-polymorphic optics. The same traversal can be used with
 * different computational contexts:
 *
 * <ul>
 *   <li>Pure transformations with Identity
 *   <li>Fallible transformations with Optional or Either
 *   <li>Error-accumulating transformations with Validated
 *   <li>Stateful transformations with State
 * </ul>
 *
 * <p>The key method is {@code modifyF}, which lifts a transformation into any Applicative functor.
 */
public final class ExprTraversal {

  private ExprTraversal() {}

  /**
   * A traversal targeting all immediate children of an expression.
   *
   * <p>This traversal does not descend recursively—it only visits direct children:
   *
   * <ul>
   *   <li>{@link Literal} and {@link Variable} have no children
   *   <li>{@link Binary} has two children: left and right
   *   <li>{@link Conditional} has three children: cond, thenBranch, elseBranch
   * </ul>
   *
   * <p>The traversal is effect-polymorphic: use {@code modifyF} with any {@code Applicative} to
   * perform effectful transformations over all children.
   *
   * @return a traversal over immediate child expressions
   */
  public static Traversal<Expr, Expr> children() {
    return new Traversal<>() {
      @Override
      public <F> Kind<F, Expr> modifyF(
          Applicative<F> F, Function<Expr, Kind<F, Expr>> f, Expr source) {
        return switch (source) {
          case Literal _ -> F.pure(source);
          case Variable _ -> F.pure(source);
          case Binary(var l, var op, var r) ->
              F.map2(f.apply(l), f.apply(r), (newL, newR) -> new Binary(newL, op, newR));
          case Conditional(var c, var t, var e) ->
              F.map3(
                  f.apply(c),
                  f.apply(t),
                  f.apply(e),
                  (newC, newT, newE) -> new Conditional(newC, newT, newE));
        };
      }

      @Override
      public List<Expr> getAll(Expr source) {
        return switch (source) {
          case Literal _, Variable _ -> List.of();
          case Binary(var l, _, var r) -> List.of(l, r);
          case Conditional(var c, var t, var e) -> List.of(c, t, e);
        };
      }
    };
  }

  /**
   * A traversal targeting all descendant expressions (including the root).
   *
   * <p>This traversal visits every node in the expression tree using bottom-up order.
   *
   * @return a traversal over all expressions in the tree
   */
  public static Traversal<Expr, Expr> allDescendants() {
    return new Traversal<>() {
      @Override
      public <F> Kind<F, Expr> modifyF(
          Applicative<F> F, Function<Expr, Kind<F, Expr>> f, Expr source) {
        // Bottom-up: first transform children, then this node
        Kind<F, Expr> withChildrenTransformed =
            children().modifyF(F, child -> this.modifyF(F, f, child), source);
        return F.flatMap(withChildrenTransformed, f);
      }

      @Override
      public List<Expr> getAll(Expr source) {
        List<Expr> results = new ArrayList<>();
        collectAll(source, results);
        return results;
      }

      private void collectAll(Expr expr, List<Expr> accumulator) {
        accumulator.add(expr);
        for (Expr child : children().getAll(expr)) {
          collectAll(child, accumulator);
        }
      }
    };
  }

  /**
   * Transform all nodes in the tree from leaves to root (bottom-up).
   *
   * <p>This is a convenience method that uses the Identity applicative for pure transformations.
   * Each node is transformed after its children have been transformed.
   *
   * @param expr the expression to transform
   * @param f the transformation function
   * @return the transformed expression
   */
  public static Expr transformBottomUp(Expr expr, Function<Expr, Expr> f) {
    // First transform all children
    Expr transformed = children().modify(child -> transformBottomUp(child, f), expr);
    // Then transform this node
    return f.apply(transformed);
  }

  /**
   * Transform all nodes in the tree from root to leaves (top-down).
   *
   * <p>Each node is transformed before its children are visited.
   *
   * @param expr the expression to transform
   * @param f the transformation function
   * @return the transformed expression
   */
  public static Expr transformTopDown(Expr expr, Function<Expr, Expr> f) {
    // First transform this node
    Expr transformed = f.apply(expr);
    // Then transform all children
    return children().modify(child -> transformTopDown(child, f), transformed);
  }

  /**
   * Effect-polymorphic bottom-up transformation.
   *
   * <p>This is the key method that showcases higher-kinded-j's power. The same traversal logic
   * works with any Applicative effect:
   *
   * <pre>{@code
   * // Pure transformation
   * Expr result = transformBottomUpF(Identity.applicative(), expr, e -> Identity.of(transform(e)));
   *
   * // Fallible transformation (stops on first failure)
   * Either<Error, Expr> result = transformBottomUpF(Either.applicative(), expr, this::validate);
   *
   * // Error-accumulating transformation (collects all errors)
   * Validated<List<Error>, Expr> result = transformBottomUpF(Validated.applicative(), expr, this::validate);
   * }</pre>
   *
   * @param <F> the effect type (Identity, Optional, Either, Validated, etc.)
   * @param F the Applicative instance for F
   * @param expr the expression to transform
   * @param f the effectful transformation function
   * @return the transformed expression wrapped in the effect
   */
  public static <F> Kind<F, Expr> transformBottomUpF(
      Applicative<F> F, Expr expr, Function<Expr, Kind<F, Expr>> f) {
    // First transform all children
    Kind<F, Expr> withChildrenTransformed =
        children().modifyF(F, child -> transformBottomUpF(F, child, f), expr);
    // Then transform this node
    return F.flatMap(withChildrenTransformed, f);
  }

  /**
   * Effect-polymorphic top-down transformation.
   *
   * @param <F> the effect type
   * @param F the Applicative instance for F
   * @param expr the expression to transform
   * @param f the effectful transformation function
   * @return the transformed expression wrapped in the effect
   */
  public static <F> Kind<F, Expr> transformTopDownF(
      Applicative<F> F, Expr expr, Function<Expr, Kind<F, Expr>> f) {
    // First transform this node
    Kind<F, Expr> transformed = f.apply(expr);
    // Then transform all children
    return F.flatMap(
        transformed, t -> children().modifyF(F, child -> transformTopDownF(F, child, f), t));
  }
}
