package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Vocabulary.Companion.petsClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VocabularyTest {
  @Test
  fun petsNamesUseOneCamelCaseDerivation() {
    petsClassName("XML HTTP request") shouldBe cn("XmlHttpRequest")
    petsClassName("supports IPv6 on iOS?") shouldBe cn("SupportsIpv6OnIos")
    petsClassName("YouTube importer") shouldBe cn("YouTubeImporter")
    petsClassName("AdWords account") shouldBe cn("AdWordsAccount")
    petsClassName("CEO's Favorite Project") shouldBe cn("CeosFavoriteProject")
    petsClassName("UNMI Contractor") shouldBe cn("UnmiContractor")
    petsClassName("PolderTECH Dutch") shouldBe cn("PolderTechDutch")
    petsClassName("Asteroid (Card)") shouldBe cn("AsteroidCard")
  }

  @Test
  fun requestedLocaleFallsBackToEnglishPerEntry() {
    val vocabulary =
        Vocabulary.create(
            setOf(cn("Card001"), cn("Card002")),
            mapOf(
                "en" to mapOf(cn("Card001") to "First Card", cn("Card002") to "Second Card"),
                "fr" to mapOf(cn("Card001") to "Premiere carte"),
            ),
            locale = "fr-CA",
        )

    vocabulary.displayName(cn("Card001")) shouldBe "Premiere carte"
    vocabulary.displayName(cn("Card002")) shouldBe "Second Card"
  }

  @Test
  fun localizedNamesAreAcceptedAndRenderedForTheirContext() {
    val vocabulary =
        Vocabulary.create(
            setOf(cn("Card072")),
            mapOf("fr" to mapOf(cn("Card072") to "Oiseaux d'ete")),
            locale = "fr",
        )

    vocabulary.canonicalName(cn("OiseauxDete")) shouldBe cn("Card072")
    vocabulary.displayName(cn("Card072")) shouldBe "Oiseaux d'ete"
    vocabulary.renderPets(parse<Expression>("Card072")) shouldBe "OiseauxDete"
  }

  @Test
  fun collisionsAreRejected() {
    shouldThrow<IllegalArgumentException> {
      Vocabulary.create(
          setOf(cn("Card001"), cn("Card002")),
          mapOf(
              "en" to
                  mapOf(
                      cn("Card001") to "Same name",
                      cn("Card002") to "Same-name",
                  )
          ),
      )
    }
  }

  @Test
  fun nonAsciiDisplayNamesAreRejected() {
    shouldThrow<IllegalArgumentException> {
      Vocabulary.create(
          setOf(cn("Card001")),
          mapOf("en" to mapOf(cn("Card001") to "Premi\u00e8re carte")),
      )
    }
  }

  @Test
  fun duplicateInputSynonymsAreRejected() {
    shouldThrow<IllegalArgumentException> {
      ClassSynonyms.of(listOf("T" to "Titanium", "T" to "Temperature"))
    }
  }

  @Test
  fun inputOnlySynonymsAreAcceptedButNeverRendered() {
    val vocabulary =
        Vocabulary.create(
            setOf(cn("Titanium"), cn("TerraformRating")),
            emptyMap(),
            derivedPetsNameClassNames = emptySet(),
            inputOnlySynonyms = ClassSynonyms.of("T" to "Titanium", "TR" to "TerraformRating"),
        )

    vocabulary.canonicalName(cn("T")) shouldBe cn("Titanium")
    vocabulary.canonicalName(cn("TR")) shouldBe cn("TerraformRating")
    vocabulary.renderPets(parse<Expression>("Titanium")) shouldBe "Titanium"
    vocabulary.renderPets(parse<Expression>("TerraformRating")) shouldBe "TerraformRating"
  }
}
