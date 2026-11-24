package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.ResourceLimitException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.count
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlin.test.Test

internal class InvariantCompletionTest {
  @Test
  internal fun bootstrapChecksMinimumsAfterAllInitialComponentsArePresent() {
    val admin =
        Agents(
            world(
                """
                ABSTRACT CLASS Status { HAS =2 Status }
                CLASS First : Status
                CLASS Second : Status
                """,
                "First",
                "Second",
            )
        )

    admin.world.reader.count("Status") shouldBe 2
  }

  @Test
  internal fun removingTheLastGloballyRequiredComponentRollsBack() {
    val world =
        world(
            """
            CLASS Rule { HAS =1 Token }
            CLASS Token
            CLASS Unrelated { HAS =1 This }
            """,
            "Token",
            "Unrelated",
        )
    val admin = Agents(world)
    val task =
        world
            .actorEngine(ADMIN)
            .addTasks(InstructionGroup.of(parse<Instruction>("-Token!")))
            .single()
    val before = world.events.nextOrdinal

    admin.world.actorEngine(ADMIN).canSelectTask(task) shouldBe false
    world.events.nextOrdinal shouldBe before
    shouldThrow<LimitsException> { admin.world.actorEngine(ADMIN).doTask(Parsing.parse("-Token!")) }

    world.events.nextOrdinal shouldBe before
    admin.world.reader.count("Token") shouldBe 1
    admin.world.reader.count("Unrelated") shouldBe 1
  }

  @Test
  internal fun transmutationPreservesEachConcreteInheritedMinimum() {
    val world =
        world(
            """
            ABSTRACT CLASS Required { HAS 1 This }
            CLASS First : Required
            CLASS Second : Required
            """,
            "First",
            "Second",
        )
    val admin = Agents(world)
    val before = world.events.nextOrdinal

    shouldThrow<LimitsException> { admin.runSystem("Second FROM First!") }

    world.events.nextOrdinal shouldBe before
    admin.world.reader.count("First") shouldBe 1
    admin.world.reader.count("Second") shouldBe 1
  }

  @Test
  internal fun inheritedReplacementEffectKeepsExactlyOneFamilyMember() {
    val admin = Agents(replacementWorld())

    admin.runSystem("Passed!")

    admin.world.reader.count("Ready") shouldBe 0
    admin.world.reader.count("Passed") shouldBe 1
    admin.runSystem("Ready!")
    admin.world.reader.count("Ready") shouldBe 1
    admin.world.reader.count("Passed") shouldBe 0
  }

  @Test
  internal fun familyMaximumRejectsRepeatedValueWithoutAPerTypeMaximum() {
    val world = replacementWorld()
    val admin = Agents(world)
    val before = world.events.nextOrdinal

    shouldThrow<LimitsException> { admin.runSystem("Ready!") }

    world.events.nextOrdinal shouldBe before
    admin.world.reader.count("Ready") shouldBe 1
  }

  @Test
  internal fun gainThenRemovePreservesExactOneAcrossRecursiveEffects() {
    val world = statusWorld("This:: Notice!", "This:: -Ready!")
    val admin = Agents(world)

    admin.runSystem("Passed!")

    admin.world.reader.count("Ready") shouldBe 0
    admin.world.reader.count("Passed") shouldBe 1
    admin.world.reader.count("Status") shouldBe 1
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
    val admin = Agents(world)

    admin.runSystem("-Ready!")

    admin.world.reader.count("Ready") shouldBe 0
    admin.world.reader.count("Passed") shouldBe 1
  }

  @Test
  internal fun failedRepairRestoresComponentsTasksHistoryAndEffects() {
    val world = statusWorld("This:: Notice!", "This: -Ready!")
    val admin = Agents(world)
    val before = world.events.nextOrdinal

    shouldThrow<LimitsException> { admin.runSystem("Passed!") }

    world.events.nextOrdinal shouldBe before
    world.tasks.isEmpty() shouldBe true
    admin.world.reader.count("Ready") shouldBe 1
    admin.world.reader.count("Passed") shouldBe 0
    admin.world.reader.count("Notice") shouldBe 0
    admin.runSystem("Pulse!")
    admin.world.reader.count("Reward") shouldBe 0
  }

