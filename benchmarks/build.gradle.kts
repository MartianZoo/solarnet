plugins {
  id("solarnet.jvm")
  alias(libs.plugins.jmh)
}

dependencies {
  implementation(project(":canon"))
  implementation(project(":engine"))
  implementation(project(":pets"))
}

jmh {
  jmhVersion = "1.37"
  benchmarkMode = listOf("avgt")
  timeUnit = "ms"
  warmupIterations = 5
  warmup = "1s"
  iterations = 10
  timeOnIteration = "1s"
  fork = 2
  jvmArgs = listOf("-Xint")
  threads = 1
}

tasks.named("check") { dependsOn("jmhClasses") }
