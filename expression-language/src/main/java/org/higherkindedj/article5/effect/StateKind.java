// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article5.effect;

import java.util.function.BiFunction;
import java.util.function.Function;
import org.higherkindedj.hkt.Applicative;
import org.higherkindedj.hkt.Kind;
import org.higherkindedj.hkt.Monad;

/**
 * Higher-kinded encoding for {@link State}.
 *
 * <p>This provides the bridge between our {@code State} type and Higher-Kinded-J's type class
 * system. Unlike {@code Validated}, {@code State} forms both an {@code Applicative} AND a {@code
 * Monad}, because state threading is inherently sequential.
 *
 * @param <S> the state type
 */
public final class StateKind<S> {

  private StateKind() {}

  /**
   * Witness type for State in the Kind encoding.
   *
   * @param <S> the state type
   */
  public interface Mu<S> {}

  /** Wrapper to make State work with Kind. */
  private record StateHolder<S, A>(State<S, A> state) implements Kind<Mu<S>, A> {}

  /**
   * Wrap a State in the Kind encoding.
   *
   * @param state the state to wrap
   * @param <S> the state type
   * @param <A> the result type
   * @return the wrapped state
   */
  public static <S, A> Kind<Mu<S>, A> widen(State<S, A> state) {
    return new StateHolder<>(state);
  }

  /**
   * Unwrap a Kind to get the underlying State.
   *
   * @param kind the kind to unwrap
   * @param <S> the state type
   * @param <A> the result type
   * @return the underlying state
   */
  @SuppressWarnings("unchecked")
  public static <S, A> State<S, A> narrow(Kind<Mu<S>, A> kind) {
    return ((StateHolder<S, A>) kind).state();
  }

  /**
   * Evaluate the state action within a Kind, returning only the result.
   *
   * @param kind the wrapped state action
   * @param initial the initial state
   * @param <S> the state type
   * @param <A> the result type
   * @return the result value
   */
  public static <S, A> A eval(Kind<Mu<S>, A> kind, S initial) {
    return narrow(kind).eval(initial);
  }

  /**
   * Create a Monad instance for State.
   *
   * <p>State is a full Monad, enabling sequencing of state-dependent computations.
   *
   * @param <S> the state type
   * @return a Monad instance for State
   */
  public static <S> Monad<Mu<S>> monad() {
    return new Monad<>() {
      @Override
      public <A> Kind<Mu<S>, A> of(A value) {
        return widen(State.of(value));
      }

      @Override
      public <A, B> Kind<Mu<S>, B> map(Kind<Mu<S>, A> fa, Function<A, B> f) {
        return widen(narrow(fa).map(f));
      }

      @Override
      public <A, B, C> Kind<Mu<S>, C> map2(
          Kind<Mu<S>, A> fa, Kind<Mu<S>, B> fb, BiFunction<A, B, C> f) {
        return widen(narrow(fa).flatMap(a -> narrow(fb).map(b -> f.apply(a, b))));
      }

      @Override
      public <A, B> Kind<Mu<S>, B> flatMap(Kind<Mu<S>, A> fa, Function<A, Kind<Mu<S>, B>> f) {
        return widen(narrow(fa).flatMap(a -> narrow(f.apply(a))));
      }
    };
  }

  /**
   * Create an Applicative instance for State.
   *
   * <p>Applicative operations sequence left-to-right, threading state through.
   *
   * @param <S> the state type
   * @return an Applicative instance for State
   */
  public static <S> Applicative<Mu<S>> applicative() {
    return monad(); // Monad extends Applicative
  }
}
