package dev.martianzoo.codegen

import dev.martianzoo.tfm.canon.Canon
import java.nio.file.Path
import java.time.Instant

/** Supplies the complete Canon vocabulary to Gradle's generated-source compilation test. */
internal fun main(args: Array<String>) {
  val files =
      PetsTypeGenerator(
              Canon.classTable,
              "dev.martianzoo.codegen.canon",
              "CanonicalPets",
              generatedAt = Instant.EPOCH,
          )
          .generate()
  files.forEach { it.writeTo(Path.of(args.single())) }
}
