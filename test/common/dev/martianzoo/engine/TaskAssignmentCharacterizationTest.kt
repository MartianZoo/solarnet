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
              CLASS PlayerStep<Owner> { This: SystemToken }
              CLASS AdminToken
              CLASS IconicToken : Iconic
              CLASS SystemToken : System
              ABSTRACT CLASS Reward : Owned<Player> {
                CLASS RewardA
                CLASS RewardB
              }
              CLASS SystemRequest : Owned<Player>, System {
                This: Reward
              }
              CLASS AutomaticSystemSource : Owned<Player> {
                This:: SystemRequest, Token<Player1>
              }
              CLASS UnownedSystemRequest<@Player> : System {
                This: TASK @Player { Reward<@Player> }
              }
              ABSTRACT CLASS SystemChoice : System {
                CLASS SystemChoiceA { -This: TASK Player1 { Reward<Player1> } }
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
    val admin = game.testAgent(ADMIN)
    val checkpoint = game.events.nextOrdinal

    val taskId =
        game
            .actorEngine(PLAYER1)
            .addTasks(InstructionGroup.of(parse<Instruction>("SystemToken! BY Admin")))
            .single()

    p1.tasks.isEmpty() shouldBe true
    admin.tasks.ids().shouldContainExactly(taskId)
    game.tasks.getTaskData(taskId).let {
      it.assignee shouldBe ADMIN
      it.selected shouldBe false
    }

    game.timeline.atomic { game.actorEngine(ADMIN).selectTask(taskId) }

    p1.count("SystemToken") shouldBe 1
    game.events.changesSince(checkpoint).single().actor shouldBe ADMIN
  }

  @Test
  internal fun systemGainHandsOffBeforeThePlayersExecutionIsRejected() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN)

    val taskId =
        game
            .actorEngine(PLAYER1)
            .addTasks(InstructionGroup.of(parse<Instruction>("SystemToken! BY Player2")))
            .single()
    admin.tasks.ids().shouldContainExactly(taskId)

    game.actorEngine(ADMIN).selectTask(taskId)

    val handedOff = game.tasks.getTaskData(taskId)
    handedOff.assignee shouldBe PLAYER2
    handedOff.selected shouldBe true
    admin.tasks.isEmpty() shouldBe true
    p2.tasks.ids().shouldContainExactly(taskId)
    val beforeExecution = game.events.nextOrdinal

    shouldThrow<DeadEndException> { game.actorEngine(PLAYER2).doTask(taskId) }

    game.tasks.getTaskData(taskId) shouldBe handedOff
    game.events.nextOrdinal shouldBe beforeExecution
    p1.count("SystemToken") shouldBe 0
  }

  @Test
  internal fun iconicGainRemainsAssignedToPlayer() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN)

    val taskId = p1.addTasks("IconicToken").single()

    p1.tasks.ids().shouldContainExactly(taskId)
    admin.tasks.isEmpty() shouldBe true
    game.tasks.getTaskData(taskId).assignee shouldBe PLAYER1
  }

  @Test
  internal fun gatedSystemGainBeginsAssignedToAdmin() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN)
    p1.runOperation("Marker<Player1>")

    val taskId =
        game
            .actorEngine(PLAYER1)
            .addTasks(InstructionGroup.of(parse<Instruction>("Marker<Player1>: SystemToken!")))
            .single()
    p1.tasks.isEmpty() shouldBe true
    admin.tasks.ids().shouldContainExactly(taskId)

    game.timeline.atomic { game.actorEngine(ADMIN).selectTask(taskId) }
    p1.count("SystemToken") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun eagerAdminWaitsForTheGateThenReturnsThePlayersChoice() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN)
    p1.addTasks("Marker<Player1>")
    val taskId = p1.addTasks("Marker<Player1>: SystemToken THEN Reward<Player1>").single()

    admin.tasks.ids().shouldContainExactly(taskId)
    admin.autoExecNow(EAGER)
    p1.count("SystemToken") shouldBe 0
    game.tasks.getTaskData(taskId).selected shouldBe false

    p1.doTask("Marker<Player1>")

    p1.count("SystemToken") shouldBe 1
    admin.tasks.isEmpty() shouldBe true
    p1.doTask("RewardA")
    p1.count("RewardA") shouldBe 1
    game.isIdle() shouldBe true
  }

  @Test
  internal fun eagerAdminResolvesAScaledSystemGainWithoutPlayerAutoexecution() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN)
    p1.runOperation("2 Marker<Player1>")
    val before = game.events.nextOrdinal

    p1.runOperation("SystemToken / Marker<Player1>")

    p1.count("SystemToken") shouldBe 2
    game.events.changesSince(before).single().actor shouldBe ADMIN
    game.isIdle() shouldBe true
  }

  @Test
  internal fun playerChoosesVariableSystemAmountBeforeAdminExecutesIt() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN)
    p1.addTasks("X SystemToken")
    val before = game.events.nextOrdinal

    p1.doTask("3 SystemToken")

    p1.count("SystemToken") shouldBe 3
    game.events.changesSince(before).single().actor shouldBe ADMIN
    game.isIdle() shouldBe true
  }

  @Test
  internal fun automaticSystemGainUsesAdminAndKeepsThePlayerChoiceWithItsOwner() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN)
    val before = game.events.nextOrdinal

    p1.beginOperation("AutomaticSystemSource")

    val changes = game.events.changesSince(before)
    changes.single { it.change.gaining?.className.toString() == "SystemRequest" }.actor shouldBe
        ADMIN
    changes.single { it.change.gaining?.className.toString() == "Token" }.actor shouldBe PLAYER1
    p1.doTask("RewardA")
    p1.count("RewardA") shouldBe 1
    game.isIdle() shouldBe true
  }

  @Test
  internal fun playerChoosesWhetherAndWhatToRemoveBeforeAdminExecutesIt() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN)
    p1.runOperation("SystemChoiceA, SystemChoiceB")
    p1.addTasks("-SystemChoiceA?")
    p1.doTasks()
    p1.count("SystemChoice") shouldBe 2
    p1.doTasks("Ok")
    p1.addTasks("-SystemChoice!")

    p1.doTasks()
    p1.count("SystemChoice") shouldBe 2
    val before = game.events.nextOrdinal
    p1.doTask("-SystemChoiceA")

    p1.count("SystemChoiceA") shouldBe 0
    p1.count("SystemChoiceB") shouldBe 1
    game.events.changesSince(before).single().actor shouldBe ADMIN
    p1.doTasks("RewardA")
    p1.count("RewardA") shouldBe 1
    game.isIdle() shouldBe true
  }

  @Test
  internal fun ownedSystemGainRoutesItsQueuedChoiceToItsOwner() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN)

    p1.beginOperation("SystemRequest")

    val reward = game.tasks.extract { it }.single()
    reward.assignee shouldBe PLAYER1
    reward.instruction shouldBe parse<Instruction>("Reward<Player1>!")
  }

  @Test
  internal fun systemGainReturnsItsContinuationToThePlayer() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN)

    p1.beginOperation("SystemToken THEN Reward<Player1>")

    val reward = game.tasks.extract { it }.single()
    reward.assignee shouldBe PLAYER1
    reward.instruction shouldBe parse<Instruction>("Reward<Player1>!")
  }

  @Test
  internal fun eagerAdminExecutesSuccessiveSystemStepsBeforeReturningThePlayerChoice() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN)
    val checkpoint = game.events.nextOrdinal

    p1.beginOperation("SystemToken THEN SystemToken THEN Reward<Player1>")

    game.events.changesSince(checkpoint).map { it.actor } shouldBe listOf(ADMIN, ADMIN)
    p1.tasks.extract { it.instruction.toString() } shouldBe listOf("Reward<Player1>!")
    p1.doTask("RewardA")
    p1.count("RewardA") shouldBe 1
    game.isIdle() shouldBe true
  }

  @Test
  internal fun narrowingAPlayerChoiceToASystemStepStillReturnsItsContinuation() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN)
    val request = p1.addTasks("X SystemToken THEN Reward<Player1>").single()
    val checkpoint = game.events.nextOrdinal

    p1.selectTask(request)
    p1.narrowTask("2 SystemToken")

    p1.count("SystemToken") shouldBe 2
    game.events.changesSince(checkpoint).single().actor shouldBe ADMIN
    p1.tasks.extract { it.instruction.toString() } shouldBe listOf("Reward<Player1>!")
  }

  @Test
  internal fun unownedSystemGainCanDispatchItsQueuedChoice() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN)

    p1.beginOperation("UnownedSystemRequest<Player1>")

    val reward = game.tasks.extract { it }.single()
    reward.assignee shouldBe PLAYER1
    reward.instruction shouldBe parse<Instruction>("Reward<Player1>!")
  }

  @Test
  internal fun playerNarrowsAbstractSystemGainBeforeAdminExecutesIt() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN)
    val checkpoint = game.events.nextOrdinal

    val taskId = p1.addTasks("SystemChoice").single()
    p1.tasks.ids().shouldContainExactly(taskId)
    admin.tasks.isEmpty() shouldBe true

    p1.doTask("SystemChoiceA")

    // The Player supplies the narrowing and Admin performs the resulting concrete System gain,
    // so the Player's submission is accepted without the Player ever executing System work.
    p1.count("SystemChoiceA") shouldBe 1
    game.events.changesSince(checkpoint).single().actor shouldBe ADMIN
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun optionalAndAlternativeSystemGainsKeepTheSamePlayerChoice() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN)

    listOf("SystemToken?", "SystemToken OR Ok").forEachIndexed { index, offer ->
      val declined = p1.addTasks(offer).single()
      p1.tasks.ids().shouldContainExactly(declined)
      p1.doTask("Ok")
      p1.count("SystemToken") shouldBe index

      val accepted = p1.addTasks(offer).single()
      p1.tasks.ids().shouldContainExactly(accepted)
      val checkpoint = game.events.nextOrdinal
      p1.doTask("SystemToken")
      p1.count("SystemToken") shouldBe index + 1
      game.events.changesSince(checkpoint).single().actor shouldBe ADMIN
    }
  }

  @Test
  internal fun playerChoosesAnAbstractSystemTransmutation() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN)
    p1.runOperation("SystemChoiceA")

    val taskId = p1.addTasks("SystemChoice(NOT Source@) FROM Source@SystemChoice").single()
    p1.tasks.ids().shouldContainExactly(taskId)
    val checkpoint = game.events.nextOrdinal
    p1.doTask("SystemChoiceB FROM SystemChoiceA")

    p1.count("SystemChoiceA") shouldBe 0
    p1.count("SystemChoiceB") shouldBe 1
    game.events.changesSince(checkpoint).single().actor shouldBe ADMIN
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
  internal fun eagerAdminWorkPrecedesEarlierEagerPlayerWork() {
    val game = game()
    val p1 = game.testAgent(PLAYER1)
    game.testAgent(ADMIN)
    p1.addTasks("Token<Player1>")
    game.actorEngine(ADMIN).addTasks(InstructionGroup.of(parse<Instruction>("AdminToken!")))
    val checkpoint = game.events.nextOrdinal

    p1.autoExecNow()

    game.events.changesSince(checkpoint).map { it.actor }.shouldContainExactly(ADMIN, PLAYER1)
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun eagerAdminHandlesTriggeredWorkBetweenExplicitPlayerSteps() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN)
    p1.addTasks("PlayerStep<Player1>")
    p1.addTasks("Marker<Player1>")
    val checkpoint = game.events.nextOrdinal

    p1.continueOperation {
      doTask("PlayerStep<Player1>")
      p1.count("SystemToken") shouldBe 1
      doTask("Marker<Player1>")
    }

    game.events
        .changesSince(checkpoint)
        .map { it.actor }
        .shouldContainExactly(PLAYER1, ADMIN, PLAYER1)
  }

  @Test
  internal fun abstractSystemChoiceDoesNotPreventExecutablePlayerWork() {
    val game = game()
    val p1 = game.testAgent(PLAYER1)
    game.testAgent(ADMIN)
    val systemChoice = p1.addTasks("SystemChoice").single()
    p1.addTasks("Token<Player1>")

    p1.autoExecNow()

    p1.count("Token<Player1>") shouldBe 1
    game.tasks.ids().shouldContainExactly(systemChoice)
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
  internal fun sharedLoopRespectsEveryPlayerPolicyAndAlwaysPerformsAdminWork() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN)

    p2.addTasks("Token<Player2>")
    admin.addTasks("AdminToken")
    p1.autoExecNow()

    // A policy describes what a Player's client does for it. Admin has no such client, so its
    // work is performed while Player2's own task waits for Player2.
    admin.count("AdminToken") shouldBe 1
    p2.count("Token<Player2>") shouldBe 0
    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
  }

  @Test
  internal fun concreteGainByAdminBeginsAssignedToAdmin() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    game.testAgent(ADMIN)
    val checkpoint = game.events.nextOrdinal

    val task =
        game
            .actorEngine(PLAYER1)
            .addTasks(InstructionGroup.of(parse<Instruction>("Token<Player1>! BY Admin")))
            .single()
    p1.tasks.isEmpty() shouldBe true
    game.tasks.getTaskData(task).let {
      it.assignee shouldBe ADMIN
      it.selected shouldBe false
      it.instruction shouldBe parse<Instruction>("Token<Player1>! BY Admin")
    }
    p1.count("Token") shouldBe 0

    game.timeline.atomic { game.actorEngine(ADMIN).selectTask(task) }

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
    game.testAgent(ADMIN)
    val checkpoint = game.events.nextOrdinal

    p1.addTasks("(Token<Player1> THEN Marker<Player1>) BY Admin")

    // Admin performs the continuation itself, so the sequencing shows in the order of its two
    // changes rather than in a continuation left pending between them.
    p1.count("Token") shouldBe 1
    p1.count("Marker") shouldBe 1
    game.tasks.isEmpty() shouldBe true
    game.events
        .changesSince(checkpoint)
        .map { it.actor to it.change.gaining?.type?.rootClass?.className?.toString() }
        .shouldContainExactly(Actor.ADMIN to "Token", Actor.ADMIN to "Marker")
  }

  @Test
  internal fun resolvedByIsRejectedWhenSharedChoicesKeepThenInOneTask() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    val task =
        p1.addTasks("((X? Token<Player1> THEN X? Marker<Player1>) OR -Marker<Player1>) BY Player2")
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
            assignee = PLAYER2,
            instruction = InstructionGroup(listOf(parse<Instruction>("Token<Player2>!"))),
            cause = cause,
        )

    val event = queues.addTasks(pending).single()
    val added = event.task

    added.id.ordinal shouldBe event.ordinal
    added.assignee shouldBe PLAYER2
    added.instruction shouldBe pending.instruction.instructions.single()
    added.cause shouldBe cause
    world.tasksFor(PLAYER2).ids().shouldContainExactly(TaskId(event.ordinal))
  }
}
