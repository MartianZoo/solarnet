package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TaskDelegationTest {
  @Test
  internal fun `the active player offers a concrete reaction before its owner executes it`() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN)
    admin.runOperation("Observer")

    p1.beginOperation("ConcreteReactor<Player2>") {
      val reaction = game.tasks.extract { it }.single()

      shouldThrow<TaskException> { p2.selectTask(reaction.id) }
      shouldThrow<TaskException> { p2.doTask(reaction.id) }
      p1.canExecuteTask(reaction.id) shouldBe true
      p1.selectTask(reaction.id)
      p2.tasks.isEmpty() shouldBe true
      p1.doTask(reaction.id)

      val delegated = game.tasks.getTaskData(game.tasks.selectedTask()!!)
      delegated.selected shouldBe true
      p2.doTask(delegated.id)

      val followUp = game.tasks.extract { it }.single()
      shouldThrow<TaskException> { p2.doTask(followUp.id) }
      p1.doTask(followUp.id)
      p2.doTask("FollowUp")
    }

    p1.count("Wrong") shouldBe 0
    p1.count("FollowUp") shouldBe 1
  }

  @Test
  internal fun `the active player offers an abstract reaction without making the owners choice`() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }

    p1.beginOperation("AbstractReactor<Player2>") {
      p1.addTasks("Spare<Player1>?, Later<Player1>?")
      val reward = game.tasks.extract { it }.single { it.instruction is Instruction.Dispatch }

      p1.doTask("Spare<Player1>")
      shouldThrow<TaskException> { p1.doTask("RewardA<Player2>") }
      shouldThrow<TaskException> { p1.selectTask("RewardA<Player2>") }
      game.tasks.getTaskData(reward.id) shouldBe reward
      shouldThrow<TaskException> { p2.selectTask(reward.id) }
      shouldThrow<TaskException> { p2.doTask("RewardA<Player2>") }
      p1.doTask(reward.id)

      val delegated = game.tasks.getTaskData(game.tasks.selectedTask()!!)
      delegated.selected shouldBe true
      p2.count("RewardA") shouldBe 0
      shouldThrow<TaskException> { p1.doTask("Later<Player1>") }

      p2.narrowTask("RewardA<Player2>")
      p2.count("RewardA") shouldBe 0
      shouldThrow<TaskException> { p1.doTask("RewardA<Player2>") }
      p2.doTask("RewardA<Player2>")

      val followUp = game.tasks.extract { it }.single { it.instruction is Instruction.Dispatch }
      shouldThrow<TaskException> { p2.doTask(followUp.id) }
      p1.doTask(followUp.id)
      p2.doTask("FollowUp")
      p1.doTask("Later<Player1>")
    }

    p2.count("RewardA") shouldBe 1
    p1.count("FollowUp") shouldBe 1
    p1.count("Spare") shouldBe 1
    p1.count("Later") shouldBe 1
  }

  @Test
  internal fun `the active players form cannot make the receiving owners choice`() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }

    p1.beginOperation("AbstractReactor<Player2>") {
      val reward = game.tasks.extract { it }.single()
      shouldThrow<NarrowingException> {
        p1.tryTask("RewardA<Player2>", reward.id)
      }
      game.tasks.getTaskData(reward.id) shouldBe reward

      val form = p1.fillInTask(reward.id)
      shouldThrow<NarrowingException> {
        form.narrow("NOW Player2 { RewardA<Player2> }")
      }
      game.tasks.getTaskData(reward.id) shouldBe reward

      p1.fillInTask(reward.id).commit()
      p2.tasks.isEmpty() shouldBe true
      p1.doTask(reward.id)

      game.tasks.getTaskData(game.tasks.selectedTask()!!).let {
        it.selected shouldBe true
        it.instruction.toString() shouldBe "Reward<Player2>!"
      }
      p2.doTask("RewardA<Player2>")

      val followUp = game.tasks.extract { it }.single()
      shouldThrow<TaskException> { p2.doTask(followUp.id) }
      p1.doTask(followUp.id)
      p2.doTask("FollowUp")
    }

    p2.count("RewardA") shouldBe 1
  }

  @Test
  internal fun `selection viability includes execution by the receiving assignee`() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }

    p1.beginOperation("ImpossibleReactor<Player2>") {
      val impossible = game.tasks.extract { it }.single()
      val beforeSelection = game.tasks.getTaskData(impossible.id)

      p1.canSelectTask(impossible.id) shouldBe false
      shouldThrow<NotNowException> { p1.doTask(impossible.id) }
      game.tasks.getTaskData(impossible.id) shouldBe beforeSelection

      p1.dropTask(impossible.id)
    }
  }

  @Test
  internal fun systemBookkeepingReturnsItsQueuedChoiceToTheOriginalPlayer() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN)

    p1.beginOperation("UnownedSystemRequest<Player1>")

    p1.doTask("RewardA<Player1>")
    p1.count("RewardA") shouldBe 1
  }

  @Test
  internal fun systemBookkeepingPreservesTheActivePlayersHandoffToAnotherOwner() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN)

    p1.beginOperation("Player2SystemSource")

    val reward = game.tasks.extract { it }.single()
    shouldThrow<TaskException> { p2.doTask("RewardA<Player2>") }
    p1.doTask(reward.id)
    game.tasks.getTaskData(game.tasks.selectedTask()!!).selected shouldBe true
    p2.doTask("RewardA<Player2>")
    p2.count("RewardA") shouldBe 1
  }

  @Test
  internal fun theOwnersAutomaticPolicyWaitsForTheActivePlayersHandoff() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = EAGER }

    p1.beginOperation("ConcreteReactor<Player2>")

    p2.count("Result") shouldBe 0
    val reward = game.tasks.extract { it }.single()
    p1.doTask(reward.id)
    p2.count("Result") shouldBe 1
    game.isIdle() shouldBe true
  }

  @Test
  internal fun aDispatchReturnsItsContinuationToItsOwnerAfterTheActivePlayerStartsIt() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN).runOperation("Observer")
    p2.addTasks("SOON Player1 { Spare<Player1> } THEN Result<Player2>")

    p1.doTask("NOW Player2 { SOON Player1 { Spare<Player1> } }")
    p2.doTask("SOON Player1 { Spare<Player1> }")
    p1.doTask("Spare<Player1>")
    shouldThrow<TaskException> { p1.doTask("Result<Player2>") }
    shouldThrow<TaskException> { p2.doTask("Result<Player2>") }
    p1.doTask("NOW Player2 { Result<Player2> }")
    p2.doTask("Result<Player2>")
    p1.doTask("NOW Player2 { FollowUp }")
    p2.doTask("FollowUp")

    p1.count("Wrong") shouldBe 0
    p1.count("FollowUp") shouldBe 1
    game.isIdle() shouldBe true
  }

  @Test
  internal fun aCalculatedHandoffCountsCompleteGroupsAndLeavesTheResourceChoiceToItsOwner() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    p1.runOperation("5 Spare<Player1>")
    p2.addTasks("Reward<Player2> / 2 Spare<Player1>")

    shouldThrow<TaskException> { p1.doTask("NOW Player2 { 5 Reward<Player2> }") }
    shouldThrow<TaskException> { p1.doTask("NOW Player2 { 2 RewardA<Player2> }") }
    p1.doTask("NOW Player2 { 2 Reward<Player2> }")
    val selected = p2.tasks.selectedTask()!!
    p2.fillInTask(selected).apply {
      narrow("RewardA<Player2> / 2 Spare<Player1>")
      commit()
    }
    p2.count("RewardA") shouldBe 0
    p2.doTask("2 RewardA<Player2>")
    p2.count("RewardA") shouldBe 2
    game.isIdle() shouldBe true
  }

  private fun game() =
      Engine.newGame(
              testGamePremise(
                  """
              CLASS Turn<@Player> : TaskScope<@Player>
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
                This: Result
              }
              CLASS AbstractReactor : Owned<Player> {
                This: Reward
                Reward: FollowUp
              }
              CLASS ImpossibleReactor : Owned<Player> {
                This: -Result
              }
              CLASS UnownedSystemRequest<@Player> : System {
                This: NOW @Player { Reward<@Player> }
              }
              CLASS OwnedSystemRequest : Owned<Player>, System {
                This: Reward
              }
              CLASS Player2SystemSource : Owned<Player2> {
                This: OwnedSystemRequest<Player2>
              }
              CLASS Observer {
                Result<Player2> BY Player1: Wrong
                Result<Player2> BY Player2: FollowUp
              }
              """,
                  players = 2,
              )
          )
          .also { it.testAgent(ADMIN).exMachina("Turn<Player1>") }
}
