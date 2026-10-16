plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("solarnet.kotlin-base")
}

val webReplSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/tfm/web/webrepl")
val sharedSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/tfm/web/shared")

kotlin {
  js {
    browser { commonWebpackConfig { cssSupport { enabled.set(true) } } }
    binaries.executable()
  }

  sourceSets {
    jsMain {
      kotlin.setSrcDirs(listOf(webReplSourceDirectory))
      dependencies {
        implementation(
            "dev.martianzoo.pets:catalog:${providers.gradleProperty("petsVersion").get()}"
        )
        implementation(project(":engine"))
        implementation("dev.martianzoo.pets:pets:${providers.gradleProperty("petsVersion").get()}")
        implementation(project(":state"))
        implementation(project(":script"))
        implementation(
            "dev.martianzoo.pets:tfm-canon:${providers.gradleProperty("petsVersion").get()}"
        )
        implementation(npm("jquery", "3.7.1"))
        implementation(npm("jquery.terminal", "2.46.2"))
        implementation(devNpm("tslib", "2.8.1"))
      }
    }
    jsTest {
      kotlin.setSrcDirs(
          listOf(
              rootProject.layout.projectDirectory.dir(
                  "test/common/dev/martianzoo/agenttestsupport"
              ),
              rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/testsupport"),
              rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/tfm/tests"),
              rootProject.layout.projectDirectory.dir("test/js/dev/martianzoo/tfm/web/webrepl"),
          )
      )
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.kotest.assertions.core)
        implementation(project(":agent"))
        implementation(project(":state"))
        implementation(project(":tfm-engine"))
        implementation(
            "dev.martianzoo.pets:tfm-fake:${providers.gradleProperty("petsVersion").get()}"
        )
        implementation(project(":tfm-state"))
      }
    }
  }
}

val allBrowserTestsRequested = rootProject.extra["allBrowserTestsRequested"] as Boolean

tasks.named<org.gradle.api.tasks.testing.AbstractTestTask>("jsBrowserTest") {
  if (allBrowserTestsRequested) {
    filter.includeTestsMatching("dev.martianzoo.tfm.web.webrepl.BrowserHistoryTest")
  } else {
    filter.includeTestsMatching("dev.martianzoo.tfm.web.webrepl.BrowserHistoryTest")
    filter.includeTestsMatching(
        "dev.martianzoo.tfm.tests.replays.OtbGame20260828Test.otbGame20260828"
    )
  }
}

tasks.register("test") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs Web browser tests and the selected browser replay."
  dependsOn("jsBrowserTest")
}

tasks.named<ProcessResources>("jsProcessResources") {
  from(webReplSourceDirectory) { include("*.html", "*.css") }
  from(sharedSourceDirectory) { into("assets") }
}

val almanacDistribution by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
  attributes { attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME)) }
}

dependencies {
  almanacDistribution(
      "dev.martianzoo.pets:almanac:${providers.gradleProperty("petsVersion").get()}@zip"
  )
}

tasks.named<ProcessResources>("jsProcessResources") {
  dependsOn(almanacDistribution)
  from(almanacDistribution.map { zipTree(it) }) { into("almanac") }
  // Local art remains a consumer asset; published Almanac does not require it.
  from(rootProject.layout.projectDirectory.dir("_local/images")) {
    include("*.png", "MC/*.png")
    into("almanac/images")
  }
}
