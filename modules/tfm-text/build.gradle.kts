plugins { id("solarnet.jvm") }

val textSourceDirectory = rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/tfm/text")

kotlin {
  sourceSets {
    main { kotlin.setSrcDirs(listOf(textSourceDirectory)) }
    test {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/tfm/text"))
      )
    }
  }
}

dependencies {
  implementation(project(":pets"))
  implementation(project(":tfm-canon"))
  testImplementation(libs.kotest.assertions.core)
}

tasks.named<ProcessResources>("processResources") {
  from(textSourceDirectory) {
    include("*.tsv")
    into("language")
  }
}

tasks.register<JavaExec>("writeEnglishCardTextCurrent") {
  group = "verification"
  description = "Writes the English renderer's current canonical-card output snapshot."
  dependsOn("testClasses")
  classpath = sourceSets.test.get().runtimeClasspath
  mainClass = "dev.martianzoo.tfm.text.EnglishCardTextCurrentGenerator"
  args(
      textSourceDirectory.file("english-card-text-current.tsv").asFile.absolutePath,
      textSourceDirectory.file("english-card-text-refusals.tsv").asFile.absolutePath,
  )
  outputs.upToDateWhen { false }
}
