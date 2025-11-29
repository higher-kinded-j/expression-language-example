// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article5.typecheck;

import java.util.List;
import java.util.stream.Stream;

/**
 * A type error with a descriptive message.
 *
 * @param message the error message
 */
public record TypeError(String message) {

  @Override
  public String toString() {
    return message;
  }
}

/**
 * A collection of type errors that can be accumulated.
 *
 * <p>This type supports the Semigroup-like operation needed by {@code Validated}: combining two
 * error collections into one.
 *
 * @param errors the list of type errors
 */
record TypeErrors(List<TypeError> errors) {

  /**
   * Create a TypeErrors containing a single error.
   *
   * @param message the error message
   * @return a TypeErrors with one error
   */
  public static TypeErrors single(String message) {
    return new TypeErrors(List.of(new TypeError(message)));
  }

  /**
   * Combine this TypeErrors with another, accumulating all errors.
   *
   * @param other the other errors to combine
   * @return a new TypeErrors containing all errors from both
   */
  public TypeErrors combine(TypeErrors other) {
    return new TypeErrors(Stream.concat(errors.stream(), other.errors.stream()).toList());
  }

  /**
   * Check if there are any errors.
   *
   * @return true if there is at least one error
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Get the number of errors.
   *
   * @return the error count
   */
  public int size() {
    return errors.size();
  }

  @Override
  public String toString() {
    return errors.stream().map(TypeError::message).reduce((a, b) -> a + "\n" + b).orElse("");
  }
}
