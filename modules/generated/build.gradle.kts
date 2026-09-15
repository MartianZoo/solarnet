plugins { id("solarnet.kmp-jvm-js") }

val generatePetsTypes = project(":codegen").tasks.named("generatePetsTypes")
val generatedSupportDirectory =
    rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/generated")
val generatedTestDirectory =
    rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/generated")

kotlin {
  sourceSets {
    commonMain {
      kotlin.srcDir(generatePetsTypes)
      kotlin.srcDir(generatedSupportDirectory)
      dependencies { api(project(":pets")) }
    }
    commonTest { kotlin.setSrcDirs(listOf(generatedTestDirectory)) }
  }
}
