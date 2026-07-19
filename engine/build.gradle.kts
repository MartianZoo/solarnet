import java.net.URI

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("org.jetbrains.dokka")
}

val copyCanonResourcesForKarma by
    tasks.registering(Copy::class) {
      dependsOn(":canon:jsProcessResources")
      from(project(":canon").layout.buildDirectory.dir("processedResources/js/main"))
      into(rootProject.layout.buildDirectory.dir("js/packages/solarnet-engine-test"))
    }

val copyPetsResourcesForKarma by
    tasks.registering(Copy::class) {
      dependsOn(":pets:jsProcessResources")
      from(project(":pets").layout.buildDirectory.dir("processedResources/js/main/pets"))
      into(rootProject.layout.buildDirectory.dir("js/packages/solarnet-engine-test/pets"))
    }

val includeSlowTests = providers.gradleProperty("includeSlowTests").orNull?.toBoolean() == true

val requestedTaskNames = gradle.startParameter.taskNames
val slowTestsExplicitlyRequested = requestedTaskNames.any {
  it == "jsBrowserTest" ||
      it.endsWith(":jsBrowserTest") ||
      it == "allTestsIncludingSlow" ||
      it.endsWith(":allTestsIncludingSlow")
}

kotlin {
  jvm()
  js(IR) {
    browser()
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation("io.insert-koin:koin-core:4.1.1")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        implementation(project(":pets"))
      }
    }
    commonTest {
      dependencies {
        implementation(kotlin("test"))
        implementation("io.kotest:kotest-assertions-core:6.1.11")
        implementation(project(":canon")) // easiest to test the engine this way
      }
    }
  }
}

tasks.named("jsBrowserTest") {
  dependsOn(copyCanonResourcesForKarma)
  dependsOn(copyPetsResourcesForKarma)
  enabled = includeSlowTests || slowTestsExplicitlyRequested
}

tasks.register("allTestsIncludingSlow") {
  group = "verification"
  description = "Runs all engine tests, including slow browser tests."
  dependsOn("allTests")
  dependsOn("jsBrowserTest")
}

dokka {
  dokkaSourceSets {
    configureEach {
      sourceLink {
        localDirectory.set(file("src"))
        remoteUrl.set(URI("https://github.com/MartianZoo/solarnet/tree/main/engine/src"))
        remoteLineSuffix.set("#L")
      }
    }
    named("commonMain") {
      samples.from("src/commonMain/kotlin/dev/martianzoo/tfm/engine/samples.kt")
    }
  }
}
