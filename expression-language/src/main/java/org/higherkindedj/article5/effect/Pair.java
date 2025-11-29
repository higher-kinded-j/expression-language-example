// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article5.effect;

/**
 * A simple pair (tuple) of two values.
 *
 * <p>Used by the State monad to return both a result value and the updated state.
 *
 * @param first the first value
 * @param second the second value
 * @param <A> the type of the first value
 * @param <B> the type of the second value
 */
public record Pair<A, B>(A first, B second) {

  /**
   * Create a pair from two values.
   *
   * @param first the first value
   * @param second the second value
   * @param <A> the type of the first value
   * @param <B> the type of the second value
   * @return a new pair
   */
  public static <A, B> Pair<A, B> of(A first, B second) {
    return new Pair<>(first, second);
  }
}