  @Test
  internal fun amapKeepsItsCapAndTreatsNegativeHeadroomAsZero() {
    val world =
        world(
            """
            CLASS Token {
              HAS MAX 1 This
              This IF =2 Token:: 5* Token, -Token!
            }
            """
        )
    val admin = Agents(world)
    admin.runSystem("5* Token")
    admin.world.reader.count("Token") shouldBe 1

    val result = admin.runSystem("Token!")

    admin.world.reader.count("Token") shouldBe 1
    result.changes.size shouldBe 2
  }

  @Test
  internal fun amapDoesNotInferATargetByExcludingAZeroCapacitySubclass() {
    checkZeroCapacityChoice("*")
  }

  @Test
  internal fun optionalDoesNotInferATargetByExcludingAZeroCapacitySubclass() {
    checkZeroCapacityChoice("?")
  }

  private fun checkZeroCapacityChoice(quantifier: String) {
    val premise =
        testGamePremise(
                """
                CLASS Holder { HAS MAX 1 This }
                ABSTRACT CLASS Token
                CLASS Capped<Holder> : Token { HAS MAX 0 This }
                CLASS Available<Holder> : Token
                """
            )
            .withTestSetup("Holder")
    val player =
        Agents(Engine.newGame(premise)) { EAGER }[PLAYER1].also { it.autoExecPolicy = NONE }

    player.runOperation("Token$quantifier") { doTask("Capped$quantifier") }

    player.count("Token") shouldBe 0
  }

  @Test
  internal fun failedExecutionWithNarrowingRestoresThePendingTaskAndCascade() {
    val world = statusWorld("This:: Notice!")
    val admin = Agents(world)
    val engine = world.actorEngine(ADMIN)
    val task = engine.addTasks(InstructionGroup.of(parse<Instruction>("Status!"))).single()
    val pending = admin.world.actorEngine(ADMIN).tasks.getTaskData(task)
    val before = world.events.nextOrdinal

    shouldThrow<LimitsException> { engine.doTask(parse<Instruction>("Passed!"), taskId = task) }

    // Use the raw engine so later automatic Admin submissions cannot obscure restoration.
    world.events.nextOrdinal shouldBe before
    world.events.changesSince(before) shouldBe emptyList()
    admin.world.actorEngine(ADMIN).tasks.getTaskData(task).instruction shouldBe pending.instruction
    admin.world.reader.count("Ready") shouldBe 1
    admin.world.reader.count("Passed") shouldBe 0
    admin.world.reader.count("Notice") shouldBe 0
  }

  @Test
  internal fun adminSelectionExecutesTheUniqueSuccessfulAlternative() {
    val world =
        world(
            """
                CLASS Guarded { This:: -Missing! }
                CLASS Missing
                CLASS Other
                """
        )
    val admin = Agents(world)
    val engine = world.actorEngine(ADMIN)
    val task =
        engine.addTasks(InstructionGroup.of(parse<Instruction>("Guarded! OR Other!"))).single()

    engine.selectTask(task)

    admin.world.reader.count("Other") shouldBe 1
    (task in world.tasks) shouldBe false
  }

  @Test
  internal fun choiceKeepsAnArmWhoseCascadeRepairsTheLimit() {
    val world = statusWorld("This:: -Ready!")
    val admin = Agents(world)

    admin.runSystem("Passed! OR Die!")

    admin.world.reader.count("Passed") shouldBe 1
    admin.world.reader.count("Ready") shouldBe 0
  }

