plugins { id("solarnet.kmp-jvm-js") }

val commonSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/viewer")
val commonTestDirectory =
    rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/viewer")
val jsSourceDirectory = rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/viewer")
val webSourceDirectory = rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/web")

kotlin {
  js {
    browser { commonWebpackConfig { cssSupport { enabled.set(true) } } }
    binaries.executable()
  }

  sourceSets {
    commonMain {
      kotlin.setSrcDirs(listOf(commonSourceDirectory))
      dependencies {
        implementation(project(":engine"))
        implementation(project(":pets"))
        implementation(project(":tfm-canon"))
        implementation(project(":tfm-engine"))
      }
    }
    commonTest { kotlin.setSrcDirs(listOf(commonTestDirectory)) }
    jsMain {
      kotlin.setSrcDirs(listOf(jsSourceDirectory))
      dependencies { implementation(devNpm("tslib", "2.8.1")) }
    }
  }
}

tasks.named<ProcessResources>("jsProcessResources") {
  from(jsSourceDirectory) { include("*.html", "*.css") }
  from(webSourceDirectory.dir("assets")) { into("assets") }
  val localImages =
      providers
          .gradleProperty("localImagesDir")
          .map(rootProject::file)
          .orElse(rootProject.layout.projectDirectory.dir("_local/images").asFile)
  from(localImages) { into("images") }
}

// The game viewer's development server is the shared browser-app host. Its webpack configuration
// also bundles and serves the browser REPL, so make that application available before webpack runs.
tasks.named("jsBrowserDevelopmentRun") {
  dependsOn(":web:jsDevelopmentExecutableCompileSync", ":web:jsProcessResources")
}
