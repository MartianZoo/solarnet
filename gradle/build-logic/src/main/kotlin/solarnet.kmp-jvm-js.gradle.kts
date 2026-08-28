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

// Karma serves each module's browser tests out of that module's own package directory, so the Canon
// and Pets resources the tests load have to be copied in next to them.
val copyResourcesForKarma by
    tasks.registering(Copy::class) {
      dependsOn(":tfm-canon:jsProcessResources", ":pets:jsProcessResources")
      from(project(":tfm-canon").layout.buildDirectory.dir("processedResources/js/main"))
      from(project(":pets").layout.buildDirectory.dir("processedResources/js/main/pets")) {
        into("pets")
      }
      into(rootProject.layout.buildDirectory.dir("js/packages/solarnet-${project.name}-test"))
    }

tasks.named("jsBrowserTest") { dependsOn(copyResourcesForKarma) }

rootProject.tasks
    .matching { it.name == "rootPackageJson" }
    .configureEach { dependsOn(copyResourcesForKarma) }
