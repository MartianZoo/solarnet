package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldInclude
import io.kotest.matchers.string.shouldNotInclude
import kotlin.test.Test

internal class InitializerTest {
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

    val failure = shouldThrow<PetException> { Engine.newGame(premise) }

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

    val failure = shouldThrow<PetException> { Engine.newGame(premise) }

    failure.message.orEmpty().shouldInclude("RequiredAtBootstrap (found 0, expected 1)")
  }

  @Test
  internal fun completedBootstrapChecksDependentLowerBoundsPerLiveScope() {
    val premise =
        testGamePremise(
                """
                ABSTRACT CLASS Anchor {
                  HAS MAX 1 This
                  HAS =1 Marker<This>
                  CLASS Left, Right, Absent
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

    val failure = shouldThrow<PetException> { Engine.newGame(premise) }

    failure.message.orEmpty().shouldInclude("Marker<Right> (found 0, expected 1)")
    failure.message.orEmpty().shouldNotInclude("Marker<Left>")
    failure.message.orEmpty().shouldNotInclude("Marker<Absent>")
  }

  @Test
  internal fun bootstrapRejectsTaskWithMultipleConcreteNarrowings() {
    val premise =
        testGamePremise(
                """
                ABSTRACT CLASS Choice { CLASS Left, Right }
                CLASS BootstrapProbe { This: Choice }
                """,
                players = 0,
            )
            .copy(initialComponentTypes = setOf(cn("BootstrapProbe").expression))

    shouldThrow<TaskException> { Engine.newGame(premise) }.message.orEmpty().shouldInclude("Choice")
  }
}
