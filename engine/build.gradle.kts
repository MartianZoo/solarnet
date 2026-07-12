import java.net.URI

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("org.jetbrains.dokka")
}

val copyCanonResourcesForKarma by tasks.registering(Copy::class) {
  dependsOn(":canon:jsProcessResources")
  from(project(":canon").layout.buildDirectory.dir("processedResources/js/main"))
  into(rootProject.layout.buildDirectory.dir("js/packages/solarnet-engine-test"))
}

val includeSlowTests =
  providers.gradleProperty("includeSlowTests").map(String::toBoolean).orElse(false)

fun explicitlyRequested(taskName: String): Boolean =
  gradle.startParameter.taskNames.any { it == taskName || it.endsWith(":$taskName") }

kotlin {
  jvm()
  js(IR) {
    browser()
  }

  sourceSets {
    commonMain {
      kotlin.srcDir("src/main/java")
      dependencies {
        implementation("io.insert-koin:koin-core:4.1.1")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        implementation(project(":pets"))
      }
    }
    commonTest {
      kotlin.srcDir("src/test/java")
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
  onlyIf {
    includeSlowTests.get() ||
      explicitlyRequested("jsBrowserTest") ||
      explicitlyRequested("allTestsIncludingSlow")
  }
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
      samples.from("src/main/java/dev/martianzoo/tfm/engine/samples.kt")
    }
  }
}
