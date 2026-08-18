plugins { id("solarnet.kmp-jvm-js") }

kotlin {
  sourceSets {
    commonMain { dependencies { implementation(project(":pets")) } }
    commonTest { dependencies { implementation(libs.kotest.assertions.core) } }
  }
}

tasks.named<Copy>("copyResourcesForKarma") {
  dependsOn("jsProcessResources")
  from(layout.buildDirectory.dir("processedResources/js/main"))
}
