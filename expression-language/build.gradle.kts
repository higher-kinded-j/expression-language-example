plugins {
    id("java")
    application
    // Spotless - enable when Gradle Plugin Portal is accessible
    // id("com.diffplug.spotless") version "8.1.0"
}

group = "org.higherkindedj"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("--enable-preview"))
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}

application {
    mainClass.set("org.higherkindedj.article2.demo.Article2Demo")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

// Higher-Kinded-J version - enable when Maven Central is accessible
val hkjVersion = "0.2.1"

dependencies {
    // Higher-Kinded-J - uncomment when Maven Central is accessible
    // implementation("io.github.higher-kinded-j:hkj-core:$hkjVersion")
    // annotationProcessor("io.github.higher-kinded-j:hkj-processor:$hkjVersion")

    // Testing - uncomment when Maven Central is accessible
    // testImplementation(platform("org.junit:junit-bom:5.13.0"))
    // testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

// Spotless configuration - enable when Gradle Plugin Portal is accessible
// spotless {
//     lineEndings = com.diffplug.spotless.LineEnding.UNIX
//     java {
//         target("src/**/*.java")
//         googleJavaFormat("1.32.0").formatJavadoc(true)
//         removeUnusedImports()
//         trimTrailingWhitespace()
//         licenseHeaderFile(rootProject.file("config/spotless/copyright.txt"), "(package|import|public|@)")
//     }
// }