  @Test
  internal fun choiceAllowsADeadEndAndRollsItBackBeforeAnotherChoice() {
    val world = statusWorld("This:: Notice!")
    val admin = Agents(world)

    val engine = world.actorEngine(ADMIN)
    val task =
        engine.addTasks(InstructionGroup.of(parse<Instruction>("Passed! OR Reward!"))).single()
    val before = world.events.nextOrdinal
    shouldThrow<LimitsException> { engine.doTask(parse<Instruction>("Passed!"), taskId = task) }
    world.events.nextOrdinal shouldBe before
    admin.world.reader.count("Notice") shouldBe 0
    admin.world.reader.count("Passed") shouldBe 0
    admin.transact()

    admin.world.reader.count("Passed") shouldBe 0
    admin.world.reader.count("Ready") shouldBe 1
    admin.world.reader.count("Notice") shouldBe 0
    admin.world.reader.count("Reward") shouldBe 1
  }

  @Test
  internal fun abstractGainCanSelectAConcreteRepair() {
    val world = statusWorld("This:: -Ready!")
    val admin = Agents(world)

    val engine = world.actorEngine(ADMIN)
    val task = engine.addTasks(InstructionGroup.of(parse<Instruction>("Status!"))).single()
    engine.doTask(task)

    admin.world.reader.count("Passed") shouldBe 1
    admin.world.reader.count("Ready") shouldBe 0
  }

  @Test
  internal fun intermediateStatesRemainVisibleToNestedQueuedConditions() {
    val world = statusWorld("This:: Notice!, -Ready!", "This IF =1 Status: Reward!")
    val admin = Agents(world)

    admin.runSystem("Passed!")

    admin.world.reader.count("Status") shouldBe 1
    admin.world.reader.count("Reward") shouldBe 0
  }

  @Test
  internal fun sourceAvailabilityCannotBeRepairedAfterRemoval() {
    val world = world("CLASS Token { -This:: Token! }")
    val admin = Agents(world)
    val before = world.events.nextOrdinal

    shouldThrow<LimitsException> { admin.runSystem("-Token!") }

    world.events.nextOrdinal shouldBe before
    admin.world.reader.count("Token") shouldBe 0
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
    val admin = Agents(world)
    val before = world.events.nextOrdinal

    shouldThrow<DependencyException> { admin.runSystem("Attached!") }

    world.events.nextOrdinal shouldBe before
    admin.world.reader.count("Anchor") shouldBe 0
    admin.world.reader.count("Attached") shouldBe 0
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
    val admin = Agents(world)
    admin.runSystem("FirstAnchor!")
    val before = world.events.nextOrdinal

    shouldThrow<LimitsException> { admin.runSystem("-Attached!") }

    world.events.nextOrdinal shouldBe before
    admin.world.reader.count("Attached") shouldBe 1
    admin.runSystem("-FirstAnchor!")
    admin.world.reader.count("Bridge") shouldBe 0
    admin.world.reader.count("Attached") shouldBe 0
  }

  @Test
  internal fun gainingAnOwnerEstablishesItsInheritedRequiredParts() {
    val admin =
        Agents(
            world(
                """
                ABSTRACT CLASS Anchor { HAS MAX 1 This, =2 Marker<This> }
                CLASS First : Anchor
                CLASS Second : Anchor
                CLASS Marker<Anchor> { This: Reward }
                CLASS Reward
                """
            )
        )

    admin.runSystem("First!, Second!")
    admin.world.reader.count("Marker<First>") shouldBe 2
    admin.world.reader.count("Marker<Second>") shouldBe 2
    admin.world.reader.count("Reward") shouldBe 4

    shouldThrow<LimitsException> { admin.runSystem("-Marker<First>!") }
    admin.world.reader.count("Marker<First>") shouldBe 2
    admin.runSystem("-First!")
    admin.world.reader.count("Marker<First>") shouldBe 0
    admin.world.reader.count("Marker<Second>") shouldBe 2
  }

