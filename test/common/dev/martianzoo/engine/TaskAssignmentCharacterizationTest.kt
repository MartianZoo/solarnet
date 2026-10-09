package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TaskAssignmentCharacterizationTest {
  private fun game() =
      Engine.newGame(
          testGamePremise(
              """
              CLASS Token<Owner>
              CLASS Marker<Owner>
              CLASS AdminToken
              CLASS HiddenToken : Hidden
              CLASS SystemToken : System
              ABSTRACT CLASS Reward : Owned<Player> {
                CLASS RewardA
                CLASS RewardB
              }
              CLASS SystemRequest : Owned<Player>, System {
                This: Reward
              }
              CLASS UnownedSystemRequest<@Player> : System {
                This: Reward<@Player>
              }
              CLASS Player2SystemSource : Owned<Player2> {
                This: UnownedSystemRequest<Player2>
              }
              ABSTRACT CLASS SystemChoice : System {
                CLASS SystemChoiceA
                CLASS SystemChoiceB
              }
              CLASS AutomaticBy { This:: Token<Player1> BY Admin }
              """,
              players = 2,
          )
      )

  @Test
  internal fun systemGainBeginsAssignedToAdmin() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    val checkpoint = game.timeline.checkpoint()

    val taskId = p1.addTasks("SystemToken BY Admin").single()

    p1.tasks.isEmpty() shouldBe true
    admin.tasks.ids().shouldContainExactly(taskId)
    game.tasks.getTaskData(taskId).let {
      it.controller shouldBe PLAYER1
      it.selectionAssignee shouldBe PLAYER1
      it.assignee shouldBe ADMIN
      it.selected shouldBe false
    }

    admin.selectTask(taskId)

    p1.count("SystemToken") shouldBe 1
    game.events.changesSince(checkpoint).single().actor shouldBe ADMIN
  }

  @Test
  internal fun systemGainPreservesAnExplicitPlayerPerformerAndRejectsIt() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }

    val taskId = p1.addTasks("SystemToken BY Player2").single()
    admin.tasks.ids().shouldContainExactly(taskId)

    shouldThrow<DeadEndException> { admin.selectTask(taskId) }

    game.tasks.getTaskData(taskId).let {
      it.assignee shouldBe ADMIN
      it.selected shouldBe false
    }
    p2.tasks.isEmpty() shouldBe true
    p1.count("SystemToken") shouldBe 0
  }

  @Test
  internal fun hiddenGainRemainsAssignedToPlayer() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }

    val taskId = p1.addTasks("HiddenToken").single()

    p1.tasks.ids().shouldContainExactly(taskId)
    admin.tasks.isEmpty() shouldBe true
    game.tasks.getTaskData(taskId).assignee shouldBe PLAYER1
  }

  @Test
  internal fun enclosingInstructionsDelaySystemRoutingUntilTheyResolveToADirectGain() {
    listOf(
            "SystemToken / Marker<Player1>",
            "Marker<Player1>: SystemToken",
        )
        .forEach { instruction ->
          val game = game()
          val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
          val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
          p1.runOperation("Marker<Player1>")

          val taskId = p1.addTasks(instruction).single()
          p1.tasks.ids().shouldContainExactly(taskId)
          admin.tasks.isEmpty() shouldBe true

          p1.selectTask(taskId)
          p1.tasks.isEmpty() shouldBe true
          admin.tasks.ids().shouldContainExactly(taskId)

          admin.selectTask(taskId)
          p1.count("SystemToken") shouldBe 1
          game.tasks.isEmpty() shouldBe true
        }
  }

  @Test
  internal fun ownedSystemGainRoutesItsQueuedChoiceToItsOwner() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }

    val request = p1.addTasks("SystemRequest").single()
    admin.selectTask(request)

    val reward = game.tasks.extract { it }.single()
    reward.controller shouldBe PLAYER1
    reward.selectionAssignee shouldBe PLAYER1
    reward.assignee shouldBe PLAYER1
    reward.instruction shouldBe parse<Instruction>("Reward<Player1>!")
  }

  @Test
  internal fun systemGainReturnsItsContinuationToTheOriginalPlayer() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }

    val request = p1.addTasks("SystemToken THEN Reward<Player1>").single()
    admin.selectTask(request)

    val reward = game.tasks.extract { it }.single()
    reward.controller shouldBe PLAYER1
    reward.selectionAssignee shouldBe PLAYER1
    reward.assignee shouldBe PLAYER1
    reward.instruction shouldBe parse<Instruction>("Reward<Player1>!")
  }

  @Test
  internal fun unownedSystemGainReturnsItsQueuedChoiceToTheOriginalRecipient() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }

    val request = p1.addTasks("UnownedSystemRequest<Player1>").single()
    admin.selectTask(request)

    val reward = game.tasks.extract { it }.single()
    reward.controller shouldBe PLAYER1
    reward.selectionAssignee shouldBe PLAYER1
    reward.assignee shouldBe PLAYER1
    reward.instruction shouldBe parse<Instruction>("Reward<Player1>!")
  }

  @Test
  internal fun unownedSystemGainKeepsASelectionRecipientDifferentFromItsController() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }

    val source = p1.addTasks("Player2SystemSource").single()
    p1.selectTask(source)

    val request = game.tasks.extract { it }.single()
    request.controller shouldBe PLAYER1
    request.selectionAssignee shouldBe PLAYER2
    request.assignee shouldBe ADMIN
    admin.selectTask(request.id)

    val reward = game.tasks.extract { it }.single()
    reward.controller shouldBe PLAYER1
    reward.selectionAssignee shouldBe PLAYER2
    reward.assignee shouldBe PLAYER1
    p1.selectTask(reward.id)
    p2.tasks.ids().shouldContainExactly(reward.id)
  }

  @Test
  internal fun adminCanNarrowAnUnselectedAbstractSystemGainByTaskId() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    val checkpoint = game.timeline.checkpoint()

    val taskId = p1.addTasks("SystemChoice").single()
    admin.fillInTask(taskId).apply { narrow("SystemChoiceA") }.commit()

    p1.count("SystemChoiceA") shouldBe 1
    game.events.changesSince(checkpoint).single().actor shouldBe ADMIN
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun eagerAdminExecutesSystemGainWhilePlayerAutoexecIsOff() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN)

    p1.runOperation("SystemToken")

    p1.count("SystemToken") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun ordinaryActorCanOnlySeeAndExecuteTasksAssignedToIt() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }

    p2.addTasks("Token<Player2>")

    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
    shouldThrow<TaskException> { p1.doTask("Token<Player2>") }

    p2.doTask("Token<Player2>")
    p2.count("Token<Player2>") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun oneAgentsPolicyDoesNotExecuteAnotherActorsTask() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }

    p2.addTasks("Token<Player2>")
    p1.autoExecPolicy = EAGER

    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
    p2.count("Token<Player2>") shouldBe 0
  }

  @Test
  internal fun sharedLoopRespectsEveryAssigneesPolicy() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }

    p2.addTasks("Token<Player2>")
    admin.addTasks("AdminToken")
    p1.autoExecNow()

    admin.count("AdminToken") shouldBe 0
    p2.count("Token<Player2>") shouldBe 0
    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2, ADMIN)
  }

  @Test
  internal fun concreteByWorkIsHandedToItsPerformingActor() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    val checkpoint = game.timeline.checkpoint()

    val task = p1.addTasks("Token<Player1> BY Admin").single()
    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER1)

    shouldThrow<TaskException> { p1.doTask("Token<Player1> BY Admin") }
    game.tasks.getTaskData(task).selected shouldBe false
    p1.selectTask(task)

    game.tasks.getTaskData(task).let {
      it.assignee shouldBe ADMIN
      it.selected shouldBe true
      it.instruction shouldBe parse<Instruction>("Token<Player1>!")
    }
    p1.count("Token") shouldBe 0

    admin.selectTask(task)

    p1.count("Token") shouldBe 1
    game.events.changesSince(checkpoint).single().actor shouldBe Actor.ADMIN
  }

  @Test
  internal fun automaticByCannotInventAnExecutingActorWithoutATaskHandoff() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    shouldThrow<ExpressionException> { p1.runOperation("AutomaticBy") }

    p1.count("AutomaticBy") shouldBe 0
    p1.count("Token") shouldBe 0
  }

  @Test
  internal fun aFormCanCompleteConcreteByWorkThatRemainsWithItsAssignee() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val task = p1.addTasks("Token<Player1> BY Player1").single()

    p1.fillInTask(task).commit()

    p1.count("Token") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun performerOverridePreservesThenTaskSequencing() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    val checkpoint = game.timeline.checkpoint()

    val token = p1.addTasks("(Token<Player1> THEN Marker<Player1>) BY Admin").single()
    game.tasks.extract { it.then != null }.shouldContainExactly(true)

    p1.selectTask(token)
    admin.selectTask(token)

    p1.count("Token") shouldBe 1
    p1.count("Marker") shouldBe 0
    game.tasks
        .extract { it.instruction.toString() }
        .shouldContainExactly("Marker<Player1>! BY Admin")

    val marker = game.tasks.ids().single()
    p1.selectTask(marker)
    admin.selectTask(marker)

    game.events
        .changesSince(checkpoint)
        .map { it.actor }
        .shouldContainExactly(Actor.ADMIN, Actor.ADMIN)
  }

  @Test
  internal fun resolvedByIsRejectedWhenSharedChoicesKeepThenInOneTask() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    val task =
        p1.addTasks("((X Token<Player1>? THEN X Marker<Player1>?) OR -Marker<Player1>) BY Player2")
            .single()
    val beforeSelection = game.tasks.getTaskData(task)

    shouldThrow<TaskException> { p1.selectTask(task) }

    game.tasks.getTaskData(task) shouldBe beforeSelection
    p1.count("Token<Player1>") shouldBe 0
    p1.count("Marker<Player1>") shouldBe 0
  }

  @Test
  internal fun resolvedOuterByRemainsOnEverySeparableThenStage() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    val task =
        p1.addTasks("((Token<Player1> THEN Marker<Player1>) OR -AdminToken) BY Player2").single()

    p1.selectTask(task)

    game.tasks.getTaskData(task).let {
      it.assignee shouldBe PLAYER2
      it.instruction shouldBe parse<Instruction>("Token<Player1>!")
      it.then shouldBe InstructionGroup(listOf(parse<Instruction>("Marker<Player1>! BY Player2")))
    }
    p2.selectTask(task)

    val marker = game.tasks.ids().single()
    p1.selectTask(marker)
    p2.selectTask(marker)
    p1.count("Token<Player1>") shouldBe 1
    p1.count("Marker<Player1>") shouldBe 1
  }

  @Test
  internal fun pendingTaskReceivesItsAddEventOrdinalWhenInsertedIntoItsAssigneesQueue() {
    val world = GameWorld(testGamePremise("CLASS Token<Player>", players = 2))
    val queues = TaskQueues(world)
    val cause = Cause(parse<Expression>("Token"), triggerEvent = 0)
    val pending =
        PendingTask(
            controller = PLAYER2,
            instruction = InstructionGroup(listOf(parse<Instruction>("Token<Player2>!"))),
            cause = cause,
        )

    val event = queues.addTasks(pending).single()
    val added = event.task

    added.id.ordinal shouldBe event.ordinal
    added.assignee shouldBe PLAYER2
    added.selectionAssignee shouldBe PLAYER2
    added.instruction shouldBe pending.instruction.instructions.single()
    added.cause shouldBe cause
    world.tasksFor(PLAYER2).ids().shouldContainExactly(TaskId(event.ordinal))
  }
}
