// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article5.effect;

import java.util.function.Function;

/**
 * The State monad: a computation that threads state through a sequence of operations.
 *
 * <p>State captures the pattern of functions {@code S -> (A, S)}: take a state, produce a result
 * and a new state. This eliminates the need to manually pass state through every function call.
 *
 * <p>Key operations:
 *
 * <ul>
 *   <li>{@link #of} - create a state action that returns a value without modifying state
 *   <li>{@link #get} - access the current state
 *   <li>{@link #put} - replace the state with a new value
 *   <li>{@link #modify} - update the state using a function
 *   <li>{@link #flatMap} - sequence state actions
 * </ul>
 *
 * @param runState the state transition function
 * @param <S> the state type
 * @param <A> the result type
 */
public record State<S, A>(Function<S, Pair<A, S>> runState) {

  /**
   * Create a state action that returns the given value without modifying state.
   *
   * @param value the value to return
   * @param <S> the state type
   * @param <A> the result type
   * @return a state action returning the value
   */
  public static <S, A> State<S, A> of(A value) {
    return new State<>(s -> Pair.of(value, s));
  }

  /**
   * Access the current state.
   *
   * @param <S> the state type
   * @return a state action that returns the current state
   */
  public static <S> State<S, S> get() {
    return new State<>(s -> Pair.of(s, s));
  }

  /**
   * Replace the state with a new value.
   *
   * @param newState the new state
   * @param <S> the state type
   * @return a state action that sets the state and returns nothing meaningful
   */
  public static <S> State<S, Void> put(S newState) {
    return new State<>(s -> Pair.of(null, newState));
  }

  /**
   * Update the state using a function.
   *
   * @param f the state transformation function
   * @param <S> the state type
   * @return a state action that modifies the state
   */
  public static <S> State<S, Void> modify(Function<S, S> f) {
    return new State<>(s -> Pair.of(null, f.apply(s)));
  }

  /**
   * Sequence this state action with another that depends on the result.
   *
   * <p>This is the monadic bind operation. It runs this action, then passes the result to the given
   * function to produce the next action.
   *
   * @param f the function producing the next state action
   * @param <B> the result type of the next action
   * @return a combined state action
   */
  public <B> State<S, B> flatMap(Function<A, State<S, B>> f) {
    return new State<>(
        s -> {
          Pair<A, S> result = runState.apply(s);
          return f.apply(result.first()).runState().apply(result.second());
        });
  }

  /**
   * Transform the result of this state action.
   *
   * @param f the transformation function
   * @param <B> the new result type
   * @return a state action with the transformed result
   */
  public <B> State<S, B> map(Function<A, B> f) {
    return flatMap(a -> of(f.apply(a)));
  }

  /**
   * Run this state action with the given initial state, returning only the result.
   *
   * @param initial the initial state
   * @return the result value
   */
  public A eval(S initial) {
    return runState.apply(initial).first();
  }

  /**
   * Run this state action with the given initial state, returning only the final state.
   *
   * @param initial the initial state
   * @return the final state
   */
  public S exec(S initial) {
    return runState.apply(initial).second();
  }

  /**
   * Run this state action with the given initial state, returning both result and final state.
   *
   * @param initial the initial state
   * @return a pair of (result, final state)
   */
  public Pair<A, S> run(S initial) {
    return runState.apply(initial);
  }
}
