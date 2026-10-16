import dev.martianzoo.buildlogic.GenerateCatalogSources

plugins {
  id("pets.kmp-jvm-js")
  alias(libs.plugins.kotlin.serialization)
}

val generateQuerySources by
    tasks.registering(GenerateCatalogSources::class) {
      inputDirectory.set(rootProject.layout.projectDirectory.dir("queries/canon-invariants"))
      logicalPrefix.set("canon-invariants")
      packageName.set("dev.martianzoo.search")
      sourceName.set("Query")
      outputDirectory.set(layout.buildDirectory.dir("generated/querySources"))
    }

kotlin {
  compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
  wasmJs { browser { testTask { useKarma { useChromeHeadless() } } } }
  js { browser { testTask { useKarma { useChromeHeadless() } } } }
  sourceSets {
    commonMain {
      kotlin.setSrcDirs(listOf(rootProject.file("src/common/dev/martianzoo/search")))
      kotlin.srcDir(
          files(project(":codegen").layout.buildDirectory.dir("generated/petsXml"))
              .builtBy(":codegen:generatePetsXml")
      )
      dependencies {
        api(project(":tfm-canon"))
        implementation(project(":catalog"))
        implementation(project(":pets"))
        implementation(libs.kotlinx.serialization.json)
      }
    }
    commonTest {
      kotlin.setSrcDirs(listOf(rootProject.file("test/common/dev/martianzoo/search")))
      kotlin.srcDir(generateQuerySources)
    }
    jvmMain {
      kotlin.setSrcDirs(listOf(rootProject.file("src/jvm/dev/martianzoo/search")))
      dependencies { implementation(libs.jaxen) }
    }
    jsMain { kotlin.setSrcDirs(listOf(rootProject.file("src/js/dev/martianzoo/search"))) }
    wasmJsMain { kotlin.setSrcDirs(listOf(rootProject.file("src/wasmJs/dev/martianzoo/search"))) }
  }
}

val jvmMain = kotlin.targets.getByName("jvm").compilations.getByName("main")

tasks.register<JavaExec>("queryCanon") {
  group = "application"
  description = "Queries authored or elaborated Canon with XPath 1.0."
  classpath = files(jvmMain.output.allOutputs, jvmMain.runtimeDependencyFiles)
  mainClass.set("dev.martianzoo.search.QueryCanonKt")
  providers.gradleProperty("queryForm").orNull?.let { args("--form", it) }
  if (providers.gradleProperty("queryXml").isPresent) args("--xml")
  providers.gradleProperty("query").orNull?.let { args(it) }
}

tasks.named("test") {
  description = "Runs Search tests on JVM, browser JavaScript, and browser Wasm."
  dependsOn("jsBrowserTest", "wasmJsBrowserTest")
}
