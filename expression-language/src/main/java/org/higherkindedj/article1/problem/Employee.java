package org.higherkindedj.article1.problem;

/**
 * An employee with their contact address.
 */
public record Employee(
    String id,
    String name,
    Address address
) {}
