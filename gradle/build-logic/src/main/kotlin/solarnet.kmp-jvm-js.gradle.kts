import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("solarnet.kotlin-base")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
  jvm()
  js { browser() }

  sourceSets {
    commonTest { dependencies { implementation(kotlin("test")) } }
    jvmTest {
      dependencies {
        implementation(project.dependencies.platform(libs.findLibrary("junit-bom").get()))
        implementation(kotlin("test-junit5"))
        runtimeOnly(libs.findLibrary("junit-platform-launcher").get())
      }
    }
  }
}

// Multiplatform modules name their JVM test task `jvmTest`, so plain `gradle test` would skip them.
tasks.register("test") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs this module's JVM test suite."
  dependsOn("jvmTest")
}
