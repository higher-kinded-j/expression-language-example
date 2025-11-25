package org.higherkindedj.article1.problem;

/**
 * A simple address record demonstrating immutable data in Java.
 */
public record Address(
    String street,
    String city,
    String postcode
) {}
