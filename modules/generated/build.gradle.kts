plugins { id("solarnet.kmp-jvm-js") }

val generatePetsTypes = project(":codegen").tasks.named("generatePetsTypes")
val generatedTestDirectory =
    rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/generated")

kotlin {
  sourceSets {
    commonMain {
      kotlin.srcDir(generatePetsTypes)
      dependencies { api(project(":pets")) }
    }
    commonTest { kotlin.setSrcDirs(listOf(generatedTestDirectory)) }
  }
}
