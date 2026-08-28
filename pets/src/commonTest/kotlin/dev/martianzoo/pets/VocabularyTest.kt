package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Vocabulary.Companion.defaultEnglishDisplayName
import dev.martianzoo.pets.Vocabulary.Companion.petsClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VocabularyTest {
  @Test
  internal fun englishDisplayNamesSeparateClassNameWords() {
    defaultEnglishDisplayName(cn("ColonizerTrainingCamp")) shouldBe "Colonizer Training Camp"
    defaultEnglishDisplayName(cn("BeamFromAThoriumAsteroid")) shouldBe "Beam From AThorium Asteroid"
    defaultEnglishDisplayName(cn("Builder7")) shouldBe "Builder 7"
    defaultEnglishDisplayName(cn("NaturalPreserve_SpecialTile")) shouldBe
        "Natural Preserve Special Tile"
    defaultEnglishDisplayName(cn("UseCardActionSA")) shouldBe "Use Card Action SA"

    val vocabulary = Vocabulary.create(setOf(cn("ColonizerTrainingCamp")), emptyMap())
    vocabulary.displayName(cn("ColonizerTrainingCamp")) shouldBe "Colonizer Training Camp"
  }

  @Test
  internal fun petsNamesUseOneCamelCaseDerivation() {
    petsClassName("XML HTTP request") shouldBe cn("XmlHttpRequest")
    petsClassName("supports IPv6 on iOS?") shouldBe cn("SupportsIpv6OnIos")
    petsClassName("YouTube importer") shouldBe cn("YouTubeImporter")
    petsClassName("AdWords account") shouldBe cn("AdWordsAccount")
    petsClassName("CEO's Favorite Project") shouldBe cn("CeosFavoriteProject")
    petsClassName("UNMI Contractor") shouldBe cn("UnmiContractor")
    petsClassName("PolderTECH Dutch") shouldBe cn("PolderTechDutch")
    petsClassName("Asteroid (Card)") shouldBe cn("AsteroidCard")
    petsClassName("L1 Trade Terminal") shouldBe cn("L_1TradeTerminal")
  }

  @Test
  internal fun requestedLocaleFallsBackToEnglishPerEntry() {
    val vocabulary =
        Vocabulary.create(
            setOf(cn("ColonizerTrainingCamp"), cn("AsteroidMiningConsortium")),
            mapOf(
                "en" to
                    mapOf(
                        cn("ColonizerTrainingCamp") to "First Card",
                        cn("AsteroidMiningConsortium") to "Second Card",
                    ),
                "fr" to mapOf(cn("ColonizerTrainingCamp") to "Premiere carte"),
            ),
            locale = "fr-CA",
        )

    vocabulary.displayName(cn("ColonizerTrainingCamp")) shouldBe "Premiere carte"
    vocabulary.displayName(cn("AsteroidMiningConsortium")) shouldBe "Second Card"
  }

  @Test
  internal fun localizedNamesAreAcceptedAndRenderedForTheirContext() {
    val vocabulary =
        Vocabulary.create(
            setOf(cn("Birds")),
            mapOf("fr" to mapOf(cn("Birds") to "Oiseaux d'ete")),
            locale = "fr",
        )

    vocabulary.canonicalName(cn("OiseauxDete")) shouldBe cn("Birds")
    vocabulary.displayName(cn("Birds")) shouldBe "Oiseaux d'ete"
    vocabulary.renderPets(parse<Expression>("Birds")) shouldBe "OiseauxDete"
  }

  @Test
  internal fun explicitPetsNamesAreAliasesForCanonicalClasses() {
    val vocabulary =
        Vocabulary.create(
            setOf(cn("Player1")),
            emptyMap(),
            derivedPetsNameClassNames = emptySet(),
            petsNameAliases = mapOf(cn("Player1") to cn("Mom")),
        )

    vocabulary.canonicalName(cn("Mom")) shouldBe cn("Player1")
    vocabulary.displayName(cn("Player1")) shouldBe "Mom"
    vocabulary.renderPets(parse<Expression>("Player1")) shouldBe "Mom"
  }

  @Test
  internal fun localizedNameCollisionsAreRejected() {
    shouldThrow<IllegalArgumentException> {
      Vocabulary.create(
          setOf(cn("ColonizerTrainingCamp"), cn("AsteroidMiningConsortium")),
          mapOf(
              "fr" to
                  mapOf(
                      cn("ColonizerTrainingCamp") to "Same name",
                      cn("AsteroidMiningConsortium") to "Same-name",
                  )
          ),
          locale = "fr",
      )
    }
  }

  @Test
  internal fun nonAsciiDisplayNamesAreRejected() {
    shouldThrow<IllegalArgumentException> {
      Vocabulary.create(
          setOf(cn("ColonizerTrainingCamp")),
          mapOf("en" to mapOf(cn("ColonizerTrainingCamp") to "Premi\u00e8re carte")),
      )
    }
  }

  @Test
  internal fun duplicateInputSynonymsAreRejected() {
    shouldThrow<IllegalArgumentException> {
      Vocabulary.create(
          setOf(cn("Titanium"), cn("Temperature")),
          emptyMap(),
          derivedPetsNameClassNames = emptySet(),
          inputOnlySynonyms = listOf("T" to "Titanium", "T" to "Temperature"),
      )
    }
  }

  @Test
  internal fun inputOnlySynonymsAreAcceptedButNeverRendered() {
    val vocabulary =
        Vocabulary.create(
            setOf(cn("Titanium"), cn("TerraformRating")),
            emptyMap(),
            derivedPetsNameClassNames = emptySet(),
            inputOnlySynonyms = listOf("T" to "Titanium", "TR" to "TerraformRating"),
        )

    vocabulary.canonicalName(cn("T")) shouldBe cn("Titanium")
    vocabulary.canonicalName(cn("TR")) shouldBe cn("TerraformRating")
    vocabulary.renderPets(parse<Expression>("Titanium")) shouldBe "Titanium"
    vocabulary.renderPets(parse<Expression>("TerraformRating")) shouldBe "TerraformRating"
  }
}
