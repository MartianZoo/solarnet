plugins { id("solarnet.kmp-jvm-js") }

val textSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/text")
val coloniesEnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/bundles/colonies/english"
    )
val corporateEraEnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/bundles/corpera/english"
    )
val prelude1EnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/bundles/prelude/english"
    )
val prelude2EnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/bundles/prelude2/english"
    )
val promoEnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/bundles/promos/english"
    )
val turmoilEnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/bundles/turmoil/english"
    )
val vastitasEnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/bundles/vastitas/english"
    )
val venusNextEnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/bundles/venus/english"
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
              corporateEraEnglishSourceDirectory,
              prelude1EnglishSourceDirectory,
              prelude2EnglishSourceDirectory,
              promoEnglishSourceDirectory,
              turmoilEnglishSourceDirectory,
              vastitasEnglishSourceDirectory,
              venusNextEnglishSourceDirectory,
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
      dependencies {
        implementation(project(":catalog"))
        implementation(project(":state"))
        implementation(project(":tfm-fake"))
        implementation(libs.kotest.assertions.core)
      }
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
  description = "Writes the English renderer's current published-card output snapshot."
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

tasks.register<JavaExec>("writeEnglishTextComparisons") {
  group = "documentation"
  description = "Writes printed, generated, and Pets wording comparisons for each category."
  dependsOn(jvmTestCompilation.compileTaskProvider)
  classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
  mainClass = "dev.martianzoo.tfm.text.EnglishTextComparisonGenerator"
  args(
      rootProject.layout.projectDirectory
          .dir("docs/english-wording-comparisons")
          .asFile
          .absolutePath
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
