// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article5.effect;

import java.util.function.Function;

/**
 * A validation result that can accumulate errors.
 *
 * <p>Unlike {@code Either}, which short-circuits on the first error, {@code Validated} collects all
 * errors before failing. This is the key to error accumulation in type checking and validation.
 *
 * <p>Validated forms an {@code Applicative} but NOT a {@code Monad}. This isn't a limitation; it's
 * the feature. Without {@code flatMap}, independent validations run in parallel (logically),
 * accumulating all their errors.
 *
 * @param <E> the error type (typically a collection or combinable type)
 * @param <A> the success value type
 */
public sealed interface Validated<E, A> {

  /**
   * A successful validation result.
   *
   * @param value the validated value
   * @param <E> the error type
   * @param <A> the success value type
   */
  record Valid<E, A>(A value) implements Validated<E, A> {}

  /**
   * A failed validation with accumulated errors.
   *
   * @param errors the accumulated errors
   * @param <E> the error type
   * @param <A> the success value type
   */
  record Invalid<E, A>(E errors) implements Validated<E, A> {}

  /**
   * Create a successful validation.
   *
   * @param value the success value
   * @param <E> the error type
   * @param <A> the success value type
   * @return a Valid containing the value
   */
  static <E, A> Validated<E, A> valid(A value) {
    return new Valid<>(value);
  }

  /**
   * Create a failed validation.
   *
   * @param errors the errors
   * @param <E> the error type
   * @param <A> the success value type
   * @return an Invalid containing the errors
   */
  static <E, A> Validated<E, A> invalid(E errors) {
    return new Invalid<>(errors);
  }

  /**
   * Transform the success value if present.
   *
   * @param f the transformation function
   * @param <B> the new success type
   * @return a new Validated with the transformed value, or the original errors
   */
  default <B> Validated<E, B> map(Function<A, B> f) {
    return switch (this) {
      case Valid(var value) -> new Valid<>(f.apply(value));
      case Invalid(var errors) -> new Invalid<>(errors);
    };
  }

  /**
   * Check if this is a successful validation.
   *
   * @return true if Valid, false if Invalid
   */
  default boolean isValid() {
    return this instanceof Valid;
  }

  /**
   * Get the success value if present.
   *
   * @return the value
   * @throws IllegalStateException if this is Invalid
   */
  default A getValue() {
    return switch (this) {
      case Valid(var value) -> value;
      case Invalid _ -> throw new IllegalStateException("Cannot get value from Invalid");
    };
  }

  /**
   * Get the errors if present.
   *
   * @return the errors
   * @throws IllegalStateException if this is Valid
   */
  default E getErrors() {
    return switch (this) {
      case Valid _ -> throw new IllegalStateException("Cannot get errors from Valid");
      case Invalid(var errors) -> errors;
    };
  }
}
