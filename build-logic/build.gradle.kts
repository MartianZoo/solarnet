import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins { `kotlin-dsl` }

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
  implementation(libs.detekt.gradle.plugin)
  implementation(libs.dokka.gradle.plugin)
  implementation(libs.kotlin.jvm.gradle.plugin)
  implementation(libs.kotlin.multiplatform.gradle.plugin)
}
