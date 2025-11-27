// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article3.optics;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An Optional (affine traversal) focuses on at most one value.
 *
 * <p>This is the result of composing a Lens with a Prism.
 *
 * @param <S> the type of the whole structure
 * @param <A> the type of the focused part (if it exists)
 */
public record Optionall<S, A>(Function<S, Optional<A>> getOptional, BiFunction<A, S, S> set) {

  /** Create an Optional from getter and setter. */
  public static <S, A> Optionall<S, A> of(
      Function<S, Optional<A>> getOptional, BiFunction<A, S, S> set) {
    return new Optionall<>(getOptional, set);
  }

  /** Try to get the focused value. */
  public Optional<A> getOptional(S whole) {
    return getOptional.apply(whole);
  }

  /** Set a new value, returning updated structure. Only applies if the path exists. */
  public S set(A newValue, S whole) {
    return set.apply(newValue, whole);
  }

  /** Modify the focused value if it exists. */
  public S modify(Function<A, A> f, S whole) {
    return getOptional(whole).map(a -> set(f.apply(a), whole)).orElse(whole);
  }

  /** Compose with a lens. */
  public <B> Optionall<S, B> andThen(Lens<A, B> lens) {
    return Optionall.of(s -> getOptional(s).map(lens::get), (b, s) -> modify(a -> lens.set(b, a), s));
  }
}
