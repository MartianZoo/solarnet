plugins { id("solarnet.jvm") }

dependencies {
  implementation(project(":pets"))
  implementation(project(":canon"))
  testImplementation(libs.kotest.assertions.core)
}

tasks.register<JavaExec>("writeEnglishCardTextCurrent") {
  group = "verification"
  description = "Writes the English renderer's current canonical-card output snapshot."
  dependsOn("testClasses")
  classpath = sourceSets.test.get().runtimeClasspath
  mainClass = "dev.martianzoo.tfm.language.EnglishCardTextCurrentGenerator"
  args(
      layout.projectDirectory
          .file("src/main/resources/language/english-card-text-current.tsv")
          .asFile
          .absolutePath,
      layout.projectDirectory
          .file("src/main/resources/language/english-card-text-refusals.tsv")
          .asFile
          .absolutePath,
  )
  outputs.upToDateWhen { false }
}
