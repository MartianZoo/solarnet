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

internal class ReleasedAmountsTest {
  @Test
  fun combinesDirectAndReleasedAmountsWithoutExecutingNewRewards() {
    val premise =
        testGamePremise(
            """
        CLASS Coin : Owned<Player> { This: Bonus }
        CLASS Bonus : Owned<Player>
        """,
            players = 2,
        )
    val agents = Agents(Engine.newGame(premise)) { NONE }
    val sender = agents[PLAYER1]
    val recipient = agents[PLAYER2]
    sender.addTasks("NOW $PLAYER2 { 2 Coin<$PLAYER2> }, NOW $PLAYER2 { 2 Coin<$PLAYER2> }")
    recipient.addTasks("Coin")
    recipient.doTask("5 Coin", releasedBy = sender)
    recipient.count("Coin") shouldBe 5
    recipient.count("Bonus") shouldBe 0
    sender.tasks.isEmpty() shouldBe true
    recipient.doTask("5 Bonus")
    agents.world.isIdle() shouldBe true
  }

  @Test
  fun rejectsAnIncorrectTotalWithoutSearchingSubsetsOrSpendingTasks() {
    val agents =
        Agents(Engine.newGame(testGamePremise("CLASS Coin : Owned<Player>", players = 2))) { NONE }
    val sender = agents[PLAYER1]
    val recipient = agents[PLAYER2]
    sender.addTasks(
        "NOW $PLAYER2 { 2 Coin<$PLAYER2> }, NOW $PLAYER2 { 2 Coin<$PLAYER2> }, NOW $PLAYER2 { Coin<$PLAYER2> }"
    )
    shouldThrow<TaskException> { recipient.doTask("4 Coin", releasedBy = sender) }
    recipient.count("Coin") shouldBe 0
    sender.tasks.ids().size shouldBe 3
    recipient.doTask("5 Coin", releasedBy = sender)
    recipient.count("Coin") shouldBe 5
  }

  @Test
  fun rollsBackEarlierReleasesWhenALaterGainFails() {
    val premise = testGamePremise("CLASS Coin : Owned<Player> { HAS MAX 3 Coin }", players = 2)
    val agents = Agents(Engine.newGame(premise)) { NONE }
    val sender = agents[PLAYER1]
    val recipient = agents[PLAYER2]
    sender.addTasks("NOW $PLAYER2 { 2 Coin<$PLAYER2> }, NOW $PLAYER2 { 2 Coin<$PLAYER2> }")
    shouldThrow<LimitsException> { recipient.doTask("4 Coin", releasedBy = sender) }
    recipient.count("Coin") shouldBe 0
    sender.tasks.ids().size shouldBe 2
    recipient.tasks.isEmpty() shouldBe true
  }

  @Test
  fun executesDirectWorkWithoutReleasingAnUnrelatedNowTask() {
    val premise = testGamePremise("CLASS Coin\nCLASS Plant", players = 2)
    val agents = Agents(Engine.newGame(premise)) { NONE }
    val sender = agents[PLAYER1]
    val recipient = agents[PLAYER2]
    sender.addTasks("NOW $PLAYER2 { 2 Coin }")
    recipient.addTasks("Plant")

    recipient.doTask("Plant", releasedBy = sender)
    recipient.count("Plant") shouldBe 1
    recipient.count("Coin") shouldBe 0
    sender.tasks.ids().size shouldBe 1

    recipient.doTask("2 Coin", releasedBy = sender)
    recipient.count("Coin") shouldBe 2
    agents.world.isIdle() shouldBe true
  }

  @Test
  fun rejectsAnAmbiguousRequestBeforeReleasingAnything() {
    val premise = testGamePremise("CLASS Coin\nCLASS Plant", players = 2)
    val agents = Agents(Engine.newGame(premise)) { NONE }
    val sender = agents[PLAYER1]
    val recipient = agents[PLAYER2]
    sender.addTasks("NOW $PLAYER2 { Plant THEN Coin }")
    recipient.addTasks("Plant")
    val before = agents.world.events.nextOrdinal

    val failure = shouldThrow<TaskException> { recipient.doTask("Plant", releasedBy = sender) }
    failure.message.orEmpty().contains("ambiguous") shouldBe true
    agents.world.events.nextOrdinal shouldBe before
    recipient.count("Plant") shouldBe 0
    sender.tasks.ids().size shouldBe 1
    recipient.tasks.ids().size shouldBe 1
  }

  @Test
  fun releasesALinkedTaskWithoutExecutingItsLaterStage() {
    val agents =
        Agents(
            Engine.newGame(
                testGamePremise(
                    "CLASS Coin : Owned<Player>\nCLASS Marker : Owned<Player>",
                    players = 2,
                )
            )
        ) {
          NONE
        }
    val sender = agents[PLAYER1]
    val recipient = agents[PLAYER2]
    sender.addTasks("NOW $PLAYER2 { Coin<$PLAYER2> THEN Marker<$PLAYER2> }")
    val before = agents.world.events.nextOrdinal
    shouldThrow<TaskException> { recipient.doTask("Marker", releasedBy = sender) }
    agents.world.events.nextOrdinal shouldBe before
    sender.tasks.ids().size shouldBe 1
    recipient.doTask("Coin", releasedBy = sender)
    recipient.count("Coin") shouldBe 1
    recipient.count("Marker") shouldBe 0
    recipient.doTask("Marker", releasedBy = sender)
    agents.world.isIdle() shouldBe true
  }

  @Test
  fun totalsIncludeEligibleLaterTasksButWaitForOrdinaryWork() {
    val agents = Agents(Engine.newGame(testGamePremise("CLASS Coin\nCLASS Marker"))) { NONE }
    val player = agents[PLAYER1]
    player.addTasks("Marker?, LATER { 2 Coin, 3 Coin }, FINALLY { Coin }")
    shouldThrow<TaskException> { player.doTask("5 Coin") }
    player.doTask("Ok")
    player.doTask("5 Coin")
    player.count("Coin") shouldBe 5
    player.doTask("Coin")
    player.count("Coin") shouldBe 6
    agents.world.isIdle() shouldBe true
  }

  @Test
  fun totalsFinishZeroValuedWorkToo() {
    val agents = Agents(Engine.newGame(testGamePremise("CLASS Coin\nCLASS Marker"))) { NONE }
    val player = agents[PLAYER1]
    player.addTasks("3 Coin, Coin / Marker")
    player.doTask("3 Coin")
    player.count("Coin") shouldBe 3
    agents.world.isIdle() shouldBe true
  }
}
