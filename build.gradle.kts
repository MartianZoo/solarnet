import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

subprojects {
  repositories { mavenCentral() }

  tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
      exceptionFormat = FULL
      showExceptions = true
      showStackTraces = true
    }
  }
}
