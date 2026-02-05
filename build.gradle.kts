plugins {
    application
}

group = "org.higherkindedj"
version = "1.0-SNAPSHOT"

application {
    mainClass.set(project.findProperty("mainClass")?.toString() ?: "org.higherkindedj.example.tutorials.TutorialGettingStarted")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("--enable-preview"))
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}

tasks.withType<Test> {
    jvmArgs("--enable-preview")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.higher-kinded-j:hkj-core:0.3.4")
    annotationProcessor("io.github.higher-kinded-j:hkj-processor-plugins:0.3.4")
}

