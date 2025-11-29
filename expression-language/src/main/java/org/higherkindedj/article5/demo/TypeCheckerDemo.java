// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article5.demo;

import org.higherkindedj.article5.effect.Validated;
import org.higherkindedj.article5.typecheck.ExprTypeChecker;
import org.higherkindedj.article5.typecheck.Type;
import org.higherkindedj.article5.typecheck.TypeEnv;
import org.higherkindedj.article5.typecheck.TypeError;
import org.higherkindedj.article5.typecheck.TypeErrors;
import org.higherkindedj.article4.ast.BinaryOp;
import org.higherkindedj.article4.ast.Expr;
import org.higherkindedj.article4.ast.Expr.Binary;
import org.higherkindedj.article4.ast.Expr.Conditional;
import org.higherkindedj.article4.ast.Expr.Literal;
import org.higherkindedj.article4.ast.Expr.Variable;

/**
 * Demonstrates type checking with error accumulation using Validated.
 *
 * <p>Key concept: Validated collects ALL type errors in a single pass, rather than failing on the
 * first error. This provides a better developer experience.
 */
public final class TypeCheckerDemo {

  public static void main(String[] args) {
    System.out.println("Type Checker Demo: Error Accumulation with Validated");
    System.out.println("====================================================");
    System.out.println();

    demoWellTypedExpression();
    demoSingleTypeError();
    demoMultipleTypeErrors();
    demoUndefinedVariables();
    demoConditionalTypeChecking();
  }

  private static void demoWellTypedExpression() {
    System.out.println("1. Well-typed expression");
    System.out.println("   ---------------------");

    // (x + 1) * 2 where x: INT
    Expr expr =
        new Binary(
            new Binary(new Variable("x"), BinaryOp.ADD, new Literal(1)),
            BinaryOp.MUL,
            new Literal(2));

    TypeEnv env = TypeEnv.of("x", Type.INT);

    System.out.println("   Expression: " + expr.format());
    System.out.println("   Environment: x: INT");

    Validated<TypeErrors, Type> result = ExprTypeChecker.typeCheck(expr, env);
    printResult(result);
    System.out.println();
  }

  private static void demoSingleTypeError() {
    System.out.println("2. Single type error");
    System.out.println("   ------------------");

    // 1 + true (type mismatch)
    Expr expr = new Binary(new Literal(1), BinaryOp.ADD, new Literal(true));

    System.out.println("   Expression: " + expr.format());

    Validated<TypeErrors, Type> result = ExprTypeChecker.typeCheck(expr, TypeEnv.empty());
    printResult(result);
    System.out.println();
  }

  private static void demoMultipleTypeErrors() {
    System.out.println("3. Multiple type errors (accumulated in one pass)");
    System.out.println("   -----------------------------------------------");

    // (1 + true) * (false && 42)
    // Two errors: INT+BOOL and BOOL&&INT
    Expr leftError = new Binary(new Literal(1), BinaryOp.ADD, new Literal(true));
    Expr rightError = new Binary(new Literal(false), BinaryOp.AND, new Literal(42));
    Expr expr = new Binary(leftError, BinaryOp.MUL, rightError);

    System.out.println("   Expression: " + expr.format());
    System.out.println("   This expression has TWO type errors.");
    System.out.println("   With Validated, we see BOTH errors in one pass:");

    Validated<TypeErrors, Type> result = ExprTypeChecker.typeCheck(expr, TypeEnv.empty());
    printResult(result);
    System.out.println();
  }

  private static void demoUndefinedVariables() {
    System.out.println("4. Multiple undefined variables");
    System.out.println("   -----------------------------");

    // x + y where neither is defined
    Expr expr = new Binary(new Variable("x"), BinaryOp.ADD, new Variable("y"));

    System.out.println("   Expression: " + expr.format());
    System.out.println("   Environment: (empty)");
    System.out.println("   Both variables are undefined; we see both errors:");

    Validated<TypeErrors, Type> result = ExprTypeChecker.typeCheck(expr, TypeEnv.empty());
    printResult(result);
    System.out.println();
  }

  private static void demoConditionalTypeChecking() {
    System.out.println("5. Conditional with multiple errors");
    System.out.println("   ---------------------------------");

    // if 42 then true else "hello"
    // Errors: condition is INT (not BOOL), branches have different types
    Expr expr = new Conditional(new Literal(42), new Literal(true), new Literal("hello"));

    System.out.println("   Expression: " + expr.format());
    System.out.println("   Multiple errors in conditional:");

    Validated<TypeErrors, Type> result = ExprTypeChecker.typeCheck(expr, TypeEnv.empty());
    printResult(result);
    System.out.println();
  }

  private static void printResult(Validated<TypeErrors, Type> result) {
    switch (result) {
      case Validated.Valid(var type) -> System.out.println("   Result: Valid, type = " + type);
      case Validated.Invalid(var errors) -> {
        System.out.println("   Result: Invalid, " + errors.size() + " error(s):");
        for (TypeError error : errors.errors()) {
          System.out.println("     - " + error.message());
        }
      }
    }
  }
}
