package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.PetException
import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.*
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmAuthority
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.Hellas
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class PropertyTest {
  @Test
  internal fun numberPropertiesAreReadableWithoutBecomingComponents() {
    val game = Engine.newGame(canonicalPremise(Hellas, VenusNextExpansion, players = 2))
    val p1 = game.tfm(PLAYER1)
    val componentCount = p1.count("Component")

    p1.count("Hellas_8_4.row") shouldBe 8
    p1.count("Hellas_8_4.column") shouldBe 4
    p1.count("ColonizerTrainingCamp.cost") shouldBe 8
    p1.count("Class<ColonizerTrainingCamp>.cost") shouldBe 8
    p1.count("PowerPlantSP.cost") shouldBe 11
    p1.count("Component") shouldBe componentCount

    shouldThrow<ExpressionException> { p1.count("MaxwellBaseArea.row") }
    shouldThrow<ExpressionException> { p1.count("CardFront.cost") }
    shouldThrow<ExpressionException> { p1.count("cost") }
    shouldThrow<PetSyntaxException> { p1.count("cost<ColonizerTrainingCamp>") }
  }

  @Test
  internal fun numberPropertiesWorkInsideARefinement() {
    val game = Engine.newGame(canonicalPremise(Hellas, players = 2))
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
  internal fun metricPropertiesAreEvaluatedExplicitlyInsideClassEffects() {
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

  @Test
  internal fun requirementPropertiesAreEvaluatedAfterTheirEffectReceiverBecomesConcrete() {
    val authority = TfmAuthority.Composite(Canon, RequirementPropertyProbeAuthority)
    val game = Engine.newGame(canonicalPremise(authority = authority, players = 2))
    val p1 = game.tfm(PLAYER1).godMode()
    val p2 = game.tfm(PLAYER2).godMode()

    p1.manual("RequirementPropertyMarker")
    p1.manual("OptionalRequirementPropertyProbe") {
      doTask("RequirementPropertyStarted<Player1>")
      doTask("RequirementPropertyPassed<Player1>")
      doTask("RequirementPropertyFinished<Player1>")
    }
    p1.manual("RequiredRequirementPropertyProbe") {
      doTask("RequirementPropertyStarted<Player1>")
      doTask("RequirementPropertyPassed<Player1>")
      doTask("RequirementPropertyFinished<Player1>")
    }
    p2.manual("OptionalRequirementPropertyProbe") {
      doTask("RequirementPropertyStarted<Player2>")
      doTask("RequirementPropertyPassed<Player2>")
      doTask("RequirementPropertyFinished<Player2>")
    }
    p2.autoExecMode = NONE
    p2.manual("RequiredRequirementPropertyProbe") {
      doTask("RequirementPropertyStarted<Player2>")
      val gatedResult =
          tasks
              .extract { it }
              .single {
                "RequirementPropertyPassed<Player2>" in it.instruction.toString()
              }
      gatedResult.instruction.toString() shouldBe
          "(RequiredRequirementPropertyProbe<Player2>, RequirementPropertyMarker<Player2>): RequirementPropertyPassed<Player2>?"
      p2.dropTask(gatedResult.id)
      doTask("RequirementPropertyFinished<Player2>")
    }

    p1.count("RequirementPropertyStarted") shouldBe 2
    p1.count("RequirementPropertyPassed") shouldBe 2
    p1.count("RequirementPropertyFinished") shouldBe 2
    p2.count("RequirementPropertyStarted") shouldBe 2
    p2.count("RequirementPropertyPassed") shouldBe 1
    p2.count("RequirementPropertyFinished") shouldBe 2

    shouldThrow<PetException> { p1.manual("RecursiveRequirementPropertyProbe") }
        .message shouldContain "is recursive"
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
                score = COUNT "TemperatureStep"
                fixedScore = 8
                This:: MetricPropertyResult / EVAL This.score
                This:: FixedMetricPropertyResult / EVAL This.fixedScore
              }
              """
                  .trimIndent()
          )
          .toSet()
}

private object RequirementPropertyProbeAuthority : TfmAuthority() {
  override val explicitClassDeclarations =
      parseClasses(
              """
              ABSTRACT CLASS RequirementPropertyProbe : Owned<Player> {
                requirement = Requirement?
                This: RequirementPropertyStarted<Owner>? THEN (EVAL This.requirement: RequirementPropertyPassed<Owner>?, RequirementPropertyFinished<Owner>?)
              }
              CLASS OptionalRequirementPropertyProbe : RequirementPropertyProbe
              CLASS RequiredRequirementPropertyProbe : RequirementPropertyProbe {
                requirement = HAS "This, RequirementPropertyMarker<Owner>"
              }
              CLASS RecursiveRequirementPropertyProbe : RequirementPropertyProbe {
                requirement = HAS "EVAL This.requirement"
              }
              CLASS RequirementPropertyMarker : Owned<Player>
              CLASS RequirementPropertyStarted : Owned<Player>
              CLASS RequirementPropertyPassed : Owned<Player>
              CLASS RequirementPropertyFinished : Owned<Player>
              """
                  .trimIndent()
          )
          .toSet()
}
