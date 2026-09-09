plugins { id("solarnet.kmp-jvm-js") }

kotlin {
  sourceSets {
    commonTest {
      kotlin.setSrcDirs(
          listOf(
              rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/testsupport"),
              rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/tfm/tests"),
          )
      )
      dependencies {
        implementation(libs.kotest.assertions.core)
        implementation(project(":engine"))
        implementation(project(":pets"))
        implementation(project(":script"))
        implementation(project(":tfm-canon"))
        implementation(project(":tfm-fake"))
        implementation(project(":tfm-engine"))
      }
    }
    jsTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/js/dev/martianzoo/tfm/tests"))
      )
    }
    jvmTest {
      kotlin.setSrcDirs(
          listOf(
              rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/tfm/tests"),
              rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/tfm/randomcards"),
              rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/tfm/testlib"),
          )
      )
      kotlin.exclude("PetGenerator.kt", "testHelpers.kt")
    }
  }
}

val randomCardCount = providers.gradleProperty("randomCardCount").orElse("12")
val randomCardSeed = providers.gradleProperty("randomCardSeed")
val randomCardOutput = providers.gradleProperty("randomCardOutput")
val jvmTestCompilation = kotlin.targets.getByName("jvm").compilations.getByName("test")

tasks.register<JavaExec>("sampleRandomCards") {
  group = "verification"
  description = "Prints or writes randomly generated project cards as raw Pets."
  dependsOn(jvmTestCompilation.compileTaskProvider)
  classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
  mainClass = "dev.martianzoo.tfm.randomcards.RandomCardGenerator"
  args(randomCardCount.get())
  randomCardSeed.orNull?.let { args(it) }
  randomCardOutput.orNull?.let {
    require(randomCardSeed.isPresent) { "randomCardOutput requires randomCardSeed" }
    args(it)
  }
}

val browserTestsRequested =
    gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "jsBrowserTest" }

// A routine build exercises the most extensive shared replay in Chrome. Naming the browser task
// directly removes this filter and runs every shared Terraforming Mars test. Other full-game
// replays live in jvmTest so they cannot be selected by a browser task.
tasks.named<org.gradle.api.tasks.testing.AbstractTestTask>("jsBrowserTest") {
  if (!browserTestsRequested) {
    filter.includeTestsMatching(
        "dev.martianzoo.tfm.tests.replays.OtbGame20260828Test.otbGame20260828"
    )
  }
}

tasks.register("jsBrowserSmokeTest") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs one extensive Terraforming Mars game in a browser."
  dependsOn("jsBrowserTest")
}

// Generated game-specific Catalogs deliberately have distinct class universes. Periodic worker
// replacement keeps the complete replay suite from retaining all of them in one test JVM.
tasks.named<Test>("jvmTest") { forkEvery = 50 }
