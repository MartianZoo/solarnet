package dev.martianzoo.engine

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TaskDelegationTest {
  @Test
  internal fun `a concrete reaction stays with its controller and credits its owner`() {
    val game = game()
    val p1 = game.agent(PLAYER1).also { it.autoExecMode = NONE }
    val engine = game.agent(ENGINE)
    engine.manual("Observer")

    p1.beginManual("ConcreteReactor<Player2>") {
      val reaction = game.tasks.extract { it }.single()
      reaction.assignee shouldBe PLAYER1
      reaction.controller shouldBe PLAYER1
      reaction.actor shouldBe PLAYER2

      p1.selectTask(reaction.id)

      val followUp = game.tasks.extract { it }.single()
      followUp.assignee shouldBe PLAYER1
      followUp.controller shouldBe PLAYER1
      followUp.actor shouldBe PLAYER2
      p1.selectTask(followUp.id)
    }

    p1.count("Wrong") shouldBe 0
    p1.count("FollowUp") shouldBe 1
  }

  @Test
  internal fun `selecting an abstract reaction hands it to its owner and retains control`() {
    val game = game()
    val p1 = game.agent(PLAYER1).also { it.autoExecMode = NONE }
    val p2 = game.agent(PLAYER2).also { it.autoExecMode = NONE }

    p1.beginManual("AbstractReactor<Player2>") {
      p1.addTasks("Spare<Player1>?, Later<Player1>?")
      val reward =
          game.tasks.extract { it }.single { it.instruction.toString().startsWith("Reward") }
      reward.assignee shouldBe PLAYER1
      reward.controller shouldBe PLAYER1
      reward.actor shouldBe PLAYER2

      p1.doTask("Spare<Player1>")
      p1.selectTask(reward.id)

      val delegated = game.tasks.getTaskData(reward.id)
      delegated.assignee shouldBe PLAYER2
      delegated.controller shouldBe PLAYER1
      delegated.selected shouldBe true
      shouldThrow<TaskException> { p1.doTask("Later<Player1>") }

      p2.doTask("RewardA<Player2>")

      val followUp = game.tasks.extract { it }.single { it.instruction.toString() == "FollowUp!" }
      followUp.assignee shouldBe PLAYER1
      followUp.controller shouldBe PLAYER1
      followUp.actor shouldBe PLAYER2
      p1.selectTask(followUp.id)
      p1.doTask("Later<Player1>")
    }

    p2.count("RewardA") shouldBe 1
    p1.count("FollowUp") shouldBe 1
    p1.count("Spare") shouldBe 1
    p1.count("Later") shouldBe 1
  }

  private fun game() =
      Engine.newGame(
          testGamePremise(
              """
              CLASS Result : Owned<Player>
              CLASS FollowUp
              CLASS Wrong
              CLASS Spare : Owned<Player>
              CLASS Later : Owned<Player>
              ABSTRACT CLASS Reward : Owned<Player> {
                CLASS RewardA
                CLASS RewardB
              }
              CLASS ConcreteReactor : Owned<Player> {
                This: Result<Owner>
              }
              CLASS AbstractReactor : Owned<Player> {
                This: Reward<Owner>
                Reward<Owner>: FollowUp
              }
              CLASS Observer {
                Result<Player2> BY Player1: Wrong
                Result<Player2> BY Player2: FollowUp
              }
              """,
              players = 2,
          )
      )
}
