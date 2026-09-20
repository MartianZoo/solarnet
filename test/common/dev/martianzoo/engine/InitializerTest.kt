package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldInclude
import io.kotest.matchers.string.shouldNotInclude
import kotlin.test.Test

internal class InitializerTest {
  @Test
  internal fun bootstrapLayersAfterAdminUseOrdinaryTasks() {
    val premise =
        testGamePremise("CLASS BootstrapPhase\nCLASS Premise", players = 0)
            .copy(
                bootstrapClassName = cn("BootstrapPhase"),
                premiseClassName = cn("Premise"),
            )

    val events = Engine.newGame(premise).events.entriesSince(Checkpoint(0))

    events.map { it::class } shouldBe
        listOf(
            ChangeEvent::class,
            TaskAddedEvent::class,
            TaskEditedEvent::class,
            ChangeEvent::class,
            TaskRemovedEvent::class,
            TaskAddedEvent::class,
            TaskEditedEvent::class,
            ChangeEvent::class,
            TaskRemovedEvent::class,
        )
    (events[0] as ChangeEvent).change.gaining?.className shouldBe cn("Admin")
    (events[3] as ChangeEvent).change.gaining?.className shouldBe cn("BootstrapPhase")
    (events[7] as ChangeEvent).change.gaining?.className shouldBe cn("Premise")
  }

  @Test
  internal fun bootstrapCauseUsesItsFirstChangeWhenImmediateEffectsFollow() {
    val premise =
        testGamePremise(
                """
                CLASS BootstrapPhase { This:: Marker }
                CLASS Marker
                """,
                players = 0,
            )
            .copy(bootstrapClassName = cn("BootstrapPhase"))

    val game = Engine.newGame(premise)

    game.reader.count(game.classTable.resolve(cn("BootstrapPhase").expression)) shouldBe 1
    game.reader.count(game.classTable.resolve(cn("Marker").expression)) shouldBe 1
  }

  @Test
  internal fun generatedPremiseDoesNotFallThroughToDirectCreation() {
    val premise =
        testGamePremise(
                """
                CLASS BootstrapProbe { HAS =1 This }
                CLASS BrokenPremise { HAS =1 This }
                """,
                players = 1,
            )
            .copy(
                initialComponentTypes = setOf(cn("BootstrapProbe").expression),
                premiseClassName = cn("BrokenPremise"),
            )

    val failure = shouldThrow<InvalidGameConfigException> { Engine.newGame(premise) }

    failure.message.orEmpty().shouldInclude("Player1 (found 0)")
    failure.message.orEmpty().shouldInclude("BootstrapProbe (found 0)")
  }

  @Test
  internal fun directPremiseCreatesPlayersAndInitialComponentsWithoutAGeneratedRecipe() {
    val premise =
        testGamePremise("CLASS BootstrapProbe { HAS =1 This }", players = 2)
            .copy(initialComponentTypes = setOf(cn("BootstrapProbe").expression))

    val game = Engine.newGame(premise)
    val admin = game.testAgent(ADMIN)

    premise.premiseClassName shouldBe null
    admin.count("Player") shouldBe 2
    admin.count("BootstrapProbe") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun completedBootstrapRejectsAMissingPositiveLowerBound() {
    val premise = testGamePremise("CLASS RequiredAtBootstrap { HAS =1 This }", players = 0)

    val failure = shouldThrow<InvalidGameConfigException> { Engine.newGame(premise) }

    failure.message.orEmpty().shouldInclude("RequiredAtBootstrap (found 0, expected 1)")
  }

  @Test
  internal fun uninhabitedConcreteClassDoesNotImposeAPositiveLowerBound() {
    val premise =
        testGamePremise(
            """
            ABSTRACT CLASS Empty
            CLASS Holder<Empty> { HAS =1 This }
            CLASS Live
            """,
            players = 0,
        )

    val game = Engine.newGame(premise)
    game.classTable.isInhabited(cn("Holder")) shouldBe false
    game.reader.count(game.classTable.resolve(cn("Holder").expression)) shouldBe 0
  }

  @Test
  internal fun completedBootstrapChecksDependentLowerBoundsPerLiveScope() {
    val premise =
        testGamePremise(
                """
                ABSTRACT CLASS Anchor {
                  HAS MAX 1 This
                  HAS =1 Marker<This>
                  CLASS Left
                  CLASS Right
                  CLASS Absent
                }
                CLASS Marker<Anchor>
                """,
                players = 0,
            )
            .copy(
                initialComponentTypes =
                    setOf(
                        cn("Left").expression,
                        cn("Right").expression,
                        cn("Marker").of(cn("Left").expression),
                    )
            )

    val failure = shouldThrow<InvalidGameConfigException> { Engine.newGame(premise) }

    failure.message.orEmpty().shouldInclude("Marker<Right> (found 0, expected 1)")
    failure.message.orEmpty().shouldNotInclude("Marker<Left>")
    failure.message.orEmpty().shouldNotInclude("Marker<Absent>")
  }

  @Test
  internal fun bootstrapRejectsTaskWithMultipleConcreteNarrowings() {
    val premise =
        testGamePremise(
                """
                ABSTRACT CLASS Choice {
                  CLASS Left
                  CLASS Right
                }
                CLASS BootstrapProbe { This: Choice }
                """,
                players = 0,
            )
            .copy(initialComponentTypes = setOf(cn("BootstrapProbe").expression))

    shouldThrow<InvalidGameConfigException> { Engine.newGame(premise) }
        .message
        .orEmpty()
        .shouldInclude("Choice")
  }
}
