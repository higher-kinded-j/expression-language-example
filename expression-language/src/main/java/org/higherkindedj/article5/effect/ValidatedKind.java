// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article5.effect;

import java.util.function.BiFunction;
import java.util.function.Function;
import org.higherkindedj.hkt.Applicative;
import org.higherkindedj.hkt.Kind;

/**
 * Higher-kinded encoding for {@link Validated}.
 *
 * <p>This provides the bridge between our {@code Validated} type and Higher-Kinded-J's type class
 * system. It enables us to use {@code Validated} with {@code modifyF} and other effect-polymorphic
 * operations.
 *
 * <p>The key insight: {@code Validated} forms an {@code Applicative} but NOT a {@code Monad}. This
 * is what enables error accumulation.
 *
 * @param <E> the error type (must be combinable via a Semigroup-like operation)
 */
public final class ValidatedKind<E> {

  private ValidatedKind() {}

  /**
   * Witness type for Validated in the Kind encoding.
   *
   * @param <E> the error type
   */
  public interface Mu<E> {}

  /** Wrapper to make Validated work with Kind. */
  private record ValidatedHolder<E, A>(Validated<E, A> validated) implements Kind<Mu<E>, A> {}

  /**
   * Wrap a Validated in the Kind encoding.
   *
   * @param validated the validated to wrap
   * @param <E> the error type
   * @param <A> the success type
   * @return the wrapped validated
   */
  public static <E, A> Kind<Mu<E>, A> widen(Validated<E, A> validated) {
    return new ValidatedHolder<>(validated);
  }

  /**
   * Unwrap a Kind to get the underlying Validated.
   *
   * @param kind the kind to unwrap
   * @param <E> the error type
   * @param <A> the success type
   * @return the underlying validated
   */
  @SuppressWarnings("unchecked")
  public static <E, A> Validated<E, A> narrow(Kind<Mu<E>, A> kind) {
    return ((ValidatedHolder<E, A>) kind).validated();
  }

  /**
   * Create an Applicative instance for Validated with the given error combiner.
   *
   * <p>The error combiner is used to accumulate errors when combining multiple Invalid results.
   *
   * @param combineErrors function to combine two error values
   * @param <E> the error type
   * @return an Applicative instance for Validated
   */
  public static <E> Applicative<Mu<E>> applicative(BiFunction<E, E, E> combineErrors) {
    return new Applicative<>() {
      @Override
      public <A> Kind<Mu<E>, A> of(A value) {
        return widen(Validated.valid(value));
      }

      @Override
      public <A, B> Kind<Mu<E>, B> map(Kind<Mu<E>, A> fa, Function<A, B> f) {
        return widen(narrow(fa).map(f));
      }

      @Override
      public <A, B, C> Kind<Mu<E>, C> map2(
          Kind<Mu<E>, A> fa, Kind<Mu<E>, B> fb, BiFunction<A, B, C> f) {
        Validated<E, A> va = narrow(fa);
        Validated<E, B> vb = narrow(fb);

        return switch (va) {
          case Validated.Valid(var a) ->
              switch (vb) {
                case Validated.Valid(var b) -> widen(Validated.valid(f.apply(a, b)));
                case Validated.Invalid(var eb) -> widen(Validated.invalid(eb));
              };
          case Validated.Invalid(var ea) ->
              switch (vb) {
                case Validated.Valid _ -> widen(Validated.invalid(ea));
                case Validated.Invalid(var eb) -> widen(Validated.invalid(combineErrors.apply(ea, eb)));
              };
        };
      }
    };
  }
}
