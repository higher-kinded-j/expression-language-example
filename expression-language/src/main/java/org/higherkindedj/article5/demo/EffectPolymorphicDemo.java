// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article5.demo;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.higherkindedj.article5.effect.Pair;
import org.higherkindedj.article5.effect.State;
import org.higherkindedj.article5.effect.StateKind;
import org.higherkindedj.article4.ast.BinaryOp;
import org.higherkindedj.article4.ast.Expr;
import org.higherkindedj.article4.ast.Expr.Binary;
import org.higherkindedj.article4.ast.Expr.Literal;
import org.higherkindedj.article4.ast.Expr.Variable;
import org.higherkindedj.article4.traversal.ExprTraversal;
import org.higherkindedj.hkt.Kind;
import org.higherkindedj.optics.Traversal;
import org.higherkindedj.optics.util.Traversals;

/**
 * Demonstrates effect-polymorphic optics using modifyF with different effects.
 *
 * <p>Key insight: The same traversal works with multiple computational effects. We write the
 * traversal once, then use it with Identity (pure), Optional (fallible), State (stateful), etc.
 */
public final class EffectPolymorphicDemo {

  public static void main(String[] args) {
    System.out.println("Effect-Polymorphic Optics Demo");
    System.out.println("==============================");
    System.out.println();

    demoPureTransformation();
    demoCollectWithState();
    demoSameTraversalDifferentEffects();
  }

  private static void demoPureTransformation() {
    System.out.println("1. Pure transformation (using modify)");
    System.out.println("   -----------------------------------");

    // (x + 1) * (y + 2)
    Expr expr =
        new Binary(
            new Binary(new Variable("x"), BinaryOp.ADD, new Literal(1)),
            BinaryOp.MUL,
            new Binary(new Variable("y"), BinaryOp.ADD, new Literal(2)));

    System.out.println("   Original: " + expr.format());

    // Double all literals using the children traversal + recursion
    Expr doubled =
        ExprTraversal.transformBottomUp(
            expr,
            e -> {
              if (e instanceof Literal(Integer i)) {
                return new Literal(i * 2);
              }
              return e;
            });

    System.out.println("   After doubling literals: " + doubled.format());
    System.out.println();
  }

  private static void demoCollectWithState() {
    System.out.println("2. Collecting variables with State");
    System.out.println("   --------------------------------");

    // (a + b) * (c + d)
    Expr expr =
        new Binary(
            new Binary(new Variable("a"), BinaryOp.ADD, new Variable("b")),
            BinaryOp.MUL,
            new Binary(new Variable("c"), BinaryOp.ADD, new Variable("d")));

    System.out.println("   Expression: " + expr.format());

    // Collect all variable names using State
    Set<String> variables = collectVariables(expr);

    System.out.println("   Variables found: " + variables);
    System.out.println();
  }

  private static Set<String> collectVariables(Expr expr) {
    // Define a collector that adds variable names to a Set
    Function<Expr, State<Set<String>, Expr>> collector =
        e -> {
          if (e instanceof Variable(var name)) {
            return State.<Set<String>>modify(
                    vars -> {
                      Set<String> newVars = new HashSet<>(vars);
                      newVars.add(name);
                      return newVars;
                    })
                .map(v -> e);
          }
          return State.of(e);
        };

    // Use a recursive approach to visit all nodes
    return collectVariablesRecursive(expr, collector).exec(new HashSet<>());
  }

  private static State<Set<String>, Expr> collectVariablesRecursive(
      Expr expr, Function<Expr, State<Set<String>, Expr>> collector) {

    // First collect from this node
    State<Set<String>, Expr> thisNode = collector.apply(expr);

    // Then recursively collect from children
    return thisNode.flatMap(
        e ->
            switch (e) {
              case Literal _ -> State.of(e);
              case Variable _ -> State.of(e);
              case Binary(var l, var op, var r) ->
                  collectVariablesRecursive(l, collector)
                      .flatMap(
                          newL ->
                              collectVariablesRecursive(r, collector)
                                  .map(newR -> new Binary(newL, op, newR)));
              case Expr.Conditional(var c, var t, var el) ->
                  collectVariablesRecursive(c, collector)
                      .flatMap(
                          newC ->
                              collectVariablesRecursive(t, collector)
                                  .flatMap(
                                      newT ->
                                          collectVariablesRecursive(el, collector)
                                              .map(
                                                  newE -> new Expr.Conditional(newC, newT, newE))));
            });
  }

  private static void demoSameTraversalDifferentEffects() {
    System.out.println("3. Same traversal, different effects");
    System.out.println("   ----------------------------------");
    System.out.println("   The children() traversal can work with ANY effect.");
    System.out.println("   Here we demonstrate with Identity and State:");
    System.out.println();

    Expr expr = new Binary(new Literal(1), BinaryOp.ADD, new Literal(2));
    System.out.println("   Expression: " + expr.format());

    // Pure transformation using modify (Identity effect)
    Traversal<Expr, Expr> children = ExprTraversal.children();
    Expr modified =
        Traversals.modify(
            children,
            e -> {
              if (e instanceof Literal(Integer i)) {
                return new Literal(i * 10);
              }
              return e;
            },
            expr);
    System.out.println("   With Identity (pure): " + modified.format());

    // State-based transformation using modifyF
    Pair<Expr, Integer> stateResult =
        children
            .modifyF(
                e -> {
                  if (e instanceof Literal(Integer i)) {
                    // Count and transform
                    return StateKind.<Integer, Expr>widen(
                        State.<Integer>modify(count -> count + 1).map(v -> new Literal(i * 10)));
                  }
                  return StateKind.widen(State.of(e));
                },
                expr,
                StateKind.applicative())
            .let(k -> StateKind.<Integer, Expr>narrow(k).run(0));

    System.out.println(
        "   With State (counting): " + stateResult.first().format() + ", count = " + stateResult.second());
    System.out.println();

    System.out.println("   Key insight: The SAME traversal (children()) works with both");
    System.out.println("   pure and effectful transformations. This is effect polymorphism.");
    System.out.println();
  }
}
