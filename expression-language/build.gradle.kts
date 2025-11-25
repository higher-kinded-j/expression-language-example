plugins {
    id("java")
}

group = "org.higherkindedj"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

repositories {
    mavenCentral()
}

// Higher-Kinded-J version - enable when Maven Central is accessible
val hkjVersion = "0.2.1"

dependencies {
    // Higher-Kinded-J - uncomment when Maven Central is accessible
    // implementation("io.github.higher-kinded-j:hkj-core:$hkjVersion")
    // annotationProcessor("io.github.higher-kinded-j:hkj-processor:$hkjVersion")

    // Testing - commented until Maven Central is accessible
    // testImplementation(platform("org.junit:junit-bom:5.10.0"))
    // testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

// Spotless configuration - uncomment when plugin repository is accessible
// To enable: add id("com.diffplug.spotless") version "6.25.0" to plugins block
/*
spotless {
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    java {
        target("src/**/*.java")
        googleJavaFormat("1.22.0").reflowLongStrings().formatJavadoc(true)
        removeUnusedImports()
        cleanthat()
            .sourceCompatibility("24")
            .addMutator("UnnecessaryFullyQualifiedName")
        trimTrailingWhitespace()
        licenseHeaderFile(file("config/spotless/copyright.txt"), "(package|import|public|@)")
    }
}
*/