plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("solarnet.kotlin-base")
}

kotlin {
  jvm()
  js { browser() }

  sourceSets { commonTest { dependencies { implementation(kotlin("test")) } } }
}

// Multiplatform modules name their JVM test task `jvmTest`, so plain `gradle test` would skip them.
tasks.register("test") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs this module's JVM test suite."
  dependsOn("jvmTest")
}
