plugins {
  id("solarnet.jvm")
  alias(libs.plugins.jmh)
}

kotlin {
  sourceSets.named("jmh") {
    kotlin.setSrcDirs(
        listOf(rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/benchmarks"))
    )
  }
}

dependencies {
  implementation(project(":game-viewer"))
  implementation(project(":tfm-canon"))
  implementation(project(":engine"))
  implementation(project(":pets"))
  implementation(project(":tfm-engine"))
}

val jmhOutput = layout.buildDirectory.file("results/jmh/output.txt")
val jmhResults = layout.buildDirectory.file("results/jmh/results.txt")

jmh {
  jmhVersion = "1.37"
  benchmarkMode = listOf("avgt")
  timeUnit = "ms"
  warmupIterations = 5
  warmup = "1s"
  iterations = 10
  timeOnIteration = "1s"
  fork = 2
  failOnError = true
  humanOutputFile = jmhOutput.get().asFile
  jvmArgs = listOf("-Xint")
  resultsFile = jmhResults.get().asFile
  threads = 1
}

tasks.named("check") { dependsOn("jmhClasses") }

tasks.named("jmh") {
  doLast {
    val outputFile = outputs.files.single { it.name == "output.txt" }
    if ("<failure>" in outputFile.readText()) {
      throw GradleException("JMH reported a benchmark failure; see $outputFile")
    }
    val resultsFile = outputs.files.single { it.name == "results.txt" }
    if (resultsFile.readLines().size <= 1) {
      throw GradleException("JMH produced no benchmark measurements; see $resultsFile")
    }
  }
}
