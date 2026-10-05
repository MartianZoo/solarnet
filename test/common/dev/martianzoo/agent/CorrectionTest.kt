package dev.martianzoo.agent

import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CorrectionTest {
  @Test
  internal fun correctionsCheckUnrelatedExistingViolationsWithoutRepairingThem() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This, =1 Badge<This> }
                CLASS Card : Holder
                CLASS Badge<Holder>
                CLASS Token
                """
                    .trimIndent()
            )
        )
    val player = Agents(game)[PLAYER1]
    val created = player.sneak("Card")
    // Deliberately stop history inside construction to exercise a complete World audit.
    game.timeline.rollBack(Checkpoint(created.changes.last().ordinal))
    player.count("Card") shouldBe 1
    player.count("Badge") shouldBe 0
    val before = game.timeline.checkpoint()

    shouldThrow<LimitsException> { player.sneak("Token") }

    game.timeline.checkpoint() shouldBe before
    player.count("Token") shouldBe 0
    player.count("Badge") shouldBe 0
  }

  @Test
  internal fun failedExMachinaRestoresTheSelectedTaskAndItsHistory() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Token { HAS MAX 1 This }
                ABSTRACT CLASS Choice
                CLASS First : Choice
                CLASS Second : Choice
                """
                    .trimIndent()
            )
        )
    val agents = Agents(game)
    val player = agents[PLAYER1].also { it.autoExecPolicy = AutoExecPolicy.NONE }
    val taskId = player.addTasks("Choice!").single()
    player.selectTask(taskId)
    val selected = player.tasks.getTaskData(taskId)
    val before = game.timeline.checkpoint()

    shouldThrow<LimitsException> { agents.exMachina(PLAYER1, "2 Token") }

    game.timeline.checkpoint() shouldBe before
    player.tasks.getTaskData(taskId) shouldBe selected
    player.autoExecPolicy shouldBe AutoExecPolicy.NONE
    player.doTask("First")
    player.count("First") shouldBe 1
  }

  @Test
  internal fun correctionsRejectMaximumViolationsAndRollBackTheWholeBatch() {
    val game = Engine.newGame(testGamePremise("CLASS Token { HAS MAX 1 This }\nCLASS Reward"))
    val agents = Agents(game)
    val player = agents[PLAYER1]
    player.sneak("Token")
    val before = game.timeline.checkpoint()

    shouldThrow<LimitsException> { player.sneak("Reward, Token") }
    game.timeline.checkpoint() shouldBe before
    player.count("Token") shouldBe 1
    player.count("Reward") shouldBe 0

    shouldThrow<LimitsException> { agents.exMachina(PLAYER1, "Token") }
    game.timeline.checkpoint() shouldBe before
  }

  @Test
  internal fun correctionsConstructPartsAndRunAutomaticEffectsButSuppressQueuedEffects() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This, =1 Badge<This> }
                CLASS Card : Holder { This:: Reward }
                CLASS Badge<Holder> {
                  HAS MAX 1 This, =2 Detail<This>
                  This: Reward
                  -This:: Reward
                }
                CLASS Detail<Badge> { This:: Reward }
                CLASS Reward { This:: Echo; This: Skipped }
                CLASS Echo { This: Skipped }
                CLASS Skipped
                """
                    .trimIndent()
            )
        )
    val player = Agents(game)[PLAYER1]

    player.sneak("Card")
    player.count("Badge<Card>") shouldBe 1
    player.count("Detail<Badge<Card>>") shouldBe 2
    player.count("Reward") shouldBe 3
    player.count("Echo") shouldBe 3
    player.count("Skipped") shouldBe 0
    game.tasks.isEmpty() shouldBe true
    val before = game.timeline.checkpoint()

    shouldThrow<LimitsException> { player.sneak("-Detail<Badge<Card>>") }
    shouldThrow<LimitsException> { player.sneak("-Badge<Card>") }
    shouldThrow<LimitsException> { player.sneak("-Detail<Badge<Card>>, Card FROM Card") }
    game.timeline.checkpoint() shouldBe before
    player.count("Detail<Badge<Card>>") shouldBe 2

    player.sneak("-Card")
    player.count("Holder") shouldBe 0
    player.count("Badge") shouldBe 0
    player.count("Detail") shouldBe 0
    player.count("Reward") shouldBe 4
    player.count("Echo") shouldBe 4
    player.count("Skipped") shouldBe 0
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun correctionsKeepDependencyOrderingAndReactBeforeOwnerDisposal() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This }
                CLASS Card : Holder {
                  HAS =1 Branch<This>, =1 Bracket<This>
                  This:: -This!
                }
                CLASS Branch<Holder> { HAS MAX 1 This, =1 Mounted<This, Bracket<Card>> }
                CLASS Bracket<Holder> { HAS MAX 1 This }
                CLASS Mounted<Branch, Bracket> {
                  HAS MAX 1 This
                  This:: Reward<This>
                }
                CLASS Reward<Mounted> { This:: Observed; This: Skipped }
                CLASS Observed
                CLASS Skipped
                """
                    .trimIndent()
            )
        )
    val player = Agents(game)[PLAYER1]

    player.sneak("Card")

    player.count("Card") shouldBe 0
    player.count("Mounted") shouldBe 0
    player.count("Reward") shouldBe 0
    player.count("Observed") shouldBe 1
    player.count("Skipped") shouldBe 0
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun exMachinaRejectsDirectInfrastructureEditsButAllowsDerivedParts() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This, =1 Part<This> }
                CLASS Card : Holder
                CLASS Part<Holder> : Hidden
                CLASS Rule : System
                CLASS Token
                """
                    .trimIndent()
            )
        )
    val agents = Agents(game)
    val player = agents[PLAYER1]
    agents.exMachina(PLAYER1, "Card")
    player.count("Part<Card>") shouldBe 1
    val before = game.timeline.checkpoint()

    shouldThrow<ExpressionException> { agents.exMachina(PLAYER1, "Token, Rule") }
    shouldThrow<ExpressionException> { agents.exMachina(PLAYER1, "-Part<Card>") }
    shouldThrow<ExpressionException> { agents.exMachina(PLAYER1, "Token FROM Part<Card>") }
    game.timeline.checkpoint() shouldBe before
    player.count("Token") shouldBe 0

    agents.exMachina(PLAYER1, "-Card")
    player.count("Part") shouldBe 0
  }

  @Test
  internal fun exMachinaRejectsPartsThatWouldNeedGameplayToFinish() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This, =1 Work<This> }
                CLASS Start : Holder
                CLASS Work<Holder> : Barrier { This: -This! }
                """
                    .trimIndent()
            )
        )
    val agents = Agents(game)
    val player = agents[PLAYER1]
    val before = game.timeline.checkpoint()

    shouldThrow<NotNowException> { agents.exMachina(PLAYER1, "Start") }

    player.count("Start") shouldBe 0
    player.count("Work") shouldBe 0
    game.timeline.checkpoint() shouldBe before
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun correctionsValidateAbsentRequiredTypesWithoutResurrectingThem() {
    val game =
        Engine.newGame(
            testGamePremise(
                    """
                    CLASS Required { HAS =1 This }
                    CLASS Token
                    """
                        .trimIndent()
                )
                .copy(initialComponentTypes = setOf(parse("Required")))
        )
    val player = Agents(game)[PLAYER1]
    val before = game.timeline.checkpoint()

    shouldThrow<LimitsException> { player.sneak("-Required, Token") }

    game.timeline.checkpoint() shouldBe before
    player.count("Required") shouldBe 1
    player.count("Token") shouldBe 0
  }

  @Test
  internal fun correctionsMayReplaceARequiredFamilyWithinOneBatch() {
    val game =
        Engine.newGame(
            testGamePremise(
                    """
                    ABSTRACT CLASS Status { HAS =1 Status }
                    CLASS Before : Status
                    CLASS After : Status
                    """
                        .trimIndent()
                )
                .copy(initialComponentTypes = setOf(parse("Before")))
        )
    val player = Agents(game)[PLAYER1]

    player.sneak("-Before, After")

    player.count("Before") shouldBe 0
    player.count("After") shouldBe 1
  }

  @Test
  internal fun correctionsDoNotRunIdleCleanupOrCompletionCallbacks() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Brief : Temporary {
                  This:: GainReward
                  -This:: RemovalReward
                }
                CLASS GainReward
                CLASS RemovalReward
                """
                    .trimIndent()
            )
        )
    val player = Agents(game)[PLAYER1]
    var callbacks = 0
    game.onTransactionComplete = { callbacks++ }

    player.sneak("Brief")

    player.count("Brief") shouldBe 1
    player.count("GainReward") shouldBe 1
    player.count("RemovalReward") shouldBe 0
    callbacks shouldBe 0
  }
}
