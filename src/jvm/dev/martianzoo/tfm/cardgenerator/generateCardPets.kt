package dev.martianzoo.tfm.cardgenerator

import dev.martianzoo.tfm.carddata.CardData
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

public fun main(args: Array<String>) {
  require(args.size == 1) { "Usage: generateCardPets <output-directory>" }
  val outputDirectory = Path.of(args.single())
  outputDirectory.toFile().deleteRecursively()
  CardData.bundleNames.sorted().forEach { bundleName ->
    outputDirectory
        .resolve(bundleName)
        .also { it.createDirectories() }
        .resolve("cards.pets")
        .writeText(CardPetsGenerator.renderBundle(bundleName))
  }
}
