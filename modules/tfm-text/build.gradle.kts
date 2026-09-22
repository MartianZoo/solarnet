plugins { id("solarnet.kmp-jvm-js") }

val textSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/text")
val coloniesEnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/ColoniesExpansion/english"
    )
val turmoilEnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/TurmoilExpansion/english"
    )
val textDataDirectory = rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/tfm/text")
val randomCardInput = providers.gradleProperty("randomCardInput")
val randomCardEnglishOutput = providers.gradleProperty("randomCardEnglishOutput")
val randomCardEnglishComparisonOutput =
    providers.gradleProperty("randomCardEnglishComparisonOutput")

kotlin {
  sourceSets {
    commonMain {
      kotlin.setSrcDirs(
          listOf(
              textSourceDirectory,
              coloniesEnglishSourceDirectory,
              turmoilEnglishSourceDirectory,
          )
      )
      dependencies {
        implementation(project(":pets"))
        implementation(project(":tfm-canon"))
      }
    }
    jvmTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/tfm/text"))
      )
      dependencies { implementation(libs.kotest.assertions.core) }
    }
  }
}

tasks.named<ProcessResources>("jvmProcessResources") {
  from(textDataDirectory) {
    include("*.tsv")
    into("language")
  }
}

val jvmTestCompilation = kotlin.targets.getByName("jvm").compilations.getByName("test")

tasks.register<JavaExec>("writeEnglishCardTextCurrent") {
  group = "verification"
  description = "Writes the English renderer's current canonical-card output snapshot."
  dependsOn(jvmTestCompilation.compileTaskProvider)
  classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
  mainClass = "dev.martianzoo.tfm.text.EnglishCardTextCurrentGenerator"
  args(
      textDataDirectory.file("english-card-text-current.tsv").asFile.absolutePath,
      textDataDirectory.file("english-card-text-refusals.tsv").asFile.absolutePath,
  )
  outputs.upToDateWhen { false }
}

tasks.register<JavaExec>("writeEnglishGoalTextCurrent") {
  group = "verification"
  description = "Writes the English renderer's current milestone and award output snapshot."
  dependsOn(jvmTestCompilation.compileTaskProvider)
  classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
  mainClass = "dev.martianzoo.tfm.text.EnglishGoalTextCurrentGenerator"
  args(
      textDataDirectory.file("english-goal-text-current.tsv").asFile.absolutePath,
      textDataDirectory.file("english-goal-text-refusals.tsv").asFile.absolutePath,
  )
  outputs.upToDateWhen { false }
}

tasks.register<JavaExec>("writeRandomCardEnglishText") {
  group = "verification"
  description = "Writes top and bottom English text for a saved random-card PETS report."
  dependsOn(jvmTestCompilation.compileTaskProvider)
  classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
  mainClass = "dev.martianzoo.tfm.text.EnglishRandomCardTextGenerator"
  args(randomCardInput.getOrElse(""), randomCardEnglishOutput.getOrElse(""))
  randomCardEnglishComparisonOutput.orNull?.let { args(it) }
  outputs.upToDateWhen { false }
}
