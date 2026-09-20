import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
  id("solarnet.kmp-jvm-js")
  jacoco
}

val commonSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/pets")
val commonTestSupportDirectory =
    rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/testsupport")
val commonTestDirectories =
    listOf(
        commonTestSupportDirectory,
        rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/pets"),
        rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/tfm/pets"),
        rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/tfm/testlib"),
    )

kotlin {
  sourceSets {
    commonMain {
      kotlin.setSrcDirs(listOf(commonSourceDirectory))
      dependencies {
        // Pin the exact tested JitPack build from the better-parse fork; tag lookup was unreliable.
        implementation(libs.better.parse)
      }
    }
    commonTest {
      kotlin.setSrcDirs(commonTestDirectories)
      dependencies { implementation(libs.kotest.assertions.core) }
    }
    jsMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/pets"))
      )
    }
    jsTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/js/dev/martianzoo/tfm/pets"))
      )
    }
    jvmMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/pets"))
      )
    }
    jvmTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/pets"))
      )
    }
  }
}

val jvmMainCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
val jvmTest by tasks.existing(Test::class)

tasks.register<JacocoReport>("jvmTestCoverage") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs the Pets JVM test suite and reports Pets production-code coverage."
  dependsOn(jvmTest)
  executionData(
      jvmTest.map { task ->
        requireNotNull(task.extensions.getByType<JacocoTaskExtension>().destinationFile)
      }
  )
  classDirectories.from(jvmMainCompilation.output.classesDirs)
  sourceDirectories.from(
      commonSourceDirectory,
      rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/pets"),
  )
  reports {
    html.required.set(true)
    xml.required.set(true)
  }
}
