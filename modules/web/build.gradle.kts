plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("solarnet.kotlin-base")
}

val webReplSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/tfm/web/webrepl")
val classViewerSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/tfm/web/classviewer")
val sharedSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/tfm/web/shared")

kotlin {
  js {
    browser { commonWebpackConfig { cssSupport { enabled.set(true) } } }
    binaries.executable()
  }

  sourceSets {
    jsMain {
      kotlin.setSrcDirs(listOf(webReplSourceDirectory, classViewerSourceDirectory))
      dependencies {
        implementation(project(":engine"))
        implementation(project(":pets"))
        implementation(project(":script"))
        implementation(project(":tfm-canon"))
        implementation(project(":tfm-text"))
        implementation(npm("jquery", "3.7.1"))
        implementation(npm("jquery.terminal", "2.46.1"))
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
              rootProject.layout.projectDirectory.dir("test/js/dev/martianzoo/tfm/pets"),
              rootProject.layout.projectDirectory.dir("test/js/dev/martianzoo/tfm/web/classviewer"),
              rootProject.layout.projectDirectory.dir("test/js/dev/martianzoo/tfm/web/webrepl"),
          )
      )
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.kotest.assertions.core)
        implementation(project(":agent"))
        implementation(project(":state"))
        implementation(project(":tfm-engine"))
        implementation(project(":tfm-fake"))
      }
    }
  }
}

val allBrowserTestsRequested = rootProject.extra["allBrowserTestsRequested"] as Boolean

tasks.named<org.gradle.api.tasks.testing.AbstractTestTask>("jsBrowserTest") {
  if (allBrowserTestsRequested) {
    filter.includeTestsMatching("dev.martianzoo.tfm.web.webrepl.BrowserHistoryTest")
  } else {
    filter.includeTestsMatching("dev.martianzoo.tfm.pets.BrowserPetsTest")
    filter.includeTestsMatching("dev.martianzoo.tfm.web.classviewer.EnglishCardTextBrowserTest")
    filter.includeTestsMatching("dev.martianzoo.tfm.web.webrepl.BrowserHistoryTest")
    filter.includeTestsMatching(
        "dev.martianzoo.tfm.tests.replays.OtbGame20260828Test.otbGame20260828"
    )
  }
}

tasks.register("test") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs the repository's browser-specific tests and selected browser replay."
  dependsOn("jsBrowserTest")
}

tasks.named<ProcessResources>("jsProcessResources") {
  from(webReplSourceDirectory) { include("*.html", "*.css") }
  from(classViewerSourceDirectory) {
    include("*.html", "*.css")
    into("classviewer")
  }
  from(sharedSourceDirectory) { into("assets") }
}
