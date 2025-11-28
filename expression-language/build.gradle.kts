plugins {
    id("java")
    application
    id("com.diffplug.spotless") version "8.1.0"
}

group = "org.higherkindedj"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}


application {
    mainClass.set("org.higherkindedj.article3.demo.Article3Demo")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven {
        url= uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

// Higher-Kinded-J version - enable when Maven Central is accessible
val hkjVersion = "0.2.2-SNAPSHOT"

dependencies {
    // Higher-Kinded-J
    implementation("io.github.higher-kinded-j:hkj-core:$hkjVersion")
    annotationProcessor("io.github.higher-kinded-j:hkj-processor:$hkjVersion")
    annotationProcessor("io.github.higher-kinded-j:hkj-processor-plugins:$hkjVersion")


     testImplementation(platform("org.junit:junit-bom:5.13.0"))
     testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    java {
        target("src/**/*.java")
        googleJavaFormat("1.32.0").formatJavadoc(true)
        removeUnusedImports()
        trimTrailingWhitespace()
        licenseHeaderFile(rootProject.file("config/spotless/copyright.txt"), "(package|import|public|@)")
    }
}
