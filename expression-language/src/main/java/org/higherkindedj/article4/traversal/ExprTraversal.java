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

/**
 * Traversals for the expression AST.
 *
 * <p>Provides utilities for visiting and transforming all nodes in an expression tree.
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
   *   <li>{@link Conditional} has three children: cond, then_, else_
   * </ul>
   *
   * <p>Use {@link #transformBottomUp} or {@link #transformTopDown} for full tree traversal.
   *
   * @return a traversal over immediate child expressions
   */
  public static Traversal<Expr, Expr> children() {
    return new Traversal<>() {
      @Override
      public Expr modify(Function<Expr, Expr> f, Expr source) {
        return switch (source) {
          case Literal _ -> source;
          case Variable _ -> source;
          case Binary(var l, var op, var r) -> new Binary(f.apply(l), op, f.apply(r));
          case Conditional(var c, var t, var e) ->
              new Conditional(f.apply(c), f.apply(t), f.apply(e));
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
   * <p>This traversal visits every node in the expression tree.
   *
   * @return a traversal over all expressions in the tree
   */
  public static Traversal<Expr, Expr> allDescendants() {
    return new Traversal<>() {
      @Override
      public Expr modify(Function<Expr, Expr> f, Expr source) {
        return transformBottomUp(source, f);
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
   * <p>Each node is transformed after its children have been transformed. This is the correct order
   * for optimisations like constant folding, where we need to evaluate children before we can
   * simplify the parent.
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
   * <p>Each node is transformed before its children are visited. This is useful when you want to
   * pattern-match on the original structure before children change (like macro expansion).
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
}
