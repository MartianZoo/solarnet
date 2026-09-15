plugins { id("solarnet.kmp-jvm-js") }

val commonSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/web/gameviewer")
val commonTestDirectory =
    rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/tfm/web/gameviewer")
val jsSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/tfm/web/gameviewer")
val sharedSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/tfm/web/shared")

kotlin {
  js {
    browser { commonWebpackConfig { cssSupport { enabled.set(true) } } }
    binaries.executable()
  }

  sourceSets {
    commonMain {
      kotlin.setSrcDirs(listOf(commonSourceDirectory))
      dependencies {
        implementation(project(":pets"))
        implementation(project(":state"))
        implementation(project(":tfm-canon"))
        implementation(project(":tfm-fake"))
      }
    }
    commonTest {
      kotlin.setSrcDirs(listOf(commonTestDirectory))
      dependencies {
        implementation(project(":agent"))
        implementation(project(":engine"))
        implementation(project(":tfm-engine"))
      }
    }
    jsMain {
      kotlin.setSrcDirs(listOf(jsSourceDirectory))
      dependencies { implementation(devNpm("tslib", "2.8.1")) }
    }
  }
}

val replayEventLogsDirectory =
    project(":tfm-tests").layout.buildDirectory.dir("generated/replay-event-logs")

tasks.named<ProcessResources>("jsProcessResources") {
  mustRunAfter(":tfm-tests:jvmTest")
  from(jsSourceDirectory) { include("*.html", "*.css") }
  from(sharedSourceDirectory) { into("assets") }
  from(replayEventLogsDirectory) {
    include("*.json")
    into("games")
  }
  val localImages =
      providers
          .gradleProperty("localImagesDir")
          .map(rootProject::file)
          .orElse(rootProject.layout.projectDirectory.dir("_local/images").asFile)
  from(localImages) {
    include("*.png", "MC/*.png")
    into("images")
  }
  doLast {
    val gamesDirectory = destinationDir.resolve("games").also { it.mkdirs() }
    val names =
        gamesDirectory
            .listFiles { file -> file.isFile && file.extension == "json" }
            .orEmpty()
            .map { it.nameWithoutExtension }
            .sorted()
    gamesDirectory.resolve("index.txt").writeText(names.joinToString("\n", postfix = "\n"))
  }
}

// The game viewer's development server is the shared browser-app host. Its webpack configuration
// also bundles and serves the browser REPL, so make that application available before webpack runs.
tasks.named("jsBrowserDevelopmentRun") {
  dependsOn(":web:jsDevelopmentExecutableCompileSync", ":web:jsProcessResources")
}
