package org.higherkindedj.article1.problem;

import java.util.List;

/**
 * A department containing employees.
 */
public record Department(
    String name,
    Employee manager,
    List<Employee> staff
) {}
