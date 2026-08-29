plugins { id("solarnet.kmp-jvm-js") }

kotlin {
  js {
    browser { commonWebpackConfig { cssSupport { enabled.set(true) } } }
    binaries.executable()
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(project(":engine"))
        implementation(project(":pets"))
        implementation(project(":tfm-canon"))
        implementation(project(":tfm-engine"))
      }
    }
    jsMain { dependencies { implementation(devNpm("tslib", "2.8.1")) } }
  }
}

tasks.named<ProcessResources>("jsProcessResources") {
  dependsOn(":tfm-canon:jsProcessResources", ":pets:jsProcessResources")
  from(project(":tfm-canon").layout.buildDirectory.dir("processedResources/js/main"))
  from(project(":pets").layout.buildDirectory.dir("processedResources/js/main/pets")) {
    into("pets")
  }
  from(project(":web").layout.projectDirectory.dir("src/jsMain/resources/assets")) {
    into("assets")
  }
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
