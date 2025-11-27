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
 * Traversal utilities for the expression AST.
 *
 * <p>This class provides methods for traversing and transforming expression trees:
 *
 * <ul>
 *   <li>Getting immediate children of an expression
 *   <li>Bottom-up and top-down tree transformations
 *   <li>Collecting all nodes in a tree
 * </ul>
 *
 * <p>These utilities demonstrate functional tree manipulation patterns that work well with
 * immutable ASTs built using Java records and sealed interfaces.
 */
public final class ExprTraversal {

  private ExprTraversal() {}

  /**
   * Get all immediate children of an expression.
   *
   * <p>This method does not descend recursively—it only returns direct children:
   *
   * <ul>
   *   <li>{@link Literal} and {@link Variable} have no children
   *   <li>{@link Binary} has two children: left and right
   *   <li>{@link Conditional} has three children: cond, thenBranch, elseBranch
   * </ul>
   *
   * @param expr the expression to get children from
   * @return a list of immediate child expressions
   */
  public static List<Expr> getChildren(Expr expr) {
    return switch (expr) {
      case Literal _, Variable _ -> List.of();
      case Binary(var l, _, var r) -> List.of(l, r);
      case Conditional(var c, var t, var e) -> List.of(c, t, e);
    };
  }

  /**
   * Modify all immediate children of an expression.
   *
   * @param expr the expression to modify
   * @param f the transformation function to apply to each child
   * @return a new expression with transformed children
   */
  public static Expr modifyChildren(Expr expr, Function<Expr, Expr> f) {
    return switch (expr) {
      case Literal _ -> expr;
      case Variable _ -> expr;
      case Binary(var l, var op, var r) -> new Binary(f.apply(l), op, f.apply(r));
      case Conditional(var c, var t, var e) ->
          new Conditional(f.apply(c), f.apply(t), f.apply(e));
    };
  }

  /**
   * Collect all nodes in the expression tree.
   *
   * <p>Returns all nodes including the root, in depth-first pre-order.
   *
   * @param expr the root expression
   * @return a list of all expressions in the tree
   */
  public static List<Expr> getAllNodes(Expr expr) {
    List<Expr> results = new ArrayList<>();
    collectAll(expr, results);
    return results;
  }

  private static void collectAll(Expr expr, List<Expr> accumulator) {
    accumulator.add(expr);
    for (Expr child : getChildren(expr)) {
      collectAll(child, accumulator);
    }
  }

  /**
   * Transform all nodes in the tree from leaves to root (bottom-up).
   *
   * <p>Each node is transformed after its children have been transformed. This is useful for
   * optimisations that need to see the results of transforming sub-expressions first.
   *
   * @param expr the expression to transform
   * @param f the transformation function
   * @return the transformed expression
   */
  public static Expr transformBottomUp(Expr expr, Function<Expr, Expr> f) {
    // First transform all children
    Expr transformed = modifyChildren(expr, child -> transformBottomUp(child, f));
    // Then transform this node
    return f.apply(transformed);
  }

  /**
   * Transform all nodes in the tree from root to leaves (top-down).
   *
   * <p>Each node is transformed before its children are visited. This is useful for macro
   * expansion or early rewriting where the transformation might change the structure.
   *
   * @param expr the expression to transform
   * @param f the transformation function
   * @return the transformed expression
   */
  public static Expr transformTopDown(Expr expr, Function<Expr, Expr> f) {
    // First transform this node
    Expr transformed = f.apply(expr);
    // Then transform all children
    return modifyChildren(transformed, child -> transformTopDown(child, f));
  }
}
