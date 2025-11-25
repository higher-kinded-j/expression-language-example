plugins {
    id("java")
}

group = "org.higherkindedj"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

// Note: higher-kinded-j requires JDK 24+. This project uses JDK 21 with
// manual lens implementations to demonstrate the concepts. When JDK 24
// becomes widely available, uncomment the following:
//
// val hkjVersion = "0.2.1"
// dependencies {
//     implementation("io.github.higher-kinded-j:hkj-core:$hkjVersion")
//     annotationProcessor("io.github.higher-kinded-j:hkj-processor:$hkjVersion")
// }