  @Test
  internal fun bootstrapConstructsRequiredPartsAndRunsTheirEffects() {
    val admin =
        Agents(
            world(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This, =2 Marker<This> }
                CLASS Assembly : Holder
                CLASS Marker<Holder> { This: Reward }
                CLASS Reward
                """,
                "Assembly",
            )
        )

    admin.world.reader.count("Marker<Assembly>") shouldBe 2
    admin.world.reader.count("Reward") shouldBe 2
  }

  @Test
  internal fun abstractRequirementsDoNotChooseEvenTheOnlyConcretePart() {
    val world =
        world(
            """
            ABSTRACT CLASS Holder { HAS MAX 1 This }
            CLASS Assembly : Holder { HAS =1 Choice<This> }
            ABSTRACT CLASS Choice<Holder>
            CLASS OnlyChoice : Choice
            """
        )
    val admin = Agents(world)
    val before = world.events.nextOrdinal

    shouldThrow<LimitsException> { admin.runSystem("Assembly!") }

    world.events.nextOrdinal shouldBe before
    admin.world.reader.count("Assembly") shouldBe 0
    admin.world.reader.count("Choice") shouldBe 0
  }

  @Test
  internal fun minimumOnlyRequirementsDoNotConstructParts() {
    val world =
        world(
            """
            ABSTRACT CLASS Holder { HAS MAX 1 This }
            CLASS Assembly : Holder { HAS 1 Marker<This> }
            CLASS Marker<Holder>
            """
        )
    val admin = Agents(world)
    val before = world.events.nextOrdinal

    shouldThrow<LimitsException> { admin.runSystem("Assembly!") }

    world.events.nextOrdinal shouldBe before
    admin.world.reader.count("Assembly") shouldBe 0
    admin.world.reader.count("Marker") shouldBe 0
  }

  @Test
  internal fun requiredPartsDoNotInventTheirOtherDependencies() {
    val world =
        world(
            """
            ABSTRACT CLASS Anchor { HAS MAX 1 This }
            ABSTRACT CLASS Prerequisite { HAS MAX 1 This }
            CLASS Assembly : Anchor { HAS =1 Part<This, Other> }
            CLASS Other : Prerequisite
            CLASS Part<Anchor, Prerequisite>
            """
        )
    val admin = Agents(world)
    val before = world.events.nextOrdinal

    shouldThrow<DependencyException> { admin.runSystem("Assembly!") }
    world.events.nextOrdinal shouldBe before
    admin.world.reader.count("Other") shouldBe 0

    admin.runSystem("Other! THEN Assembly!")
    admin.world.reader.count("Part") shouldBe 1
  }

  @Test
  internal fun requiredPartsFollowDependenciesRegardlessOfInvariantOrder() {
    val admin =
        Agents(
            world(
                """
                ABSTRACT CLASS Holder {
                  HAS MAX 1 This, =1 Mounted<This, Bracket<This>>, =1 Bracket<This>
                }
                CLASS Assembly : Holder
                CLASS Bracket<Holder> { HAS MAX 1 This }
                CLASS Mounted<Holder, Bracket>
                """
            )
        )

    admin.runSystem("Assembly!")

    admin.world.reader.count("Bracket<Assembly>") shouldBe 1
    admin.world.reader.count("Mounted<Assembly, Bracket<Assembly>>") shouldBe 1
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
        Agents(
            world(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This }
                CLASS Assembly : Holder { HAS $requirements }
                CLASS Branch<Holder> { HAS MAX 1 This, =1 Mounted<This, Bracket<Assembly>> }
                CLASS Bracket<Holder> { HAS MAX 1 This }
                CLASS Mounted<Branch, Bracket>
                """
            )
        )

    admin.runSystem("Assembly!")

