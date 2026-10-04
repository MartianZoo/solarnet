package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class InvariantCompletionTest {
  @Test
  internal fun bootstrapChecksMinimumsAfterAllInitialComponentsArePresent() {
    val admin =
        world(
                """
                ABSTRACT CLASS Status { HAS =2 Status }
                CLASS First : Status
                CLASS Second : Status
                """,
                "First",
                "Second",
            )
            .testAgent(ADMIN)

    admin.count("Status") shouldBe 2
  }

  @Test
  internal fun inheritedReplacementEffectKeepsExactlyOneFamilyMember() {
    val admin = replacementWorld().testAgent(ADMIN)

    admin.runOperation("Passed!")

    admin.count("Ready") shouldBe 0
    admin.count("Passed") shouldBe 1
    admin.runOperation("Ready!")
    admin.count("Ready") shouldBe 1
    admin.count("Passed") shouldBe 0
  }

  @Test
  internal fun familyMaximumRejectsRepeatedValueWithoutAPerTypeMaximum() {
    val world = replacementWorld()
    val admin = world.testAgent(ADMIN)
    val before = world.timeline.checkpoint()

    shouldThrow<LimitsException> { admin.runOperation("Ready!") }

    world.timeline.checkpoint() shouldBe before
    admin.count("Ready") shouldBe 1
  }

  @Test
  internal fun gainThenRemovePreservesExactOneAcrossRecursiveEffects() {
    val world = statusWorld("This:: Notice!", "This:: -Ready!")
    val admin = world.testAgent(ADMIN)

    admin.runOperation("Passed!")

    admin.count("Ready") shouldBe 0
    admin.count("Passed") shouldBe 1
    admin.count("Status") shouldBe 1
  }

  @Test
  internal fun removeThenGainPreservesExactOne() {
    val world =
        world(
            """
            ABSTRACT CLASS Status { HAS =1 Status }
            CLASS Ready : Status { -This:: Passed! }
            CLASS Passed : Status
            """,
            "Ready",
        )
    val admin = world.testAgent(ADMIN)

    admin.runOperation("-Ready!")

    admin.count("Ready") shouldBe 0
    admin.count("Passed") shouldBe 1
  }

  @Test
  internal fun failedRepairRestoresComponentsTasksHistoryAndEffects() {
    val world = statusWorld("This:: Notice!", "This: -Ready!")
    val admin = world.testAgent(ADMIN)
    val before = world.timeline.checkpoint()

    shouldThrow<LimitsException> { admin.runOperation("Passed!") }

    world.timeline.checkpoint() shouldBe before
    world.tasks.isEmpty() shouldBe true
    admin.count("Ready") shouldBe 1
    admin.count("Passed") shouldBe 0
    admin.count("Notice") shouldBe 0
    admin.runOperation("Pulse!")
    admin.count("Reward") shouldBe 0
  }

  @Test
  internal fun amapKeepsItsCapAndTreatsNegativeHeadroomAsZero() {
    val world =
        world(
            """
            CLASS Token {
              HAS MAX 1 This
              This IF =2 Token:: 5 Token., -Token!
            }
            """
        )
    val admin = world.testAgent(ADMIN)
    admin.runOperation("5 Token.")
    admin.count("Token") shouldBe 1

    val result = admin.runOperation("Token!")

    admin.count("Token") shouldBe 1
    result.changes.size shouldBe 2
  }

  @Test
  internal fun amapDoesNotInferATargetByExcludingAZeroCapacitySubclass() {
    checkZeroCapacityChoice(".")
  }

  @Test
  internal fun optionalDoesNotInferATargetByExcludingAZeroCapacitySubclass() {
    checkZeroCapacityChoice("?")
  }

  private fun checkZeroCapacityChoice(quantifier: String) {
    val admin =
        world(
                """
                CLASS Holder { HAS MAX 1 This }
                ABSTRACT CLASS Token
                CLASS Capped<Holder> : Token { HAS MAX 0 This }
                CLASS Available<Holder> : Token
                """,
                "Holder",
            )
            .testAgent(ADMIN)

    admin.runOperation("Token$quantifier") { doTask("Capped$quantifier") }

    admin.count("Token") shouldBe 0
  }

  @Test
  internal fun failedTryWithNarrowingRestoresThePendingTaskAndCascade() {
    val world = statusWorld("This:: Notice!")
    val admin = world.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    val task = admin.addTasks("Status!").single()
    val pending = admin.tasks.getTaskData(task)
    val before = world.timeline.checkpoint()

    admin.tryTask("Passed!")

    world.timeline.checkpoint() shouldBe before
    admin.tasks.getTaskData(task) shouldBe pending
    world.tasks.selectedTask() shouldBe null
    admin.count("Ready") shouldBe 1
    admin.count("Passed") shouldBe 0
    admin.count("Notice") shouldBe 0
  }

  @Test
  internal fun choiceKeepsAnInBoundsArmEvenWhenItsAutomaticWorkMayFail() {
    val admin =
        world(
                """
                CLASS Guarded { This:: -Missing! }
                CLASS Missing
                CLASS Other
                """
            )
            .testAgent(ADMIN)
            .also { it.autoExecPolicy = NONE }
    val task = admin.addTasks("Guarded! OR Other!").single()

    admin.selectTask(task)

    admin.count("Other") shouldBe 0
    admin.doTask("Other!")
    admin.count("Other") shouldBe 1
  }

  @Test
  internal fun choiceKeepsAnArmWhoseCascadeRepairsTheLimit() {
    val world = statusWorld("This:: -Ready!")
    val admin = world.testAgent(ADMIN)

    admin.runOperation("Passed! OR Die!")

    admin.count("Passed") shouldBe 1
    admin.count("Ready") shouldBe 0
  }

  @Test
  internal fun choiceAllowsADeadEndAndRollsItBackBeforeAnotherChoice() {
    val world = statusWorld("This:: Notice!")
    val admin = world.testAgent(ADMIN)

    admin.runOperation("Passed! OR Reward!") {
      val before = world.timeline.checkpoint()
      shouldThrow<LimitsException> { doTask("Passed!") }
      world.timeline.checkpoint() shouldBe before
      admin.count("Notice") shouldBe 0
      admin.count("Passed") shouldBe 0
      doTask("Reward!")
    }

    admin.count("Passed") shouldBe 0
    admin.count("Ready") shouldBe 1
    admin.count("Notice") shouldBe 0
    admin.count("Reward") shouldBe 1
  }

  @Test
  internal fun abstractGainCanSelectAConcreteRepair() {
    val world = statusWorld("This:: -Ready!")
    val admin = world.testAgent(ADMIN)

    admin.runOperation("Status!") { doTask("Passed!") }

    admin.count("Passed") shouldBe 1
    admin.count("Ready") shouldBe 0
  }

  @Test
  internal fun intermediateStatesRemainVisibleToNestedQueuedConditions() {
    val world = statusWorld("This:: Notice!, -Ready!", "This IF =1 Status: Reward!")
    val admin = world.testAgent(ADMIN)

    admin.runOperation("Passed!")

    admin.count("Status") shouldBe 1
    admin.count("Reward") shouldBe 0
  }

  @Test
  internal fun sourceAvailabilityCannotBeRepairedAfterRemoval() {
    val world = world("CLASS Token { -This:: Token! }")
    val admin = world.testAgent(ADMIN)
    val before = world.timeline.checkpoint()

    shouldThrow<LimitsException> { admin.runOperation("-Token!") }

    world.timeline.checkpoint() shouldBe before
    admin.count("Token") shouldBe 0
  }

  @Test
  internal fun dependencyAvailabilityCannotBeRepairedAfterGain() {
    val world =
        world(
            """
            CLASS Anchor { HAS MAX 1 This }
            CLASS Attached<Anchor> { This:: Anchor! }
            """
        )
    val admin = world.testAgent(ADMIN)
    val before = world.timeline.checkpoint()

    shouldThrow<DependencyException> { admin.runOperation("Attached!") }

    world.timeline.checkpoint() shouldBe before
    admin.count("Anchor") shouldBe 0
    admin.count("Attached") shouldBe 0
  }

  @Test
  internal fun requiredCountsIncludeIndirectDependencyScopes() {
    val world =
        world(
            """
            ABSTRACT CLASS Anchor {
              HAS MAX 1 This
              HAS =1 Attached<Bridge<This>>
              This:: Bridge<This>! THEN Attached<Bridge<This>>!
            }
            CLASS FirstAnchor : Anchor
            CLASS SecondAnchor : Anchor
            CLASS Bridge<Anchor> { HAS MAX 1 This }
            CLASS Attached<Bridge>
            """
        )
    val admin = world.testAgent(ADMIN)
    admin.runOperation("FirstAnchor!")
    val before = world.timeline.checkpoint()

    shouldThrow<LimitsException> { admin.runOperation("-Attached!") }

    world.timeline.checkpoint() shouldBe before
    admin.count("Attached") shouldBe 1
    admin.runOperation("-FirstAnchor!")
    admin.count("Bridge") shouldBe 0
    admin.count("Attached") shouldBe 0
  }

  @Test
  internal fun gainingAnOwnerMustEstablishItsNewRequiredComponents() {
    val world =
        world(
            """
            ABSTRACT CLASS Anchor {
              HAS MAX 1 This
              HAS =1 Marker<This>
            }
            CLASS Broken : Anchor
            CLASS Repaired : Anchor { This:: Marker<This>! }
            CLASS Marker<Anchor>
            """
        )
    val admin = world.testAgent(ADMIN)
    val before = world.timeline.checkpoint()

    shouldThrow<LimitsException> { admin.runOperation("Broken!") }
    world.timeline.checkpoint() shouldBe before
    admin.count("Broken") shouldBe 0

    admin.runOperation("Repaired!")
    admin.count("Marker<Repaired>") shouldBe 1

    admin.runOperation("-Repaired!")
    admin.count("Repaired") shouldBe 0
    admin.count("Marker") shouldBe 0
  }

  @Test
  internal fun separateInitiatingChangesCannotRepairEachOther() {
    val world = statusWorld("")
    val admin = world.testAgent(ADMIN)
    val before = world.timeline.checkpoint()

    shouldThrow<LimitsException> { admin.runOperation("Passed! THEN -Ready!") }

    world.timeline.checkpoint() shouldBe before
    admin.count("Ready") shouldBe 1
    admin.count("Passed") shouldBe 0
  }

  @Test
  internal fun automaticSiblingsRetainTheirOriginalTriggerSnapshot() {
    val world =
        statusWorld(
            """
            This IF =2 Status:: -Ready!
            This IF =2 Status:: Reward!
            """
        )
    val admin = world.testAgent(ADMIN)

    admin.runOperation("Passed!")

    admin.count("Status") shouldBe 1
    admin.count("Reward") shouldBe 1
  }

  @Test
  internal fun amapRemovalClampsNegativeFootroomDuringARepair() {
    val world =
        world(
            """
            ABSTRACT CLASS Status { HAS =1 Status }
            CLASS Ready : Status { -This:: -Ready., Passed! }
            CLASS Passed : Status
            """,
            "Ready",
        )
    val admin = world.testAgent(ADMIN)

    val result = admin.runOperation("-Ready!")

    admin.count("Passed") shouldBe 1
    result.changes.size shouldBe 2
  }

  @Test
  internal fun abstractRemovalFindsAnExistingTargetWhoseCascadeRepairsTheMinimum() {
    val world =
        world(
            """
            ABSTRACT CLASS Token { HAS =2 Token }
            CLASS First : Token { -This:: Replacement! }
            CLASS Second : Token
            CLASS Replacement : Token
            CLASS Seed { This:: First!, Second! }
            """,
            "Seed",
        )
    val admin = world.testAgent(ADMIN)

    admin.runOperation("-Token!") { doTask("-First!") }

    admin.count("Token") shouldBe 2
    admin.count("First") shouldBe 0
    admin.count("Replacement") shouldBe 1
  }

  @Test
  internal fun abstractGainLeavesTheChoiceOfARepairToTheClient() {
    val world =
        world(
            """
            CLASS Place { HAS MAX 1 This }
            ABSTRACT CLASS Status { HAS =1 Status }
            CLASS Ready : Status
            CLASS Passed<Place> : Status { This:: -Ready! }
            CLASS Broken<Place> : Status
            """,
            "Place",
            "Ready",
        )
    val admin = world.testAgent(ADMIN)

    admin.runOperation("Status!") {
      admin.count("Passed") shouldBe 0
      val before = world.timeline.checkpoint()
      shouldThrow<LimitsException> { doTask("Broken!") }
      world.timeline.checkpoint() shouldBe before
      doTask("Passed!")
    }

    admin.count("Ready") shouldBe 0
    admin.count("Passed") shouldBe 1
    admin.count("Broken") shouldBe 0
  }

  @Test
  internal fun automaticChoiceCannotRejectAnArmRepairedByALaterConsequence() {
    val world =
        world(
            """
            ABSTRACT CLASS Status { HAS =1 Status }
            CLASS Ready : Status
            CLASS Passed : Status {
              This:: (Extra! OR -Missing!) THEN -Ready! THEN -Extra!
            }
            CLASS Extra : Status
            CLASS Missing
            """,
            "Ready",
        )
    val admin = world.testAgent(ADMIN)

    admin.runOperation("Passed!")

    admin.count("Passed") shouldBe 1
    admin.count("Status") shouldBe 1
    admin.count("Extra") shouldBe 0
  }

  @Test
  internal fun automaticChoiceRequiresAnAuthoredConditionWhenBoundsCannotDecideYet() {
    val world =
        world(
            """
            CLASS Token { HAS MAX 1 This }
            CLASS Other
            CLASS Start { This:: Token! OR Other! }
            """,
            "Token",
        )
    val admin = world.testAgent(ADMIN)
    val before = world.timeline.checkpoint()

    shouldThrow<NotFullySpecifiedException> { admin.runOperation("Start!") }

    world.timeline.checkpoint() shouldBe before
    admin.count("Start") shouldBe 0
    admin.count("Token") shouldBe 1
    admin.count("Other") shouldBe 0
  }

  @Test
  internal fun ordinaryAdminWorkAllowsAChoiceAfterTheInitiatingOperation() {
    val world =
        world(
            """
            CLASS Token { HAS MAX 1 This }
            CLASS Other
            CLASS Start { This: Token! OR Other! }
            """,
            "Token",
        )
    val admin = world.testAgent(ADMIN)

    admin.runOperation("Start!") { doTask("Other!") }

    admin.count("Start") shouldBe 1
    admin.count("Token") shouldBe 1
    admin.count("Other") shouldBe 1
  }

  @Test
  internal fun aRepairCanDependOnThePerformer() {
    checkPerformerDependentRepair(overrideActor = false)
  }

  @Test
  internal fun aRepairCanDependOnAnExplicitByPerformer() {
    checkPerformerDependentRepair(overrideActor = true)
  }

  @Test
  internal fun theWrongPerformerDoesNotTriggerTheRepair() {
    val world = performerDependentWorld()
    val before = world.timeline.checkpoint()
    val admin = world.testAgent(ADMIN)

    shouldThrow<LimitsException> { admin.runOperation("Passed! OR Die!") }

    world.timeline.checkpoint() shouldBe before
    admin.count("Ready") shouldBe 1
    admin.count("Passed") shouldBe 0
  }

  private fun checkPerformerDependentRepair(overrideActor: Boolean) {
    val world = performerDependentWorld()
    val actor = world.testAgent(if (overrideActor) ADMIN else PLAYER1)

    actor.runOperation(if (overrideActor) "(Passed! BY Player1) OR Die!" else "Passed! OR Die!")

    actor.count("Passed") shouldBe 1
    actor.count("Ready") shouldBe 0
  }

  private fun performerDependentWorld(): World {
    val source =
        """
        ABSTRACT CLASS Status { HAS =1 Status }
        CLASS Ready : Status
        CLASS Passed : Status { This BY Player1:: -Ready! }
        CLASS Other
        """
    val premise =
        testGamePremise(source).copy(initialComponentTypes = setOf(parse<Expression>("Ready")))
    return Engine.newGame(premise)
  }

  private fun replacementWorld() =
      world(
          """
          ABSTRACT CLASS Status {
            HAS =1 Status
            This:: -Status(NOT This).
          }
          CLASS Ready : Status
          CLASS Passed : Status
          """,
          "Ready",
      )

  private fun statusWorld(passedEffects: String, noticeEffects: String = "") =
      world(
          """
          ABSTRACT CLASS Status { HAS =1 Status }
          CLASS Ready : Status
          CLASS Passed : Status {
            $passedEffects
            Pulse:: Reward!
          }
          CLASS Notice { $noticeEffects }
          CLASS Pulse : Signal
          CLASS Reward
          """,
          "Ready",
      )

  private fun world(source: String, vararg initial: String): World =
      Engine.newGame(
          testGamePremise(source, players = 0)
              .copy(initialComponentTypes = initial.map { parse<Expression>(it) }.toSet())
      )
}
