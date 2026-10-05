package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.engine.Exceptions.RunawayEffectChainException
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
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
  internal fun gainingAnOwnerEstablishesItsInheritedRequiredParts() {
    val admin =
        world(
                """
                ABSTRACT CLASS Anchor { HAS MAX 1 This, =2 Marker<This> }
                CLASS First : Anchor
                CLASS Second : Anchor
                CLASS Marker<Anchor> { This: Reward }
                CLASS Reward
                """
            )
            .testAgent(ADMIN)

    admin.runOperation("First!, Second!")
    admin.count("Marker<First>") shouldBe 2
    admin.count("Marker<Second>") shouldBe 2
    admin.count("Reward") shouldBe 4

    shouldThrow<LimitsException> { admin.runOperation("-Marker<First>!") }
    admin.count("Marker<First>") shouldBe 2
    admin.runOperation("-First!")
    admin.count("Marker<First>") shouldBe 0
    admin.count("Marker<Second>") shouldBe 2
  }

  @Test
  internal fun bootstrapConstructsRequiredPartsAndRunsTheirEffects() {
    val admin =
        world(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This, =2 Marker<This> }
                CLASS Owner : Holder
                CLASS Marker<Holder> { This: Reward }
                CLASS Reward
                """,
                "Owner",
            )
            .testAgent(ADMIN)

    admin.count("Marker<Owner>") shouldBe 2
    admin.count("Reward") shouldBe 2
  }

  @Test
  internal fun abstractRequirementsDoNotChooseEvenTheOnlyConcretePart() {
    val world =
        world(
            """
            ABSTRACT CLASS Holder { HAS MAX 1 This }
            CLASS Owner : Holder { HAS =1 Choice<This> }
            ABSTRACT CLASS Choice<Holder>
            CLASS OnlyChoice : Choice
            """
        )
    val admin = world.testAgent(ADMIN)
    val before = world.timeline.checkpoint()

    shouldThrow<LimitsException> { admin.runOperation("Owner!") }

    world.timeline.checkpoint() shouldBe before
    admin.count("Owner") shouldBe 0
    admin.count("Choice") shouldBe 0
  }

  @Test
  internal fun minimumOnlyRequirementsDoNotConstructParts() {
    val world =
        world(
            """
            ABSTRACT CLASS Holder { HAS MAX 1 This }
            CLASS Owner : Holder { HAS 1 Marker<This> }
            CLASS Marker<Holder>
            """
        )
    val admin = world.testAgent(ADMIN)
    val before = world.timeline.checkpoint()

    shouldThrow<LimitsException> { admin.runOperation("Owner!") }

    world.timeline.checkpoint() shouldBe before
    admin.count("Owner") shouldBe 0
    admin.count("Marker") shouldBe 0
  }

  @Test
  internal fun requiredPartsDoNotInventTheirOtherDependencies() {
    val world =
        world(
            """
            ABSTRACT CLASS Anchor { HAS MAX 1 This }
            ABSTRACT CLASS Prerequisite { HAS MAX 1 This }
            CLASS Owner : Anchor { HAS =1 Part<This, Other> }
            CLASS Other : Prerequisite
            CLASS Part<Anchor, Prerequisite>
            """
        )
    val admin = world.testAgent(ADMIN)
    val before = world.timeline.checkpoint()

    shouldThrow<DependencyException> { admin.runOperation("Owner!") }
    world.timeline.checkpoint() shouldBe before
    admin.count("Other") shouldBe 0

    admin.runOperation("Other! THEN Owner!")
    admin.count("Part") shouldBe 1
  }

  @Test
  internal fun requiredPartsFollowDependenciesRegardlessOfInvariantOrder() {
    val admin =
        world(
                """
                ABSTRACT CLASS Holder {
                  HAS MAX 1 This, =1 Mounted<This, Bracket<This>>, =1 Bracket<This>
                }
                CLASS Owner : Holder
                CLASS Bracket<Holder> { HAS MAX 1 This }
                CLASS Mounted<Holder, Bracket>
                """
            )
            .testAgent(ADMIN)

    admin.runOperation("Owner!")

    admin.count("Bracket<Owner>") shouldBe 1
    admin.count("Mounted<Owner, Bracket<Owner>>") shouldBe 1
  }

  @Test
  internal fun nestedRequiredPartCanDependOnALaterSibling() {
    checkNestedRequiredPart("=1 Branch<This>, =1 Bracket<This>")
  }

  @Test
  internal fun nestedRequiredPartCanDependOnAnEarlierSibling() {
    checkNestedRequiredPart("=1 Bracket<This>, =1 Branch<This>")
  }

  private fun checkNestedRequiredPart(requirements: String) {
    val admin =
        world(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This }
                CLASS Owner : Holder { HAS $requirements }
                CLASS Branch<Holder> { HAS MAX 1 This, =1 Mounted<This, Bracket<Owner>> }
                CLASS Bracket<Holder> { HAS MAX 1 This }
                CLASS Mounted<Branch, Bracket>
                """
            )
            .testAgent(ADMIN)

    admin.runOperation("Owner!")

    admin.count("Branch") shouldBe 1
    admin.count("Bracket") shouldBe 1
    admin.count("Mounted") shouldBe 1
  }

  @Test
  internal fun nestedRequiredPartCanWaitForAnotherSiblingsRequiredPart() {
    val admin =
        world(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This }
                CLASS Owner : Holder { HAS =1 Branch<This>, =1 Bracket<This> }
                CLASS Branch<Holder> {
                  HAS MAX 1 This, =1 Mounted<This, Bolt<Bracket<Owner>>>
                }
                CLASS Bracket<Holder> { HAS MAX 1 This, =1 Bolt<This> }
                CLASS Bolt<Bracket> { HAS MAX 1 This }
                CLASS Mounted<Branch, Bolt>
                """
            )
            .testAgent(ADMIN)

    admin.runOperation("Owner!")

    admin.count("Branch") shouldBe 1
    admin.count("Bracket") shouldBe 1
    admin.count("Bolt") shouldBe 1
    admin.count("Mounted") shouldBe 1
  }

  @Test
  internal fun requiredPartReactsBeforeItsOwnerDisposesOfTheStructure() {
    checkPartReactsBeforeDisposal("HAS =1 Marker<This>")
  }

  @Test
  internal fun explicitlyConstructedPartReactsBeforeItsOwnerDisposesOfTheStructure() {
    checkPartReactsBeforeDisposal("This:: Marker<This>")
  }

  private fun checkPartReactsBeforeDisposal(construction: String) {
    val admin =
        world(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This }
                CLASS Owner : Holder {
                  $construction
                  This:: -This!
                }
                CLASS Marker<Holder> { HAS MAX 1 This; This:: Reward<This> }
                CLASS Reward<Marker> { This:: Observed }
                CLASS Observed
                """
            )
            .testAgent(ADMIN)

    admin.runOperation("Owner!")

    admin.count("Owner") shouldBe 0
    admin.count("Marker") shouldBe 0
    admin.count("Reward") shouldBe 0
    admin.count("Observed") shouldBe 1
  }

  @Test
  internal fun recursiveConstructionSharesTheEnclosingAutomaticEffectDepthLimit() {
    val world = constructionChainWorld(parts = 8)
    val admin = world.testAgent(ADMIN)
    val before = world.timeline.checkpoint()

    val failure = shouldThrow<RunawayEffectChainException> { admin.runOperation("Seed!") }

    failure.maximumDepth shouldBe 8
    failure.effectChain.size shouldBe 9
    failure.effectChain.first().instructions.single() shouldBe parse<Instruction>("Owner!")
    world.timeline.checkpoint() shouldBe before
    world.tasks.isEmpty() shouldBe true
    admin.count("Seed") shouldBe 0
    admin.count("Owner") shouldBe 0
    admin.count("Part1") shouldBe 0

    // Without the enclosing Seed effect, the same construction fits the depth budget.
    admin.runOperation("Owner!")
    admin.count("Part8") shouldBe 1
  }

  @Test
  internal fun constructionAtTheCombinedDepthLimitSucceeds() {
    val admin = constructionChainWorld(parts = 7).testAgent(ADMIN)

    admin.runOperation("Seed!")

    admin.count("Owner") shouldBe 1
    admin.count("Part7") shouldBe 1
  }

  private fun constructionChainWorld(parts: Int): World {
    val declarations =
        (1..parts).joinToString("\n") { index ->
          val dependency = if (index == 1) "Holder" else "Part${index - 1}"
          val requirement = if (index < parts) ", =1 Part${index + 1}<This>" else ""
          "CLASS Part$index<$dependency> { HAS MAX 1 This$requirement }"
        }
    return world(
        """
        ABSTRACT CLASS Holder { HAS MAX 1 This }
        CLASS Seed { This:: Owner! }
        CLASS Owner : Holder { HAS =1 Part1<This> }
        $declarations
        """
    )
  }

  @Test
  internal fun requiredPartsAreRecursiveAndPresentBeforeOwnerEffects() {
    val admin =
        world(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This }
                CLASS Owner : Holder {
                  HAS MAX 1 This, =1 Bracket<This>
                  This IF =2 Bolt<Bracket<This>>:: Reward
                  This IF =2 ReadyBolt:: AutomaticallyObserved
                  This IF =2 ReadyBolt: Observed
                }
                CLASS Bracket<Holder> { HAS MAX 1 This, =2 Bolt<This> }
                CLASS Bolt<Bracket> { This:: ReadyBolt }
                CLASS ReadyBolt
                CLASS Observed
                CLASS AutomaticallyObserved
                CLASS Reward
                """
            )
            .testAgent(ADMIN)

    admin.runOperation("Owner!")

    admin.count("Bolt<Bracket<Owner>>") shouldBe 2
    admin.count("Reward") shouldBe 1
    admin.count("Observed") shouldBe 1
    admin.count("AutomaticallyObserved") shouldBe 1
    admin.runOperation("-Owner!")
    admin.count("Bracket") shouldBe 0
    admin.count("Bolt") shouldBe 0
  }

  @Test
  internal fun requiredListenerObservesSiblingWhenDeclaredFirst() {
    checkRequiredSiblingListener("=1 Listener<This>, =1 Marker<This>")
  }

  @Test
  internal fun requiredListenerObservesSiblingWhenDeclaredLast() {
    checkRequiredSiblingListener("=1 Marker<This>, =1 Listener<This>")
  }

  private fun checkRequiredSiblingListener(requirements: String) {
    val admin =
        world(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This }
                CLASS Owner : Holder { HAS $requirements }
                CLASS Marker<Holder> { This IF =1 Listener<Holder>:: Snapshot }
                CLASS Listener<Holder> { Marker<Holder>: Reward }
                CLASS Snapshot
                CLASS Reward
                """
            )
            .testAgent(ADMIN)

    admin.runOperation("Owner!")

    admin.count("Marker") shouldBe 1
    admin.count("Listener") shouldBe 1
    admin.count("Snapshot") shouldBe 1
    admin.count("Reward") shouldBe 1
  }

  @Test
  internal fun overlappingRequiredPartsAreConstructedOnlyOnce() {
    val admin =
        world(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This }
                CLASS Owner : Holder {
                  HAS =1 Bracket<This>, =2 Bolt<This, Bracket<This>>
                }
                CLASS Bracket<Holder> { HAS MAX 1 This, =2 Bolt<Owner, This> }
                CLASS Bolt<Holder, Bracket> { This: Reward }
                CLASS Reward
                """
            )
            .testAgent(ADMIN)

    admin.runOperation("Owner!")

    admin.count("Bolt<Owner>") shouldBe 2
    admin.count("Reward") shouldBe 2
  }

  @Test
  internal fun requiredAtomizedPartsKeepSeparateGainEvents() {
    val admin =
        world(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This }
                CLASS Owner : Holder { HAS MAX 1 This, =2 Marker<This> }
                CLASS Marker<Holder> : Atomized { This IF =2 Marker<Holder>: Reward }
                CLASS Reward
                """
            )
            .testAgent(ADMIN)

    val result = admin.runOperation("Owner!")

    admin.count("Marker") shouldBe 2
    admin.count("Reward") shouldBe 2
    result.changes
        .filter { it.change.gaining?.type?.className == cn("Marker") }
        .map { it.change.count } shouldBe listOf(1, 1)
  }

  @Test
  internal fun failedConstructionRestoresPartsEffectsTasksAndHistory() {
    val world =
        world(
            """
            ABSTRACT CLASS Holder { HAS MAX 1 This }
            CLASS Owner : Holder { HAS =1 Notifier<This>, =1 Part<This> }
            CLASS Notifier<Holder> {
              This:: Progress
              This: Reward
            }
            CLASS Part<Holder> {
              HAS MAX 1 This, =1 MissingPart<This, Other>
              Pulse: Reward
            }
            ABSTRACT CLASS Prerequisite { HAS MAX 1 This }
            CLASS Other : Prerequisite
            CLASS MissingPart<Part, Prerequisite>
            CLASS Pulse : Signal
            CLASS Progress
            CLASS Reward
            """
        )
    val admin = world.testAgent(ADMIN)
    val before = world.timeline.checkpoint()

    shouldThrow<DependencyException> { admin.runOperation("Owner!") }

    world.timeline.checkpoint() shouldBe before
    world.tasks.isEmpty() shouldBe true
    admin.count("Owner") shouldBe 0
    admin.count("Notifier") shouldBe 0
    admin.count("Part") shouldBe 0
    admin.count("Progress") shouldBe 0
    admin.runOperation("Pulse!")
    admin.count("Reward") shouldBe 0
    admin.runOperation("Other! THEN Owner! THEN Pulse!")
    admin.count("MissingPart") shouldBe 1
    admin.count("Reward") shouldBe 2
  }

  @Test
  internal fun transmutingAnOwnerReplacesItsRequiredParts() {
    val admin =
        world(
                """
                ABSTRACT CLASS Owner { HAS MAX 1 This, =1 Part<This> }
                CLASS First : Owner
                CLASS Second : Owner
                CLASS Part<Owner> { This: Reward }
                CLASS Reward
                """
            )
            .testAgent(ADMIN)
    admin.runOperation("First!")

    admin.runOperation("Second FROM First!")

    admin.count("Part<First>") shouldBe 0
    admin.count("Part<Second>") shouldBe 1
    admin.count("Reward") shouldBe 2
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
