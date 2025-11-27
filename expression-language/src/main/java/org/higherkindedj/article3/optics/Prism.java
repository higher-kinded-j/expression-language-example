// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article3.optics;

import java.util.Optional;
import java.util.function.Function;

/**
 * A Prism focuses on one variant of a sum type.
 *
 * @param <S> the sum type (e.g., Expr)
 * @param <A> the specific variant (e.g., Literal)
 */
public record Prism<S, A>(Function<S, Optional<A>> getOptional, Function<A, S> build) {

  /** Create a prism from a partial getter and a builder. */
  public static <S, A> Prism<S, A> of(Function<S, Optional<A>> getOptional, Function<A, S> build) {
    return new Prism<>(getOptional, build);
  }

  /** Try to extract the variant from the sum type. */
  public Optional<A> getOptional(S whole) {
    return getOptional.apply(whole);
  }

  /** Construct the sum type from the variant. Always succeeds. */
  public S build(A value) {
    return build.apply(value);
  }

  /** Check if this prism matches the given value. */
  public boolean matches(S whole) {
    return getOptional(whole).isPresent();
  }

  /** Modify the variant if it matches, otherwise return unchanged. */
  public S modify(Function<A, A> f, S whole) {
    return getOptional(whole).map(a -> build(f.apply(a))).orElse(whole);
  }

  /** Compose with a lens. */
  public <B> Optionall<S, B> andThen(Lens<A, B> lens) {
    return Optionall.of(s -> getOptional(s).map(lens::get), (b, s) -> modify(a -> lens.set(b, a), s));
  }

  /** Compose with another prism. */
  public <B> Prism<S, B> andThen(Prism<A, B> other) {
    return Prism.of(s -> getOptional(s).flatMap(other::getOptional), b -> build(other.build(b)));
  }
}
