plugins { id("solarnet.kmp-jvm-js") }

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(project(":pets"))
        implementation(project(":canon"))
      }
    }
    commonTest { dependencies { implementation(libs.kotest.assertions.core) } }
  }
}

tasks.named<Copy>("copyResourcesForKarma") {
  dependsOn("jsProcessResources")
  from(layout.buildDirectory.dir("processedResources/js/main"))
}

tasks.register<JavaExec>("writeEnglishCardTextCurrent") {
  group = "verification"
  description = "Writes the English renderer's current canonical-card output snapshot."
  dependsOn("jvmTestClasses")
  classpath(
      layout.buildDirectory.dir("classes/kotlin/jvm/test"),
      layout.buildDirectory.dir("classes/kotlin/jvm/main"),
      configurations.named("jvmTestRuntimeClasspath"),
  )
  mainClass = "dev.martianzoo.tfm.language.EnglishCardTextCurrentGenerator"
  args(
      layout.projectDirectory
          .file("src/commonMain/resources/language/english-card-text-current.tsv")
          .asFile
          .absolutePath
  )
  outputs.upToDateWhen { false }
}
