plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("solarnet.kotlin-base")
}

kotlin {
  js {
    nodejs()
    binaries.executable()
    generateTypeScriptDefinitions()
  }

  sourceSets {
    jsMain {
      dependencies {
        implementation(project(":tfm-canon"))
        implementation(project(":engine"))
        implementation(project(":pets"))
        implementation(project(":tfm-engine"))
        implementation(libs.kotlinx.serialization.json)
      }
    }
    jsTest { dependencies { implementation(kotlin("test")) } }
  }
}
val typeScriptConsumerSource = layout.projectDirectory.file("src/consumerSmoke/typescript/main.ts")
val typeScriptConsumerOutput = rootProject.layout.buildDirectory.dir("js/parity-consumer-smoke")
val compiledTypeScriptConsumer = typeScriptConsumerOutput.map { it.file("main.js") }
val typeScriptConsumerPackage = typeScriptConsumerOutput.map {
  it.dir("node_modules/solarnet-parity")
}
val generatedParityDefinition = typeScriptConsumerPackage.map {
  it.file("kotlin/solarnet-parity.d.ts")
}

val prepareTypeScriptConsumerPackage =
    tasks.register<Sync>("prepareTypeScriptConsumerPackage") {
      dependsOn("compileDevelopmentExecutableKotlinJs", "jsProcessResources")
      from(layout.buildDirectory.dir("compileSync/js/main/developmentExecutable/kotlin")) {
        into("kotlin")
      }
      from(layout.buildDirectory.dir("processedResources/js/main")) { into("kotlin") }
      from(layout.projectDirectory.file("src/consumerSmoke/package.json"))
      into(typeScriptConsumerPackage)
    }

val compileTypeScriptConsumer =
    tasks.register<Exec>("compileTypeScriptConsumer") {
      group = "verification"
      description = "Compiles the external TypeScript parity-facade consumer."
      dependsOn(prepareTypeScriptConsumerPackage, ":kotlinNpmInstall")
      inputs.file(typeScriptConsumerSource)
      inputs.file(generatedParityDefinition)
      outputs.file(compiledTypeScriptConsumer)

      val compiler = rootProject.layout.buildDirectory.file("js/node_modules/typescript/lib/tsc.js")
      val moduleDirectory = typeScriptConsumerOutput.map { it.dir("node_modules") }
      commandLine(
          "node",
          compiler.get().asFile.absolutePath,
          "--baseUrl",
          moduleDirectory.get().asFile.absolutePath,
          "--module",
          "commonjs",
          "--moduleResolution",
          "node",
          "--noEmitOnError",
          "--outDir",
          typeScriptConsumerOutput.get().asFile.absolutePath,
          "--pretty",
          "false",
          "--strict",
          "--target",
          "ES2020",
          typeScriptConsumerSource.asFile.absolutePath,
      )
    }

tasks.register<Exec>("typescriptConsumerSmoke") {
  group = "verification"
  description = "Runs the TypeScript parity consumer and prints each move's new events."
  dependsOn(compileTypeScriptConsumer)
  inputs.file(compiledTypeScriptConsumer)
  commandLine("node", compiledTypeScriptConsumer.get().asFile.absolutePath)
}

tasks.named("check") { dependsOn("typescriptConsumerSmoke") }
