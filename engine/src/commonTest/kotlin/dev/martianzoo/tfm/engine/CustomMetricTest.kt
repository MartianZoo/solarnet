package dev.martianzoo.tfm.engine

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.CustomMetric
import dev.martianzoo.api.Exceptions.CustomCodeException
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.GameReader
import dev.martianzoo.data.GamePremise
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.types.Type
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CustomMetricTest {
  @Test
  fun marsRowIsCountedAsAMetricButNeverStoredAsAComponent() {
    val game = Engine.newGame(canonicalPremise(HellasMapOption, VenusNextExpansion, players = 2))
    val p1 = game.tfm(PLAYER1)
    val componentCount = p1.count("Component")

    p1.count("MarsRow<Hellas_8_4>") shouldBe 8
    game.reader.getComponents(p1.resolve("MarsRow<Hellas_8_4>")).isEmpty() shouldBe true
    p1.count("Component") shouldBe componentCount

    shouldThrow<ExpressionException> { p1.count("MarsRow<MaxwellBaseArea>") }
  }

  @Test
  fun marsRowWorksInsideARefinement() {
    val game = Engine.newGame(canonicalPremise(HellasMapOption, players = 2))
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
    shouldThrow<ExpressionException> { p1.godMode().sneak("BothBehavior") }
    p1.godMode().manual("BothBehavior")
    p1.count("Plant") shouldBe 1

    p1.count("SplitBehavior") shouldBe 9
    p1.godMode().manual("SplitBehavior")
    p1.count("Heat") shouldBe 1
  }

  @Test
  fun abstractArgumentsSumTheirConcreteSpecializations() {
    val p1 = Engine.newGame(customClassSetup()).tfm(PLAYER1)

    val invocationsBefore = ConcreteOnlyMetric.invocations
    p1.count("ConcreteOnlyMetric<Player1>") shouldBe 17
    ConcreteOnlyMetric.invocations shouldBe invocationsBefore + 1
    p1.count("ConcreteOnlyMetric<Player>") shouldBe 34
    ConcreteOnlyMetric.invocations shouldBe invocationsBefore + 3

    p1.count("ConcreteOnlyMetric<Player(HAS Plant)>") shouldBe 0
    ConcreteOnlyMetric.invocations shouldBe invocationsBefore + 3
    p1.godMode().sneak("Plant<Player1>")
    p1.count("ConcreteOnlyMetric<Player(HAS Plant)>") shouldBe 17
    ConcreteOnlyMetric.invocations shouldBe invocationsBefore + 4
  }

  @Test
  fun metricOnlyCustomClassesCannotBeUsedAsInstructionsOrComponents() {
    val p1 = Engine.newGame(customClassSetup()).tfm(PLAYER1)

    shouldThrow<ExpressionException> {
      p1.godMode().manual("ConcreteOnlyMetric<Player1>")
    }
    shouldThrow<ExpressionException> {
      p1.godMode().sneak("ConcreteOnlyMetric<Player1>")
    }
    shouldThrow<ExpressionException> {
      p1.godMode().sneak("-ConcreteOnlyMetric<Player1>")
    }
  }

  @Test
  fun changingACustomMetricDoesNotProduceAnEventForItsName() {
    val p1 = Engine.newGame(customClassSetup()).tfm(PLAYER1)

    p1.count("MetricTriggerObserver") shouldBe 1
    p1.count("PlantCount<Player1>") shouldBe 0
    p1.godMode().sneak("Plant<Player1>")
    p1.count("PlantCount<Player1>") shouldBe 1
    p1.count("Heat<Player1>") shouldBe 0
  }

  @Test
  fun customImplementationRuntimeFailuresHaveTheirOwnDomain() {
    val p1 = Engine.newGame(customClassSetup()).tfm(PLAYER1)

    shouldThrow<CustomCodeException> { p1.count("BrokenMetric") }
    shouldThrow<CustomCodeException> { p1.godMode().manual("BrokenInstruction") }
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

private object ConcreteOnlyMetric : CustomMetric() {
  var invocations = 0
    private set

  override fun count(game: GameReader, type: Type): Int {
    invocations++
    return 17
  }
}

private object PlantCount : CustomMetric() {
  override fun count(game: GameReader, type: Type): Int {
    val player = type.expressionFull.arguments.single()
    return game.count(game.resolve(parse("Plant<$player>")))
  }
}

private object BrokenMetric : CustomMetric() {
  override fun count(game: GameReader, type: Type): Int = error("broken metric")
}

private object BrokenInstruction : CustomClass() {
  override fun translate(game: GameReader): Instruction = error("broken instruction")
}

private object CustomClassDeclarations : TfmAuthority() {
  override val explicitClassDeclarations =
      parseClasses(
              """
              CLASS BothBehavior : Custom, AutoLoad
              CLASS SplitBehavior : Custom, AutoLoad
              CLASS ConcreteOnlyMetric<Player> : Custom, AutoLoad
              CLASS PlantCount<Player> : Custom
              CLASS BrokenMetric : Custom, AutoLoad
              CLASS BrokenInstruction : Custom, AutoLoad
              CLASS MetricTriggerObserver : AutoLoad {
                HAS =1 This
                PlantCount<Player1>: Heat<Player1>
              }
              """
                  .trimIndent()
          )
          .toSet()

  override val customClasses: Set<CustomClass> =
      setOf(
          BothBehavior,
          SplitInstructionImplementation.SplitBehavior,
          SplitMetricImplementation.SplitBehavior,
          ConcreteOnlyMetric,
          PlantCount,
          BrokenMetric,
          BrokenInstruction,
      )
}

private fun customClassSetup(): GamePremise =
    canonicalPremise(
        authority = TfmAuthority.compose(Canon, CustomClassDeclarations),
    )
