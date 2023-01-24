plugins { id("org.jetbrains.kotlin.jvm") version "1.8.0" }

kotlin { jvmToolchain(18) }

dependencies {
  implementation(project(":pets"))

  implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0")

  // TODO will eventually deguavafy
  implementation("com.google.guava:guava:31.1-jre")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
  testImplementation("com.google.truth:truth:1.1.3")
}

sourceSets {
  val main by getting {
    resources {
      srcDir("src/main/kotlin")
      exclude("**/*.kt")
    }
  }
}
