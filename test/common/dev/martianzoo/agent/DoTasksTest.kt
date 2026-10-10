package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class DoTasksTest {
  @Test
  internal fun executesTheWholeAtomizedAmountChosenFromAnAlternative() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Rating : Atomized { This: Coin }
                CLASS Coin
                CLASS Heat
                """
            )
        )
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    player.addTasks("2 Rating OR 2 Heat")

    player.doTasks("2 Rating")

    player.count("Rating") shouldBe 2
    player.count("Heat") shouldBe 0
    player.count("Coin") shouldBe 0
    player.tasks.ids().size shouldBe 2
  }

  @Test
  internal fun combinesResolvedAmountsAndPreservesTheirContinuations() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Coin
                CLASS Marker
                CLASS First
                CLASS Second
                """
            )
        )
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    player.runOperation("2 Marker")
    player.addTasks("Coin / Marker THEN First, 3 Coin THEN Second")

    player.doTasks("5 Coin", "First", "Second")

    player.count("Coin") shouldBe 5
    player.count("First") shouldBe 1
    player.count("Second") shouldBe 1
    game.isIdle() shouldBe true
  }

  @Test
  internal fun combinesDifferentScalarsAndPreservesBothContinuations() {
    val game = Engine.newGame(testGamePremise("CLASS Coin\nCLASS First\nCLASS Second"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    player.addTasks("2 Coin THEN First, 3 Coin THEN Second")

    player.doTasks("5 Coin", "First", "Second")

    player.count("Coin") shouldBe 5
    player.count("First") shouldBe 1
    player.count("Second") shouldBe 1
    game.isIdle() shouldBe true
  }

  @Test
  internal fun combinesAtomizedGainsWithoutIncludingNewlyTriggeredTasks() {
    val game = Engine.newGame(testGamePremise("CLASS Rating : Atomized { This: Rating }"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    player.addTasks("3 Rating")

    player.doTasks("3 Rating")

    player.count("Rating") shouldBe 3
    player.tasks.ids().size shouldBe 3
  }

  @Test
  internal fun prefersAnExistingTaskOverTheSumOfOtherTasks() {
    val game = Engine.newGame(testGamePremise("CLASS Coin"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    val siblings = player.addTasks("Coin, 2 Coin")
    player.addTasks("3 Coin")

    player.continueOperation { doTasks("3 Coin") }

    player.count("Coin") shouldBe 3
    player.tasks.ids() shouldBe siblings.toSet()
  }

  @Test
  internal fun rejectsAPartialSumWithoutChoosingASubset() {
    val game = Engine.newGame(testGamePremise("CLASS Coin"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    val tasks = player.addTasks("Coin, 2 Coin, 4 Coin")

    shouldThrow<TaskException> { player.continueOperation { doTasks("3 Coin") } }

    player.count("Coin") shouldBe 0
    player.tasks.ids() shouldBe tasks.toSet()
  }

  @Test
  internal fun doesNotCombineOtherTypesOrOtherActorsTasks() {
    val game = Engine.newGame(testGamePremise("CLASS Coin\nCLASS Plant", players = 2))
    val agents = Agents(game)
    val player = agents[PLAYER1].also { it.autoExecPolicy = NONE }
    val other = agents[PLAYER2].also { it.autoExecPolicy = NONE }
    val plants = player.addTasks("3 Plant")
    player.addTasks("Coin, 2 Coin")
    val otherCoins = other.addTasks("4 Coin")

    player.continueOperation { doTasks("3 Coin") }

    player.count("Coin") shouldBe 3
    player.count("Plant") shouldBe 0
    player.tasks.ids() shouldBe plants.toSet()
    other.tasks.ids() shouldBe otherCoins.toSet()
  }

  @Test
  internal fun combinesRemovals() {
    val game = Engine.newGame(testGamePremise("CLASS Coin"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    player.runOperation("5 Coin")
    player.addTasks("-2 Coin!, -3 Coin!")

    player.completeOperation { doTasks("-5 Coin") }

    player.count("Coin") shouldBe 0
    game.isIdle() shouldBe true
  }

  @Test
  internal fun combinesNetGainWithAnEarlierRemovalWithoutRequiringStartingStock() {
    val game = Engine.newGame(testGamePremise("CLASS Coin"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    player.addTasks("-5 Coin!, 6 Coin!, 35 Coin!")

    player.doTasks("36 Coin")

    player.count("Coin") shouldBe 36
    game.isIdle() shouldBe true
  }

  @Test
  internal fun doesNotUseASameDirectionSubsetWhenOtherChangesArePending() {
    val game = Engine.newGame(testGamePremise("CLASS Coin"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    val tasks = player.addTasks("-4 Coin!, 2 Coin!, 2 Coin!")

    shouldThrow<TaskException> { player.continueOperation { doTasks("4 Coin") } }

    player.count("Coin") shouldBe 0
    player.tasks.ids() shouldBe tasks.toSet()
  }

  @Test
  internal fun usesTheWholeNetTotalEvenWhenAnIndividualTaskHasThatAmount() {
    val game = Engine.newGame(testGamePremise("CLASS Coin"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    player.addTasks("-5 Coin!, 27 Coin!, 5 Coin!")

    player.doTasks("27 Coin")

    player.count("Coin") shouldBe 27
    game.isIdle() shouldBe true
  }

  @Test
  internal fun rollsBackEverySummedTaskIfALaterTaskFails() {
    val game = Engine.newGame(testGamePremise("CLASS Coin { HAS MAX 3 This }"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    player.addTasks("2 Coin!, 2 Coin!")
    val before = game.timeline.checkpoint()

    shouldThrow<LimitsException> { player.continueOperation { doTasks("4 Coin") } }

    game.timeline.checkpoint() shouldBe before
    player.count("Coin") shouldBe 0
  }

  @Test
  internal fun doesNotFallBackAfterAMatchingTaskFailsExecution() {
    val game = Engine.newGame(testGamePremise("CLASS Coin { HAS MAX 1 This }"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    player.addTasks("2 Coin!, Coin!, Coin!")
    val before = game.timeline.checkpoint()

    shouldThrow<LimitsException> { player.continueOperation { doTasks("2 Coin") } }

    game.timeline.checkpoint() shouldBe before
    player.count("Coin") shouldBe 0
  }

  @Test
  internal fun doesNotResolveAnAmbiguousMatchByCombiningIt() {
    val game = Engine.newGame(testGamePremise("CLASS Coin"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    player.addTasks("2 Coin!, 2 Coin?")
    val before = game.timeline.checkpoint()

    shouldThrow<TaskException> { player.continueOperation { doTasks("2 Coin") } }

    game.timeline.checkpoint() shouldBe before
    player.count("Coin") shouldBe 0
  }

  @Test
  internal fun doesNotCombineTasksWithDifferentQuantifiers() {
    val game = Engine.newGame(testGamePremise("CLASS Coin"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    player.addTasks("2 Coin!, 3 Coin?")
    val before = game.timeline.checkpoint()

    shouldThrow<TaskException> { player.continueOperation { doTasks("5 Coin") } }

    game.timeline.checkpoint() shouldBe before
    player.count("Coin") shouldBe 0
  }

  @Test
  internal fun rollsBackEarlierInstructionsWhenABatchFails() {
    val game = Engine.newGame(testGamePremise("CLASS Coin\nCLASS Plant"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    player.addTasks("Coin, 2 Plant")
    val before = game.timeline.checkpoint()

    shouldThrow<TaskException> { player.doTasks("Coin", "3 Plant") }

    game.timeline.checkpoint() shouldBe before
    player.count("Coin") shouldBe 0
  }
}
