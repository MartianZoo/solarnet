package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestOption.HellasMapOption
import dev.martianzoo.tfm.engine.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PropertyTest {
  @Test
  fun numberPropertiesAreReadableWithoutBecomingComponents() {
    val game = Engine.newGame(canonicalPremise(HellasMapOption, VenusNextExpansion, players = 2))
    val p1 = game.tfm(PLAYER1)
    val componentCount = p1.count("Component")

    p1.count("Hellas_8_4.row") shouldBe 8
    p1.count("Hellas_8_4.column") shouldBe 4
    p1.count("Card001.cost") shouldBe 8
    p1.count("Class<Card001>.cost") shouldBe 8
    p1.count("PowerPlantSP.cost") shouldBe 11
    p1.count("Component") shouldBe componentCount

    shouldThrow<ExpressionException> { p1.count("MaxwellBaseArea.row") }
    shouldThrow<ExpressionException> { p1.count("CardFront.cost") }
    shouldThrow<ExpressionException> { p1.count("cost") }
    shouldThrow<PetSyntaxException> { p1.count("cost<Card001>") }
  }

  @Test
  fun numberPropertiesWorkInsideARefinement() {
    val game = Engine.newGame(canonicalPremise(HellasMapOption, players = 2))
    val p1 = game.tfm(PLAYER1)

    p1.godMode()
        .sneak(
            "CityTile<Player1, Hellas_7_4>, CityTile<Player1, Hellas_8_4>, " +
                "CityTile<Player1, Hellas_8_5>, CityTile<Player1, Hellas_9_5>"
        )

    p1.count("OwnedTile<MarsArea(HAS 8 row)>") shouldBe 3
    p1.has("3 OwnedTile<MarsArea(HAS 8 row)>") shouldBe true
    p1.has("4 OwnedTile<MarsArea(HAS 8 row)>") shouldBe false
    p1.godMode().manual("PolarExplorer")
    p1.count("PolarExplorer") shouldBe 1
  }

  @Test
  fun metricPropertiesAreEvaluatedExplicitlyInsideClassEffects() {
    val authority = TfmAuthority.Composite(Canon, MetricPropertyProbeAuthority)
    val game = Engine.newGame(canonicalPremise(authority = authority, players = 2))
    val p1 = game.tfm(PLAYER1).godMode()

    p1.manual("3 TemperatureStep!, MetricPropertyProbe")

    p1.count("MetricPropertyResult") shouldBe 3
    p1.count("FixedMetricPropertyResult") shouldBe 8
    p1.count("MetricPropertyProbe.fixedScore") shouldBe 8
    shouldThrow<ExpressionException> { p1.count("MetricPropertyProbe.score") }
    shouldThrow<PetSyntaxException> { p1.count("EVAL MetricPropertyProbe.score") }
  }
}

private object MetricPropertyProbeAuthority : TfmAuthority() {
  override val explicitClassDeclarations =
      parseClasses(
              """
              ABSTRACT CLASS MetricPropertyHolder {
                score = Metric
                fixedScore = Metric
              }
              CLASS MetricPropertyResult
              CLASS FixedMetricPropertyResult
              CLASS MetricPropertyProbe : MetricPropertyHolder {
                score = COUNT TemperatureStep
                fixedScore = 8
                This:: MetricPropertyResult / EVAL This.score
                This:: FixedMetricPropertyResult / EVAL This.fixedScore
              }
              """
                  .trimIndent()
          )
          .toSet()
}
