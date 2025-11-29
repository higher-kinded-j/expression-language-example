// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article5.demo;

/**
 * Main entry point for Article 5 demonstrations.
 *
 * <p>Runs all demos showing effect-polymorphic optics:
 *
 * <ul>
 *   <li>TypeCheckerDemo: Type checking with error accumulation using Validated
 *   <li>InterpreterDemo: Expression evaluation with State monad
 *   <li>EffectPolymorphicDemo: Same traversal with different effects
 * </ul>
 */
public final class Article5Demo {

  public static void main(String[] args) {
    System.out.println("╔══════════════════════════════════════════════════════════════════╗");
    System.out.println("║           Article 5: Effect-Polymorphic Optics                   ║");
    System.out.println("║           Functional Optics for Modern Java                      ║");
    System.out.println("╚══════════════════════════════════════════════════════════════════╝");
    System.out.println();

    // Run Type Checker demo
    TypeCheckerDemo.main(args);

    System.out.println();
    System.out.println("────────────────────────────────────────────────────────────────────");
    System.out.println();

    // Run Interpreter demo
    InterpreterDemo.main(args);

    System.out.println();
    System.out.println("────────────────────────────────────────────────────────────────────");
    System.out.println();

    // Run Effect Polymorphic demo
    EffectPolymorphicDemo.main(args);

    System.out.println();
    System.out.println("════════════════════════════════════════════════════════════════════");
    System.out.println("Article 5 demonstrations complete.");
    System.out.println("════════════════════════════════════════════════════════════════════");
  }
}
