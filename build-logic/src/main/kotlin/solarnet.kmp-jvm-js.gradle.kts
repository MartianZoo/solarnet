plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("solarnet.kotlin-base")
}

kotlin {
  explicitApi()
  jvm()
  js { browser() }

  sourceSets { commonTest { dependencies { implementation(kotlin("test")) } } }
}

val karmaPackageDirectory =
    rootProject.layout.buildDirectory.dir("js/packages/solarnet-${project.name}-test")

val copyCanonResourcesForKarma by
    tasks.registering(Copy::class) {
      dependsOn(":canon:jsProcessResources")
      from(project(":canon").layout.buildDirectory.dir("processedResources/js/main"))
      into(karmaPackageDirectory)
    }

val copyPetsResourcesForKarma by
    tasks.registering(Copy::class) {
      dependsOn(":pets:jsProcessResources")
      from(project(":pets").layout.buildDirectory.dir("processedResources/js/main/pets"))
      into(karmaPackageDirectory.map { it.dir("pets") })
    }

tasks.named("jsBrowserTest") {
  dependsOn(copyCanonResourcesForKarma)
  dependsOn(copyPetsResourcesForKarma)
}

rootProject.tasks
    .matching { it.name == "rootPackageJson" }
    .configureEach {
      dependsOn(copyCanonResourcesForKarma)
      // Transitive resources normally reach the other test packages; Script needs this explicit
      // copy.
      if (project.name == "script") {
        dependsOn(copyPetsResourcesForKarma)
      }
    }
