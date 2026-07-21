package dev.martianzoo.tfm.engine

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.CustomMetric
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.Type
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CustomMetricTest {
  @Test
  fun marsRowIsCountedAsAMetricButNeverStoredAsAComponent() {
    val game = Engine.newGame(Canon.fromOptionCodes("BHV", 2))
    val p1 = game.tfm(PLAYER1)
    val componentCount = p1.count("Component")

    p1.count("MarsRow<Hellas_8_4>") shouldBe 8
    game.reader.getComponents(p1.resolve("MarsRow<Hellas_8_4>")).isEmpty() shouldBe true
    p1.count("Component") shouldBe componentCount

    shouldThrow<ExpressionException> { p1.count("MarsRow<MaxwellBaseArea>") }
  }

  @Test
  fun marsRowWorksInsideARefinement() {
    val game = Engine.newGame(Canon.fromOptionCodes("BH", 2))
    val p1 = game.tfm(PLAYER1)

    p1.godMode()
        .sneak(
            "CityTile<Player1, Hellas_7_4>, CityTile<Player1, Hellas_8_4>, " +
                "CityTile<Player1, Hellas_8_5>, CityTile<Player1, Hellas_9_5>"
        )

    p1.count("OwnedTile<MarsArea(HAS 8 MarsRow)>") shouldBe 3
    p1.has("3 OwnedTile<MarsArea(HAS 8 MarsRow)>") shouldBe true
    p1.has("4 OwnedTile<MarsArea(HAS 8 MarsRow)>") shouldBe false
    p1.godMode().manual("PolarExplorer")
    p1.count("PolarExplorer") shouldBe 1
  }

  @Test
  fun instructionAndMetricCapabilitiesCanShareOrSplitImplementations() {
    val game = Engine.newGame(customClassSetup())
    val p1 = game.tfm(PLAYER1)

    p1.count("BothBehavior") shouldBe 7
    shouldThrow<IllegalArgumentException> { p1.godMode().sneak("BothBehavior") }
    p1.godMode().manual("BothBehavior")
    p1.count("Plant") shouldBe 1

    p1.count("SplitBehavior") shouldBe 9
    p1.godMode().manual("SplitBehavior")
    p1.count("Heat") shouldBe 1

    p1.count("AbstractAwareMetric<Component>") shouldBe 13
  }
}

private object BothBehavior : CustomMetric() {
  override fun translate(game: GameReader): Instruction = parse("Plant<Player1>")

  override fun count(game: GameReader, type: Type): Int = 7
}

private object SplitInstructionImplementation {
  object SplitBehavior : CustomClass() {
    override fun translate(game: GameReader): Instruction = parse("Heat<Player1>")
  }
}

private object SplitMetricImplementation {
  object SplitBehavior : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int = 9
  }
}

private object AbstractAwareMetric : CustomMetric() {
  override fun count(game: GameReader, type: Type): Int =
      if (game.resolve(type.expressionFull.arguments.single()).abstract) 13 else 17
}

private object CustomClassDeclarations : TfmRuleset.Empty() {
  override val explicitClassDeclarations =
      parseClasses(
              """
              CLASS BothBehavior : Custom, AutoLoad
              CLASS SplitBehavior : Custom, AutoLoad
              CLASS AbstractAwareMetric<Component> : Custom, AutoLoad
              """
                  .trimIndent()
          )
          .toSet()

  override val customClasses: Set<CustomClass> =
      setOf(
          BothBehavior,
          SplitInstructionImplementation.SplitBehavior,
          SplitMetricImplementation.SplitBehavior,
          AbstractAwareMetric,
      )
}

private fun customClassSetup(): GameSetup {
  val base = Canon.fromOptionCodes("BMR", 2)
  return GameSetup(TfmRuleset.compose(base.ruleset, CustomClassDeclarations), base.options)
}
