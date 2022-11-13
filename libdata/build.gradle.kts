import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    `java-library` // what does this do?
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    api("com.google.guava:guava:31.1-jre")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("com.google.truth:truth:1.0")
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    exceptionFormat = FULL
    showExceptions = true
    showStackTraces = true
  }
}
