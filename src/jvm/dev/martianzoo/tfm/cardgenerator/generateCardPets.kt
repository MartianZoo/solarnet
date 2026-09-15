package dev.martianzoo.tfm.cardgenerator

import dev.martianzoo.tfm.carddata.CardData
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

public fun main(args: Array<String>) {
  require(args.size == 2) {
    "Usage: generateCardPets <output-directory> <authored-card-pets-directory>"
  }
  val outputDirectory = Path.of(args[0])
  val authoredCardPetsDirectory = Path.of(args[1])
  outputDirectory.toFile().deleteRecursively()
  CardData.bundleNames.sorted().forEach { bundleName ->
    val authoredPath = authoredCardPetsDirectory.resolve(bundleName).resolve("cards.pets")
    val authored = authoredPath.takeIf { it.exists() }?.readText()
    outputDirectory
        .resolve(bundleName)
        .also { it.createDirectories() }
        .resolve("cards.pets")
        .writeText(renderCardPets(bundleName, authored))
  }
}

internal fun renderCardPets(bundleName: String, authored: String?): String =
    listOfNotNull(
            CardPetsGenerator.renderBundle(bundleName).trimEnd(),
            authored?.trim()?.takeIf(String::isNotEmpty),
        )
        .joinToString("\n\n", postfix = "\n")
