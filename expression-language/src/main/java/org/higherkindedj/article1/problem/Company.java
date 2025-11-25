package org.higherkindedj.article1.problem;

import java.util.List;

/**
 * A company with multiple departments.
 */
public record Company(
    String name,
    Address headquarters,
    List<Department> departments
) {}
