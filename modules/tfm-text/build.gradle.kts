plugins { id("solarnet.kmp-jvm-js") }

val textSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/text")
val coloniesEnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/ColoniesExpansion/english"
    )
val corporateEraEnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/CorporateEraExpansion/english"
    )
val prelude1EnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/Prelude1CardPack/english"
    )
val prelude2EnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/Prelude2CardPack/english"
    )
val preludeCommonEnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/PreludeCommon/english"
    )
val promoEnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/PromoCardPack/english"
    )
val turmoilCardPackEnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/TurmoilCardPack/english"
    )
val turmoilEnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/TurmoilExpansion/english"
    )
val vastitasEnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/VastitasMap/english"
    )
val venusNextEnglishSourceDirectory =
    rootProject.layout.projectDirectory.dir(
        "src/common/dev/martianzoo/tfm/canon/VenusNextExpansion/english"
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
              preludeCommonEnglishSourceDirectory,
              promoEnglishSourceDirectory,
              turmoilCardPackEnglishSourceDirectory,
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
