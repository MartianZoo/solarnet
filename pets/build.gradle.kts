plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
}

dependencies {
    implementation("com.github.h0tk3y.betterParse:better-parse:0.4.4")
    implementation("com.squareup.moshi:moshi-kotlin:1.12.0")
}

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  implementation("com.google.guava:guava:31.1-jre")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
  testImplementation("com.google.truth:truth:1.0")
}

sourceSets {
  val main by getting {
    resources {
      srcDir("src/main/kotlin")
      exclude("**/*.kt")

    }
  }
}