    admin.world.reader.count("Branch") shouldBe 1
    admin.world.reader.count("Bracket") shouldBe 1
    admin.world.reader.count("Mounted") shouldBe 1
  }

  @Test
  internal fun nestedRequiredPartCanWaitForAnotherSiblingsRequiredPart() {
    val admin =
        Agents(
            world(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This }
                CLASS Assembly : Holder { HAS =1 Branch<This>, =1 Bracket<This> }
                CLASS Branch<Holder> {
                  HAS MAX 1 This, =1 Mounted<This, Bolt<Bracket<Assembly>>>
                }
                CLASS Bracket<Holder> { HAS MAX 1 This, =1 Bolt<This> }
                CLASS Bolt<Bracket> { HAS MAX 1 This }
                CLASS Mounted<Branch, Bolt>
                """
            )
        )

    admin.runSystem("Assembly!")

    admin.world.reader.count("Branch") shouldBe 1
    admin.world.reader.count("Bracket") shouldBe 1
    admin.world.reader.count("Bolt") shouldBe 1
    admin.world.reader.count("Mounted") shouldBe 1
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
        Agents(
            world(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This }
                CLASS Assembly : Holder {
                  DEFAULT -Assembly?
                  $construction
                  This:: -This
                }
                CLASS Marker<Holder> { HAS MAX 1 This; This:: Reward<This> }
                CLASS Reward<Marker> { This:: Observed }
                CLASS Observed
                """
            )
        )

    admin.runSystem("Assembly!")

    admin.world.reader.count("Assembly") shouldBe 0
    admin.world.reader.count("Marker") shouldBe 0
    admin.world.reader.count("Reward") shouldBe 0
    admin.world.reader.count("Observed") shouldBe 1
  }

  @Test
  internal fun recursiveConstructionSharesTheEnclosingAutomaticEffectDepthLimit() {
    val world = constructionChainWorld(parts = 8)
    val admin = Agents(world)
    val before = world.events.nextOrdinal

    val failure = shouldThrow<ResourceLimitException> { admin.runSystem("Seed!") }

    failure.detail shouldStartWith "automatic effect chain exceeded depth 8: Assembly! -> "
    world.events.nextOrdinal shouldBe before
    world.tasks.isEmpty() shouldBe true
    admin.world.reader.count("Seed") shouldBe 0
    admin.world.reader.count("Assembly") shouldBe 0
    admin.world.reader.count("Part1") shouldBe 0

    // Without the enclosing Seed effect, the same construction fits the depth budget.
    admin.runSystem("Assembly!")
    admin.world.reader.count("Part8") shouldBe 1
  }

  @Test
  internal fun constructionAtTheCombinedDepthLimitSucceeds() {
    val admin = Agents(constructionChainWorld(parts = 7))

    admin.runSystem("Seed!")

    admin.world.reader.count("Assembly") shouldBe 1
    admin.world.reader.count("Part7") shouldBe 1
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
        CLASS Seed { This:: Assembly! }
        CLASS Assembly : Holder { HAS =1 Part1<This> }
        $declarations
        """
    )
  }

  @Test
  internal fun requiredPartsAreRecursiveAndPresentBeforeOwnerEffects() {
    val admin =
        Agents(
            world(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This }
                CLASS Assembly : Holder {
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
        )

    admin.runSystem("Assembly!")

    admin.world.reader.count("Bolt<Bracket<Assembly>>") shouldBe 2
    admin.world.reader.count("Reward") shouldBe 1
    admin.world.reader.count("Observed") shouldBe 1
    admin.world.reader.count("AutomaticallyObserved") shouldBe 1
    admin.runSystem("-Assembly!")
    admin.world.reader.count("Bracket") shouldBe 0
    admin.world.reader.count("Bolt") shouldBe 0
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
        Agents(
            world(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This }
                CLASS Assembly : Holder { HAS $requirements }
                CLASS Marker<Holder> { This IF =1 Listener<Holder>:: Snapshot }
                CLASS Listener<Holder> { Marker<Holder>: Reward }
                CLASS Snapshot
                CLASS Reward
                """
            )
        )

    admin.runSystem("Assembly!")

    admin.world.reader.count("Marker") shouldBe 1
    admin.world.reader.count("Listener") shouldBe 1
    admin.world.reader.count("Snapshot") shouldBe 1
    admin.world.reader.count("Reward") shouldBe 1
  }

  @Test
  internal fun overlappingRequiredPartsAreConstructedOnlyOnce() {
    val admin =
        Agents(
            world(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This }
                CLASS Assembly : Holder {
                  HAS =1 Bracket<This>, =2 Bolt<This, Bracket<This>>
                }
                CLASS Bracket<Holder> { HAS MAX 1 This, =2 Bolt<Assembly, This> }
                CLASS Bolt<Holder, Bracket> { This: Reward }
                CLASS Reward
                """
            )
        )

    admin.runSystem("Assembly!")

    admin.world.reader.count("Bolt<Assembly>") shouldBe 2
    admin.world.reader.count("Reward") shouldBe 2
  }

  @Test
  internal fun requiredAtomizedPartsKeepSeparateGainEvents() {
    val admin =
        Agents(
            world(
                """
                ABSTRACT CLASS Holder { HAS MAX 1 This }
                CLASS Assembly : Holder { HAS MAX 1 This, =2 Marker<This> }
                CLASS Marker<Holder> : Atomized { This IF =2 Marker<Holder>: Reward }
                CLASS Reward
                """
            )
        )

    val result = admin.runSystem("Assembly!")

    admin.world.reader.count("Marker") shouldBe 2
    admin.world.reader.count("Reward") shouldBe 2
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
            CLASS Assembly : Holder { HAS =1 Notifier<This>, =1 Part<This> }
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
    val admin = Agents(world)
    val before = world.events.nextOrdinal

    shouldThrow<DependencyException> { admin.runSystem("Assembly!") }

    world.events.nextOrdinal shouldBe before
    world.tasks.isEmpty() shouldBe true
    admin.world.reader.count("Assembly") shouldBe 0
    admin.world.reader.count("Notifier") shouldBe 0
    admin.world.reader.count("Part") shouldBe 0
    admin.world.reader.count("Progress") shouldBe 0
    admin.runSystem("Pulse!")
    admin.world.reader.count("Reward") shouldBe 0
    admin.runSystem("Other! THEN Assembly! THEN Pulse!")
    admin.world.reader.count("MissingPart") shouldBe 1
    admin.world.reader.count("Reward") shouldBe 2
  }

  @Test
  internal fun transmutingAnOwnerReplacesItsRequiredParts() {
    val admin =
        Agents(
            world(
                """
                ABSTRACT CLASS Assembly { HAS MAX 1 This, =1 Part<This> }
                CLASS First : Assembly
                CLASS Second : Assembly
                CLASS Part<Assembly> { This: Reward }
                CLASS Reward
                """
            )
        )
    admin.runSystem("First!")

    admin.runSystem("Second FROM First!")

    admin.world.reader.count("Part<First>") shouldBe 0
    admin.world.reader.count("Part<Second>") shouldBe 1
    admin.world.reader.count("Reward") shouldBe 2
  }

  @Test
  internal fun separateInitiatingChangesCannotRepairEachOther() {
    val world = statusWorld("")
    val admin = Agents(world)
    val before = world.events.nextOrdinal

    shouldThrow<LimitsException> { admin.runSystem("Passed! THEN -Ready!") }

    world.events.nextOrdinal shouldBe before
    admin.world.reader.count("Ready") shouldBe 1
    admin.world.reader.count("Passed") shouldBe 0
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
    val admin = Agents(world)

    admin.runSystem("Passed!")

    admin.world.reader.count("Status") shouldBe 1
    admin.world.reader.count("Reward") shouldBe 1
  }

  @Test
  internal fun amapRemovalClampsNegativeFootroomDuringARepair() {
    val world =
        world(
            """
            ABSTRACT CLASS Status { HAS =1 Status }
            CLASS Ready : Status { -This:: -Ready*, Passed! }
            CLASS Passed : Status
            """,
            "Ready",
        )
    val admin = Agents(world)

    val result = admin.runSystem("-Ready!")

    admin.world.reader.count("Passed") shouldBe 1
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
    val admin = Agents(world)

    val engine = world.actorEngine(ADMIN)
    val task = engine.addTasks(InstructionGroup.of(parse<Instruction>("-Token!"))).single()
    engine.doTask(task)

    admin.world.reader.count("Token") shouldBe 2
    admin.world.reader.count("First") shouldBe 0
    admin.world.reader.count("Replacement") shouldBe 1
  }

  @Test
  internal fun abstractGainSelectsTheUniqueRepairAfterRejectingTheBrokenConcreteRequest() {
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
    val admin = Agents(world)

    val engine = world.actorEngine(ADMIN)
    val task = engine.addTasks(InstructionGroup.of(parse<Instruction>("Status!"))).single()
    admin.world.reader.count("Passed") shouldBe 0
    val before = world.events.nextOrdinal
    shouldThrow<LimitsException> { engine.doTask(parse<Instruction>("Broken!"), taskId = task) }
    world.events.nextOrdinal shouldBe before
    engine.doTask(task)

    admin.world.reader.count("Ready") shouldBe 0
    admin.world.reader.count("Passed") shouldBe 1
    admin.world.reader.count("Broken") shouldBe 0
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
    val admin = Agents(world)

    admin.runSystem("Passed!")

    admin.world.reader.count("Passed") shouldBe 1
    admin.world.reader.count("Status") shouldBe 1
    admin.world.reader.count("Extra") shouldBe 0
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
    val admin = Agents(world)
    val before = world.events.nextOrdinal

    shouldThrow<NotFullySpecifiedException> { admin.runSystem("Start!") }

    world.events.nextOrdinal shouldBe before
    admin.world.reader.count("Start") shouldBe 0
    admin.world.reader.count("Token") shouldBe 1
    admin.world.reader.count("Other") shouldBe 0
  }

  @Test
  internal fun ordinaryAdminWorkExecutesItsOnlySuccessfulAlternative() {
    val world =
        world(
            """
            CLASS Token { HAS MAX 1 This }
            CLASS Other
            CLASS Start { This: Token! OR Other! }
            """,
            "Token",
        )
    val admin = Agents(world)

    admin.runSystem("Start!")

    admin.world.reader.count("Start") shouldBe 1
    admin.world.reader.count("Token") shouldBe 1
    admin.world.reader.count("Other") shouldBe 1
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
    val world = actorDependentWorld()
    val before = world.events.nextOrdinal
    val admin = Agents(world) { EAGER }

    shouldThrow<LimitsException> { admin.runSystem("Passed! OR Die!") }

    world.events.nextOrdinal shouldBe before
    admin.world.reader.count("Ready") shouldBe 1
    admin.world.reader.count("Passed") shouldBe 0
  }

  private fun checkPerformerDependentRepair(overrideActor: Boolean) {
    val world = actorDependentWorld()
    val agents = Agents(world) { EAGER }
    if (overrideActor) agents.runSystem("(Passed! BY Player1) OR Die!")
    else agents[PLAYER1].runOperation("Passed! OR Die!")
    world.reader.count("Passed") shouldBe 1
    world.reader.count("Ready") shouldBe 0
  }

  private fun actorDependentWorld(): World {
    val source =
        """
        ABSTRACT CLASS Status { HAS =1 Status }
        CLASS Ready : Status
        CLASS Passed : Status { This BY Player1:: -Ready! }
        CLASS Other
        """
    val premise = testGamePremise(source).withTestSetup("Ready")
    return Engine.newGame(premise)
  }

  private fun replacementWorld() =
      world(
          """
          ABSTRACT CLASS Status {
            HAS =1 Status
            This:: -Status(NOT This)*
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
      Engine.newGame(testGamePremise(source, players = 0).withTestSetup(initial.joinToString()))
}
