plugins {
  `maven-publish`
  id("org.jetbrains.kotlin.multiplatform")
  id("pets.kotlin-base")
}

val sourceDirectory =
    rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/tfm/web/almanac")

kotlin {
  js {
    browser { commonWebpackConfig { cssSupport { enabled.set(true) } } }
    binaries.executable()
  }

  sourceSets {
    jsMain {
      kotlin.setSrcDirs(listOf(sourceDirectory))
      dependencies {
        implementation(project(":catalog"))
        implementation(project(":pets"))
        implementation(project(":search"))
        implementation(project(":tfm-canon"))
        implementation(project(":text"))
        implementation(devNpm("tslib", "2.8.1"))
      }
    }
    jsTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/js/dev/martianzoo/tfm/web/almanac"))
      )
      dependencies { implementation(kotlin("test")) }
    }
  }
}

tasks.register("test") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs the Almanac browser test."
  dependsOn("jsBrowserTest")
}

tasks.named<ProcessResources>("jsProcessResources") {
  from(sourceDirectory) { include("*.html", "*.css", "favicon.png") }
}

val webZip by
    tasks.registering(Zip::class) {
      dependsOn("jsBrowserDistribution")
      archiveBaseName.set("almanac")
      from(layout.buildDirectory.dir("dist/js/productionExecutable"))
    }
val webDistribution by configurations.creating {
  isCanBeResolved = false
  isCanBeConsumed = true
  attributes { attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME)) }
  outgoing.artifact(webZip)
}

publishing {
  publications {
    create<MavenPublication>("web") { artifact(webZip) }
  }
}
