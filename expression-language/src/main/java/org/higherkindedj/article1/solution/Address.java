package org.higherkindedj.article1.solution;

/**
 * Address record - identical to the problem package, but with lenses defined.
 *
 * <p>In production code with higher-kinded-j (JDK 24+), you would use
 * {@code @GenerateLenses} to auto-generate these. Here we define them
 * manually to work with JDK 21.</p>
 */
public record Address(
    String street,
    String city,
    String postcode
) {

    /**
     * Lenses for Address fields.
     *
     * <p>With {@code @GenerateLenses}, this class would be auto-generated.</p>
     */
    public static final class Lenses {
        private Lenses() {}

        public static Lens<Address, String> street() {
            return Lens.of(
                Address::street,
                (newStreet, addr) -> new Address(newStreet, addr.city(), addr.postcode())
            );
        }

        public static Lens<Address, String> city() {
            return Lens.of(
                Address::city,
                (newCity, addr) -> new Address(addr.street(), newCity, addr.postcode())
            );
        }

        public static Lens<Address, String> postcode() {
            return Lens.of(
                Address::postcode,
                (newPostcode, addr) -> new Address(addr.street(), addr.city(), newPostcode)
            );
        }
    }
}
