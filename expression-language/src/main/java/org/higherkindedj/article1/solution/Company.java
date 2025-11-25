package org.higherkindedj.article1.solution;

import java.util.List;

/**
 * Company with lenses.
 */
public record Company(
    String name,
    Address headquarters,
    List<Department> departments
) {

    public static final class Lenses {
        private Lenses() {}

        public static Lens<Company, String> name() {
            return Lens.of(
                Company::name,
                (newName, co) -> new Company(newName, co.headquarters(), co.departments())
            );
        }

        public static Lens<Company, Address> headquarters() {
            return Lens.of(
                Company::headquarters,
                (newHq, co) -> new Company(co.name(), newHq, co.departments())
            );
        }

        public static Lens<Company, List<Department>> departments() {
            return Lens.of(
                Company::departments,
                (newDepts, co) -> new Company(co.name(), co.headquarters(), newDepts)
            );
        }
    }
}
