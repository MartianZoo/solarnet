plugins {
  id("solarnet.jvm")
}

val codegenSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/codegen")
val codegenTestDirectory =
    rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/codegen")

kotlin {
  sourceSets {
    main { kotlin.setSrcDirs(listOf(codegenSourceDirectory)) }
    test { kotlin.setSrcDirs(listOf(codegenTestDirectory)) }
  }
}

dependencies {
  implementation(project(":pets"))
  implementation(libs.kotlinpoet)

  testImplementation(kotlin("test-junit5"))
  testRuntimeOnly(libs.junit.platform.launcher)
}
