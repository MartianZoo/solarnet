import java.net.URI
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  id("dev.detekt")
  id("org.jetbrains.dokka")
}

detekt {
  buildUponDefaultConfig = true
  config.setFrom(rootProject.file("detekt.yml"))
}

plugins.withType<KotlinBasePlugin>().configureEach {
  // Every Kotlin module here uses strict explicit API mode, so accidental API growth shows up
  // in review.
  extensions.configure<KotlinProjectExtension> { explicitApi() }

  val kotlinVersion = getKotlinPluginVersion()
  configurations
      .matching { it.name != "detekt" }
      .configureEach {
        resolutionStrategy.eachDependency {
          if (requested.group == "org.jetbrains.kotlin") {
            useVersion(kotlinVersion)
            because(
                "Kotlin/JS compilation requires libraries compiled for the project Kotlin version"
            )
          }
        }
      }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
  compilerOptions {
    allWarningsAsErrors.set(true)
    languageVersion.set(KotlinVersion.KOTLIN_2_2)
    apiVersion.set(KotlinVersion.KOTLIN_2_2)
    freeCompilerArgs.addAll(
        "-Wextra",
        "-Xwarning-level=REDUNDANT_VISIBILITY_MODIFIER:disabled",
        "-Xwarning-level=RETURN_VALUE_NOT_USED:disabled",
    )
  }
}

tasks.withType<KotlinJvmCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
    freeCompilerArgs.add("-Xjdk-release=17")
  }
}

tasks.withType<JavaCompile>().configureEach { options.release.set(17) }

dokka {
  dokkaPublications.configureEach { suppressInheritedMembers.set(true) }
  dokkaSourceSets.configureEach {
    documentedVisibilities.set(setOf(VisibilityModifier.Public, VisibilityModifier.Protected))
    jdkVersion.set(17)
    skipEmptyPackages.set(true)
    sourceLink {
      localDirectory.set(project.file("src"))
      remoteUrl.set(URI("https://github.com/MartianZoo/solarnet/tree/main/${project.name}/src"))
      remoteLineSuffix.set("#L")
    }
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  maxParallelForks = 4
  testLogging {
    exceptionFormat = FULL
    showExceptions = true
    showStackTraces = true
  }
}
