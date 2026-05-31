import java.net.URL

plugins {
  id("org.jetbrains.kotlin.jvm") version "2.1.20"
  id("org.jetbrains.dokka") version "1.9.20"
}

kotlin { jvmToolchain(21) }

dependencies {
  implementation(project(":pets"))

  implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.1.20"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.20")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
  testImplementation("com.google.truth:truth:1.1.3")

  testImplementation(project(mapOf("path" to ":engine")))
  testImplementation(project(mapOf("path" to ":repl")))
}

sourceSets {
  val main by getting {
    resources {
      srcDir("src/main/java")
      exclude("**/*.kt")
    }
  }
}

tasks.dokkaHtml.configure {
  dokkaSourceSets {
    configureEach {
      sourceLink {
        localDirectory.set(file("src"))
        remoteUrl.set(URL("https://github.com/MartianZoo/solarnet/tree/main/canon/src"))
        remoteLineSuffix.set("#L")
      }
      samples.from("src/main/java/dev/martianzoo/tfm/canon/samples.kt")
    }
  }
}
