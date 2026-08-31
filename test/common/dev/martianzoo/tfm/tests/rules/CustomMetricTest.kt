package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.CustomMetric
import dev.martianzoo.pets.api.Exceptions.CustomCodeException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.types.Type
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CustomMetricTest {
  @Test
  internal fun instructionAndMetricCapabilitiesCanShareOrSplitImplementations() {
    val game = Engine.newGame(customClassSetup())
    val p1 = game.tfm(PLAYER1)

    p1.count("BothBehavior") shouldBe 7
    shouldThrow<ExpressionException> { p1.sneak("BothBehavior") }
    p1.manual("BothBehavior")
    p1.count("Plant") shouldBe 1

    p1.count("SplitBehavior") shouldBe 9
    p1.manual("SplitBehavior")
    p1.count("Heat") shouldBe 1
    p1.count("Plant") shouldBe 2
  }

  @Test
  internal fun abstractArgumentsSumTheirConcreteSpecializations() {
    val p1 = Engine.newGame(customClassSetup()).tfm(PLAYER1)

    val invocationsBefore = ConcreteOnlyMetric.invocations
    p1.count("ConcreteOnlyMetric<Player1>") shouldBe 17
    ConcreteOnlyMetric.invocations shouldBe invocationsBefore + 1
    p1.count("ConcreteOnlyMetric<Player>") shouldBe 34
    ConcreteOnlyMetric.invocations shouldBe invocationsBefore + 3

    p1.count("ConcreteOnlyMetric<Player(HAS Plant)>") shouldBe 0
    ConcreteOnlyMetric.invocations shouldBe invocationsBefore + 3
    p1.sneak("Plant<Player1>")
    p1.count("ConcreteOnlyMetric<Player(HAS Plant)>") shouldBe 17
    ConcreteOnlyMetric.invocations shouldBe invocationsBefore + 4
  }

  @Test
  internal fun metricOnlyCustomClassesCannotBeUsedAsInstructionsOrComponents() {
    val p1 = Engine.newGame(customClassSetup()).tfm(PLAYER1)

    shouldThrow<ExpressionException> { p1.manual("ConcreteOnlyMetric<Player1>") }
    shouldThrow<ExpressionException> { p1.sneak("ConcreteOnlyMetric<Player1>") }
    shouldThrow<ExpressionException> { p1.sneak("-ConcreteOnlyMetric<Player1>") }
  }

  @Test
  internal fun changingACustomMetricDoesNotProduceAnEventForItsName() {
    val p1 = Engine.newGame(customClassSetup()).tfm(PLAYER1)

    p1.count("MetricTriggerObserver") shouldBe 1
    p1.count("PlantCount<Player1>") shouldBe 0
    p1.sneak("Plant<Player1>")
    p1.count("PlantCount<Player1>") shouldBe 1
    p1.count("Heat<Player1>") shouldBe 0
  }

  @Test
  internal fun customImplementationRuntimeFailuresHaveTheirOwnDomain() {
    val p1 = Engine.newGame(customClassSetup()).tfm(PLAYER1)

    shouldThrow<CustomCodeException> { p1.count("BrokenMetric") }
    shouldThrow<CustomCodeException> { p1.manual("BrokenInstruction") }
  }
}

private object BothBehavior : CustomMetric() {
  override fun translate(game: GameReader): Instruction = parse("Plant<Player1>")

  override fun count(game: GameReader, type: Type): Int = 7
}

private object SplitInstructionImplementation {
  object SplitBehavior : CustomClass() {
    override fun translate(game: GameReader): InstructionGroup =
        InstructionGroup(listOf(parse("Heat<Player1>"), parse("Plant<Player1>")))
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
  override fun translate(game: GameReader): InstructionTree = error("broken instruction")
}

private object CustomClassDeclarations : TfmCatalog() {
  override val explicitClassDeclarations =
      parseClasses(
              """
              CLASS BothBehavior : Custom
              CLASS SplitBehavior : Custom
              CLASS ConcreteOnlyMetric<Player> : Custom
              CLASS PlantCount<Player> : Custom
              CLASS BrokenMetric : Custom
              CLASS BrokenInstruction : Custom
              CLASS MetricTriggerObserver {
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
        catalog = TfmCatalog.compose(Canon, CustomClassDeclarations),
    )
