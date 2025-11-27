// Copyright (c) 2025 Magnus Smith
// Licensed under the MIT License. See LICENSE.md in the project root for license information.
package org.higherkindedj.article4.traversal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A Traversal focuses on zero or more elements of type A within a structure S.
 *
 * <p>Traversals generalise both Lens (exactly one element) and Prism (zero or one element) to
 * handle multiple targets. They are ideal for:
 *
 * <ul>
 *   <li>Operating on all elements in a collection
 *   <li>Visiting all nodes in a tree
 *   <li>Transforming multiple matching values at once
 * </ul>
 *
 * @param <S> the source type (the whole structure)
 * @param <A> the focus type (the elements we target)
 */
public interface Traversal<S, A> {

  /**
   * Modify all focused elements.
   *
   * @param f the transformation function
   * @param source the structure to transform
   * @return a new structure with all focused elements transformed
   */
  S modify(Function<A, A> f, S source);

  /**
   * Get all focused elements.
   *
   * @param source the structure to extract from
   * @return a list of all focused elements
   */
  List<A> getAll(S source);

  /**
   * Set all focused elements to the same value.
   *
   * @param value the value to set
   * @param source the structure to transform
   * @return a new structure with all focused elements set to value
   */
  default S set(A value, S source) {
    return modify(_ -> value, source);
  }

  /**
   * Compose this traversal with another traversal.
   *
   * @param <B> the focus type of the inner traversal
   * @param inner the traversal to compose with
   * @return a new traversal that focuses through both
   */
  default <B> Traversal<S, B> andThen(Traversal<A, B> inner) {
    Traversal<S, A> outer = this;
    return new Traversal<>() {
      @Override
      public S modify(Function<B, B> f, S source) {
        return outer.modify(a -> inner.modify(f, a), source);
      }

      @Override
      public List<B> getAll(S source) {
        List<B> results = new ArrayList<>();
        for (A a : outer.getAll(source)) {
          results.addAll(inner.getAll(a));
        }
        return results;
      }
    };
  }

  /**
   * Filter this traversal to only focus on elements matching a predicate.
   *
   * @param predicate the filter condition
   * @return a new traversal that only focuses on matching elements
   */
  default Traversal<S, A> filtered(java.util.function.Predicate<A> predicate) {
    Traversal<S, A> base = this;
    return new Traversal<>() {
      @Override
      public S modify(Function<A, A> f, S source) {
        return base.modify(a -> predicate.test(a) ? f.apply(a) : a, source);
      }

      @Override
      public List<A> getAll(S source) {
        return base.getAll(source).stream().filter(predicate).toList();
      }
    };
  }
}
