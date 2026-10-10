package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TaskDelegationTest {
  @Test
  internal fun `a concrete reaction passes from its controller to its executing owner`() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN)
    admin.runOperation("Observer")

    p1.beginOperation("ConcreteReactor<Player2>") {
      val reaction = game.tasks.extract { it }.single()
      reaction.assignee shouldBe PLAYER1
      reaction.controller shouldBe PLAYER1
      reaction.selectionAssignee shouldBe PLAYER2

      p1.canExecuteTask(reaction.id) shouldBe false
      p1.tryTask(reaction.id)
      game.tasks.getTaskData(reaction.id) shouldBe reaction

      p1.selectTask(reaction.id)

      val delegated = game.tasks.getTaskData(reaction.id)
      delegated.assignee shouldBe PLAYER2
      delegated.selected shouldBe true
      p2.selectTask(reaction.id)

      val followUp = game.tasks.extract { it }.single()
      followUp.assignee shouldBe PLAYER1
      followUp.controller shouldBe PLAYER1
      followUp.selectionAssignee shouldBe PLAYER2
      p1.selectTask(followUp.id)
      p2.selectTask(followUp.id)
    }

    p1.count("Wrong") shouldBe 0
    p1.count("FollowUp") shouldBe 1
  }

  @Test
  internal fun `selecting an abstract reaction hands it to its owner and retains control`() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }

    p1.beginOperation("AbstractReactor<Player2>") {
      p1.addTasks("Spare<Player1>?, Later<Player1>?")
      val reward =
          game.tasks.extract { it }.single { it.instruction.toString().startsWith("Reward") }
      reward.assignee shouldBe PLAYER1
      reward.controller shouldBe PLAYER1
      reward.selectionAssignee shouldBe PLAYER2

      p1.doTask("Spare<Player1>")
      shouldThrow<TaskException> { p1.doTask("RewardA<Player2>") }
      game.tasks.getTaskData(reward.id) shouldBe reward
      p1.selectTask(reward.id)

      val delegated = game.tasks.getTaskData(reward.id)
      delegated.assignee shouldBe PLAYER2
      delegated.controller shouldBe PLAYER1
      delegated.selected shouldBe true
      p2.count("RewardA") shouldBe 0
      shouldThrow<TaskException> { p1.doTask("Later<Player1>") }

      p2.doTask("RewardA<Player2>")

      val followUp = game.tasks.extract { it }.single { it.instruction.toString() == "FollowUp!" }
      followUp.assignee shouldBe PLAYER1
      followUp.controller shouldBe PLAYER1
      followUp.selectionAssignee shouldBe PLAYER2
      p1.selectTask(followUp.id)
      p2.selectTask(followUp.id)
      p1.doTask("Later<Player1>")
    }

    p2.count("RewardA") shouldBe 1
    p1.count("FollowUp") shouldBe 1
    p1.count("Spare") shouldBe 1
    p1.count("Later") shouldBe 1
  }

  @Test
  internal fun `a controller form cannot narrow through an assignment handoff`() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }

    p1.beginOperation("AbstractReactor<Player2>") {
      val reward = game.tasks.extract { it }.single()
      shouldThrow<TaskException> { p1.tryTask("RewardA<Player2>", reward.id) }
      game.tasks.getTaskData(reward.id) shouldBe reward

      val form = p1.fillInTask(reward.id)
      form.narrow("RewardA<Player2>")
      shouldThrow<TaskException> { form.commit() }
      game.tasks.getTaskData(reward.id) shouldBe reward

      p1.fillInTask(reward.id).commit()

      game.tasks.getTaskData(reward.id).let {
        it.assignee shouldBe PLAYER2
        it.instruction.toString() shouldBe "Reward<Player2>!"
      }
      p2.doTask("RewardA<Player2>")

      val followUp = game.tasks.extract { it }.single()
      p1.selectTask(followUp.id)
      p2.selectTask(followUp.id)
    }

    p2.count("RewardA") shouldBe 1
  }

  @Test
  internal fun `selection viability includes execution by the receiving assignee`() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    p1.beginOperation("ImpossibleReactor<Player2>") {
      val impossible = game.tasks.extract { it }.single()
      val beforeSelection = game.tasks.getTaskData(impossible.id)

      p1.canSelectTask(impossible.id) shouldBe false
      shouldThrow<NotNowException> { p1.selectTask(impossible.id) }
      game.tasks.getTaskData(impossible.id) shouldBe beforeSelection

      p1.dropTask(impossible.id)
    }
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
                OWN[This: Result]
              }
              CLASS AbstractReactor : Owned<Player> {
                OWN[This: Reward]
                Reward: FollowUp
              }
              CLASS ImpossibleReactor : Owned<Player> {
                OWN[This: -Result]
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
